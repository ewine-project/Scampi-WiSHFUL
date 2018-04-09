package de.tu_berlin.tkn.ewine;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import wishful_framework.Config.D2DAgentConfig;
import wishful_framework.Messages;
import wishful_framework.Configuration;
import wishful_framework.WishFulConfig;
import wishful_framework.WishFulController;
import wishful_framework.WishFulEntity;
import wishful_framework.WishFulEvent;

import wishful_framework.Events.*;
import wishful_framework.WishFulModule;

public class WishFulService extends Service implements zmqListener {

    public static final String TAG = "WishFulService";
    public static final String INTENT_SERVICE_STARTED = "WISHFUL_SERVICE_STARTED";
    public static final String INTENT_SERVICE_STOPPED = "WISHFUL_SERVICE_STOPPED";
    public static final String INTENT_DEBUG_TEXT      = "WISHFUL_SERVICE_DEBUG";

    /*
    * ZMQ
    * */
    public static final boolean ignoreHelloMessages = true;

    private static int pubBrokerPort = 8989;
    private static int subBrokerPort = 8990;

    //Hyperion TKN
    //private static String brokerIP = "130.149.49.153"; //"10.0.2.2";
    //Amazon EC2
    private static String brokerIP = "34.243.107.171";

    private zmqManager zmqMng = null;

    protected static WishFulService serviceInstance = null;
    protected static eWINESensingService eWINESensingServiceInstance = null;

    /*
        GUI
     */
    public static final int MSG_TYPE_DEBUG     = 0x01;
    public static final int MSG_TYPE_ERROR     = 0x02;
    public static final int MSG_TYPE_VERBOSE   = 0x03;

    /*
     Wishful
     */
    protected String uuid;
    protected String ip;
    protected String hostname;
    protected Configuration config;

    // executor for WishFul management purposes
    private ScheduledExecutorService scheduledExecutor;
    // executor for tasks / commands of the ongoing experiment
    private ScheduledExecutorService taskExecutor;

    //hard coded for now
    private String yamlConfigFile = "D2DClient.yaml";
    private WishFulConfig defaultConfig = new D2DAgentConfig();

    private static final int helloMsgInterval    = 1;
    private static final int helloTimeOut        = 10;
    public static final String helloMsgTopic    = "HELLO_MSG";

    public static final String nodeInfoTopic       = "NODE_INFO";
    public static final String nodeExitTopic       = "NODE_EXIT";
    public static final String allTopic            = "ALL";

    /** indicates how to behave if the service is killed */
    private int mStartMode =  START_STICKY; //START_STICKY_COMPATIBILITY

    /** interface for clients that bind */
    private IBinder mBinder = new wishfulBinder();

    /** indicates whether onRebind should be used */
    private boolean mAllowRebind = true;
    public String getUUID() {return uuid;}

    public static void setEWINESensingServiceInstance(eWINESensingService instance) {
        eWINESensingServiceInstance = instance;
    }

    /** Called when the service is being created. */
    @Override
    public void onCreate() {
        printVerbose("onCreate");
        super.onCreate();

        this.scheduledExecutor  = Executors.newSingleThreadScheduledExecutor();
        this.taskExecutor       = Executors.newSingleThreadScheduledExecutor();

        zmqMng = new zmqManager("tcp://"+brokerIP+":"+pubBrokerPort, "tcp://"+brokerIP+":"+subBrokerPort, this);
        zmqMng.currentThread = new Thread(zmqMng);
        zmqMng.currentThread.start();

        this.serviceInstance = this;
    }

    /** The service is starting, due to a call to startService() */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        printVerbose("onStartCommand");

        //todo report info to wishful agent server
        // - location
        // - device info

        return mStartMode;
    }

    /** A client is binding to the service with bindService() */
    @Override
    public IBinder onBind(Intent intent) {
        printVerbose("onBind");

        return mBinder;
    }

    /** Called when all clients have unbound with unbindService() */
    @Override
    public boolean onUnbind(Intent intent) {
        printVerbose("onUnbind");

        return mAllowRebind;
    }

    /** Called when a client is binding to the service with bindService()*/
    @Override
    public void onRebind(Intent intent) {

    }

    /** Called when The service is no longer used and is being destroyed */
    @Override
    public void onDestroy() {
        super.onDestroy();

        for (WishFulController controller : config.controllerList) {
            controller.onDestroy();
        }
        for (WishFulModule module : config.moduleList) {
            module.onDestroy();
        }

        Log.v(TAG, "onDestroy ");
        sendNodeExitMsg();

        if (zmqMng != null && zmqMng.currentThread != null) {
            zmqMng.terminate();
        }

        sendBroadcast(new Intent(WishFulService.INTENT_SERVICE_STOPPED));

        this.serviceInstance = null;

        this.scheduledExecutor.shutdownNow();
        this.taskExecutor.shutdownNow();


    }

    public WishFulEntity getWishFulEntity(String name) {
        if (config == null) {
            return null;
        }

        for (WishFulController controller : config.controllerList) {
            if (controller.getClass().getSimpleName().equals(name)) {
                return (WishFulEntity)controller;
            }
        }
        for (WishFulModule module : config.moduleList) {
            if (module.getClass().getSimpleName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public void init() {

        config = Configuration.parse(yamlConfigFile, defaultConfig, this);
        if (config == null) {
            printError("Parsing yaml config failed.");
            return;
        }
        config.print();

        if (config.agent != null && config.agent.name != null) {
            uuid = config.agent.name;
        } else {
            uuid = UUID.randomUUID().toString();
        }


        hostname = "";
        ip = "";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            printError("init cannot obtain IP or hostname");
        }

        //ZMQ Init
        zmqMng.subscribe(uuid);
        zmqMng.subscribe(allTopic);
        zmqMng.subscribe(nodeInfoTopic);
        zmqMng.subscribe(nodeExitTopic);
        zmqMng.subscribe(helloMsgTopic);

        this.scheduledExecutor.scheduleAtFixedRate( new SendHelloTask( this ), helloMsgInterval, helloMsgInterval,  TimeUnit.SECONDS  );

        //notify app that instance is available
        sendBroadcast(new Intent(WishFulService.INTENT_SERVICE_STARTED));
    }

    private static class SendHelloTask implements Runnable {
        WishFulService service;
        public SendHelloTask(WishFulService service) {
            this.service = service;
        }

        @Override
        public void run() {
            service.sendHelloMsg();
        }
    }

    private void sendHelloMsg() {
        if (!ignoreHelloMessages) {
            printVerbose("sendHelloMsg");
        }

        wishful_framework.Messages.HelloMsg helloMsg = wishful_framework.Messages.HelloMsg.newBuilder()
                .setUuid(uuid)
                .setTimeout(WishFulService.helloTimeOut)
                .build();

        WishfulMessage msg  = new WishfulMessage(helloMsgTopic,
                                                helloMsg.toByteArray());

        msg.description     = new WishfulMessage.Description(   WishfulMessage.getMsgType(helloMsg),
                                                                uuid,
                                                                WishfulMessage.SerializationType.PROTOBUF);

        sendMsg(msg);
    }

    private void processNodeInfoRequest(WishfulMessage receivedMsg)  {
        //yaml not yet parsed
        if (config == null || config.agent == null) {
            return;
        }
        if (receivedMsg.description.serializationType != WishfulMessage.SerializationType.PROTOBUF) {
            printError("processNodeInfoRequest wrong SerializationType "+receivedMsg.description.serializationType);
            return;
        }

        wishful_framework.Messages.NodeInfoMsg.Builder nodeInfoMsgBuilder = wishful_framework.Messages.NodeInfoMsg.newBuilder();

        nodeInfoMsgBuilder.setAgentUuid(uuid);
        nodeInfoMsgBuilder.setIp(ip);
        nodeInfoMsgBuilder.setName(config.agent.name);
        nodeInfoMsgBuilder.setHostname(hostname);

        nodeInfoMsgBuilder.setInfo(config.agent.info);

        for (wishful_framework.WishFulModule module : config.moduleList) {
            wishful_framework.Messages.Module.Builder moduleBuilder = wishful_framework.Messages.Module.newBuilder();

            moduleBuilder.setUuid(module.getUuid().toString());
            moduleBuilder.setId(module.getId());
            moduleBuilder.setName(module.getName());
            moduleBuilder.setType(Messages.Module.ModuleType.MODULE);

            Class moduleClass = module.getClass();
            Method[] methodArray = moduleClass.getDeclaredMethods();
            for (int i = 0; i < methodArray.length; i++) {

                if (!methodArray[i].getName().equalsIgnoreCase("access$super") &&
                        !methodArray[i].getName().equalsIgnoreCase("handleInEvent") &&
                        Modifier.isPublic(methodArray[i].getModifiers())) {

                    wishful_framework.Messages.Function.Builder functionBuilder = wishful_framework.Messages.Function.newBuilder();
                    functionBuilder.setName(methodArray[i].getName());

                    printVerbose(module.getName() + " addFunction " + methodArray[i].getName());

                    moduleBuilder.addFunctions(functionBuilder);
                }
            }

            LinkedList<Class> eventList = module.getInEvents();
            for (Class c : eventList) {
                wishful_framework.Messages.Event.Builder eventBuilder = wishful_framework.Messages.Event.newBuilder();
                eventBuilder.setName(c.getName());
                moduleBuilder.addInEvents(eventBuilder);
            }

            eventList = module.getOutEvents();
            for (Class c : eventList) {
                wishful_framework.Messages.Event.Builder eventBuilder = wishful_framework.Messages.Event.newBuilder();
                eventBuilder.setName(c.getName());
                moduleBuilder.addInEvents(eventBuilder);
            }

            nodeInfoMsgBuilder.addModules(moduleBuilder);
        }

        wishful_framework.Messages.NodeInfoMsg nodeInfoMsg = nodeInfoMsgBuilder.build();
        printVerbose(nodeInfoMsg.toString());

        WishfulMessage msg  = new WishfulMessage(receivedMsg.description.sourceUuid,
                                                    nodeInfoMsg.toByteArray());

        msg.description     = new WishfulMessage.Description(   WishfulMessage.getMsgType(nodeInfoMsg),
                                                                uuid,
                                                                WishfulMessage.SerializationType.PROTOBUF);

        sendMsg(msg);
    }

    private void processNodeAddNotification(WishfulMessage nodeAddMsg) {
        printVerbose("processNodeAddNotification");

        wishful_framework.Messages.NodeAddNotification nodeAddNotificationMsg = wishful_framework.Messages.NodeAddNotification.newBuilder()
                .setAgentUuid(uuid)
                .build();

        WishfulMessage msg  = new WishfulMessage(   nodeAddMsg.description.sourceUuid,
                nodeAddNotificationMsg.toByteArray());

        msg.description     = new WishfulMessage.Description(   WishfulMessage.getMsgType(nodeAddNotificationMsg),
                            uuid,
                            WishfulMessage.SerializationType.PROTOBUF);

        sendMsg(msg);
    }

    private void sendNodeExitMsg() {
        printVerbose("sendNodeExitMsg");

        wishful_framework.Messages.NodeExitMsg nodeExitMsg = wishful_framework.Messages.NodeExitMsg.newBuilder()
                .setAgentUuid(uuid)
                .build();

        WishfulMessage msg  = new WishfulMessage(  nodeExitTopic,
                nodeExitMsg.toByteArray());

        msg.description     = new WishfulMessage.Description(   WishfulMessage.getMsgType(nodeExitMsg),
                uuid,
                WishfulMessage.SerializationType.PROTOBUF);

        // todo reenable this when uniflex is fixed !
        //sendMsg(msg);
    }

    private void processEventMessage(WishfulMessage msg) {

        if (msg.description.serializationType != WishfulMessage.SerializationType.JSON) {
            printError("processEventMessage received event with non JSON serialization type " + msg.description.serializationType.toString());
            return;
        }

        //create event object from message and schedule
        Class<?> eventClass = null;
        String eventClassName = msg.description.msgType;

        if (!eventClassName.startsWith(WishFulEvent.packageName)) {
            eventClassName = WishFulEvent.packageName+ "." + eventClassName;
        }

        try {
            eventClass = Class.forName(eventClassName);
        } catch (ClassNotFoundException ex) {
            printError("processEventMessage class for event with name " + msg.description.msgType + " not found");
            return;
        }

        JSONObject jsonObject;
        String timeString = "";
        String decodedString = "";
        try {
            decodedString = new String(msg.content, "UTF-8");
            jsonObject = new JSONObject(decodedString);
            timeString = jsonObject.getString("time");
        } catch (UnsupportedEncodingException ex) {
            printError("processEventMessage UnsupportedEncodingException ");
           return;
        } catch (JSONException ex) {
            printError("processEventMessage JSONException "+decodedString);
           return;
        }

        long execTime = 0;
        try {
            execTime = Long.parseLong(timeString);
        } catch(NumberFormatException ex) {
            printError("processEventMessage cannot parse time of event "+timeString);
            return;
        }

        try {
            Constructor<?> cons = eventClass.getConstructor();
            WishFulEvent event = (WishFulEvent)cons.newInstance();
            if (!event.parseWishFulMessage(msg)) {
                printError("processEventMessage parsing message failed");
                return;
            }

            //schedule event
            this.scheduleEvent(event, execTime);

        } catch (NoSuchMethodException ex) {
            printError("processEventMessage constructor not found for class " + msg.description.msgType);
            return;
        } catch (InstantiationException ex) {
            printError("processEventMessage InstantiationException " + ex);
            return;
        } catch (InvocationTargetException ex) {
            printError("processEventMessage InvocationTargetException " + ex);
            return;
        } catch (IllegalAccessException ex) {
            printError("processEventMessage IllegalAccessException " + ex);
            return;
        }
    }

    private void scheduleEvent(WishFulEvent event, long time) {
        printVerbose("scheduleEvent " + event.getClass().getSimpleName()+" in "+time+" ms");
        this.taskExecutor.schedule( new ExecuteTask( this, event ), time,  TimeUnit.MILLISECONDS  );
    }

    private static class ExecuteTask implements Runnable {
        WishFulService service;
        WishFulEvent event;

        public ExecuteTask(WishFulService service, WishFulEvent event) {
            this.service    = service;
            this.event      = event;
        }

        @Override
        public void run() {

            this.service.printVerbose("ExecuteTask event "+event.toString());

            for (wishful_framework.WishFulModule module : service.config.moduleList) {
                if (module.isInterestedInEvent(event.getClass())) {
                    module.handleInEvent(event);
                }
            }
            for (wishful_framework.WishFulController controller : service.config.controllerList) {
                if (controller.isInterestedInEvent(event.getClass())) {
                    controller.handleInEvent(event);
                }
            }
        }
    }

    public void sendMsg(WishfulMessage msg) {
        if (zmqMng == null) {
            printError("sendMsg zmqMng is null");
            return;
        }

        try {
            List<byte[]> multiMsg = new ArrayList<byte[]>();
            multiMsg.add(msg.topic.getBytes("UTF-8"));
            multiMsg.add(msg.description.serialize().toString().getBytes("UTF-8"));
            multiMsg.add(msg.content);

            zmqMng.sendMessage(multiMsg);
        } catch (UnsupportedEncodingException ex) {
            printError("sendMsg UnsupportedEncodingException");
            return;
        }
    }

    public void zmqReceive(List<byte[]> msgArray) {

        if (msgArray.size() != 3) {
            printError("zmqReceive wrong message format consisting of " + msgArray.size() + " parts");
            return;
        }
        String topic,
                msgDescription;

        try {
            topic = new String(msgArray.get(0), "UTF-8");
            msgDescription = new String(msgArray.get(1), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            printError("zmqReceive unsupported encoding");
            return;
        }

        WishfulMessage msg = new WishfulMessage(topic, msgArray.get(2));
        if (!msg.description.parse(msgDescription)) {
            return;
        }

        if (!msg.topic.equalsIgnoreCase(helloMsgTopic) || !ignoreHelloMessages) {
            printVerbose("zmqReceive Topic " + msg.topic);
            if (!msg.topic.equalsIgnoreCase(helloMsgTopic)) {
                printVerbose("\t Descr " + msg.description);
                if (msg.description.serializationType == WishfulMessage.SerializationType.JSON) {
                    try {
                        printVerbose("\t Content " + new String(msg.content, "UTF-8"));
                    } catch (UnsupportedEncodingException ex) {
                        //ignore, its just debug output
                    }
                }
            }
        }

        //check if msg is an event
        if (msg.description.msgType.endsWith("Event")) {
            processEventMessage(msg);

            //unicast to UUID topic
        } else if (topic.equals(uuid)) {
            if (msg.description.msgType.equals(wishful_framework.Messages.NodeInfoRequest.class.getSimpleName())) {
                processNodeInfoRequest(msg);
            } else if (msg.description.msgType.equals(wishful_framework.Messages.NodeAddNotification.class.getSimpleName())) {
                processNodeAddNotification(msg);
            } else {
                printError("zmqReceive unknown message type "+msg.description.msgType+" on topic "+topic);
            }
        } else if (topic.equals(helloMsgTopic)) {
            if (msg.description.msgType.equals(wishful_framework.Messages.HelloMsg.class.getSimpleName())) {
                //we ignore hello messages since we are not interested in other agents
            } else {
                printError("zmqReceive unknown message type "+msg.description.msgType+" on topic "+topic);
            }
        } else {
            printError("zmqReceive unknown topic " + topic);
        }
    }

    /*
        Print debug using Log() and also in TextBox GUI
     */
    private void printDebug(String msg) {
        printString(msg, MSG_TYPE_DEBUG);
    }
    private void printVerbose(String msg) {
        printString(msg, MSG_TYPE_VERBOSE);
    }
    private void printError(String msg) {
        printString(msg, MSG_TYPE_ERROR);
    }

    private void printString(String msg, int msgType) {
        switch (msgType) {
            case MSG_TYPE_ERROR:
                Log.e(TAG, msg);
                break;
            case MSG_TYPE_DEBUG:
                Log.d(TAG, msg);
                break;
            default:
                Log.v(TAG, msg);
        }

        Intent intent = new Intent(WishFulService.INTENT_DEBUG_TEXT);
        intent.putExtra("text",msg);
        intent.putExtra("type",msgType);
        sendBroadcast(intent);
    }

    public void zmqInitialized() {
        init();
    }

    public void runTest(int testId) {

        String passphrase   = "";
        String ssid         = "";
        int channel         = 6;

        passphrase  = "12345678";
        ssid        = "ssid";
        

        switch(testId) {
            case TestEvent.TEST_WIFI_START_AP:
                WiFiStartNetworkCommandEvent startCmd = new WiFiStartNetworkCommandEvent();
                startCmd.channelIndex   = channel;
                startCmd.networkName    = ssid;
                startCmd.passphrase     = passphrase;
                this.scheduleEvent(startCmd, 0);
                break;

            case TestEvent.TEST_WIFI_STOP_AP:
                WiFiStopNetworkCommandEvent stopCmd = new WiFiStopNetworkCommandEvent();
                this.scheduleEvent(stopCmd, 0);
                break;

            case TestEvent.TEST_WIFI_JOIN_AP:
                WiFiJoinNetworkCommandEvent joinCmd = new WiFiJoinNetworkCommandEvent();
                joinCmd.channelIndex    = channel;
                joinCmd.networkName     = ssid;
                joinCmd.passphrase      = passphrase;
                this.scheduleEvent(joinCmd, 0);
                break;

            case TestEvent.TEST_WIFI_LEAVE_AP:
                WiFiLeaveNetworkCommandEvent leaveCmd = new WiFiLeaveNetworkCommandEvent();
                this.scheduleEvent(leaveCmd, 0);
                break;

            case TestEvent.TEST_JOIN_NEXUS5_1:
                WiFiJoinNetworkCommandEvent joinNexus = new WiFiJoinNetworkCommandEvent();
                joinNexus.channelIndex    = 1;
                joinNexus.networkName     = "DIRECT-NI-Android_2bdd";
                joinNexus.passphrase      = "eVxId91L";
                this.scheduleEvent(joinNexus, 0);
                break;

            case TestEvent.TEST_JOIN_G2_2:
                WiFiJoinNetworkCommandEvent joinG2 = new WiFiJoinNetworkCommandEvent();
                joinG2.channelIndex    = 1;
                joinG2.networkName     = "DIRECT-3D-g2-2";
                joinG2.passphrase      = "jFgEduEL";
                this.scheduleEvent(joinG2, 0);
                break;

            default:
                TestEvent testEvent = new TestEvent();
                this.scheduleEvent(testEvent, 0);
        }

    }

    public class wishfulBinder extends Binder {
        WishFulService getService() {
            return WishFulService.this;
        }
    }
}
