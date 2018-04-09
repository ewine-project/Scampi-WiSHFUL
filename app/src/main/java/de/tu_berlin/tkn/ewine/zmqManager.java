package de.tu_berlin.tkn.ewine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.util.Log;

import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.util.ArrayList;
import java.util.List;

import wishful_framework.Events.D2DAddContentNotificationEvent;
import wishful_framework.Events.D2DAddInterestNotificationEvent;
import wishful_framework.Events.D2DRemoveContentNotificationEvent;
import wishful_framework.Events.D2DRemoveInterestNotificationEvent;

public class zmqManager implements Runnable {

    public static final String TAG = "zmqManager";

    public static final String SEND_STRING = "ZMQMNG_SEND_STRING";

    private boolean isRunning;

    private ZMQ.Context zmqContext;
    private ZMQ.Socket zmqPubSocket;
    private ZMQ.Socket zmqSubSocket;

    private Object pubSocketLock = new Object();

    private String pubBrokerAddress = null;
    private String subBrokerAddress = null;

    public Thread currentThread;
    private List<zmqListener> listenerArray;

    private Context context = null;
    private BReceiver bReceiver = null;

    public zmqManager(String pubBrokerAddress, String subBrokerAddress, zmqListener context) {

        this.listenerArray = new ArrayList<zmqListener>();
        this.listenerArray.add(context);

        this.isRunning = false;

        this.zmqContext     = null;
        this.zmqPubSocket   = null;
        this.zmqSubSocket   = null;

        this.pubBrokerAddress = pubBrokerAddress;
        this.subBrokerAddress = subBrokerAddress;

        this.context = (Context)context;
    }

    public void start() {
        this.zmqContext = ZMQ.context(1);

        this.zmqPubSocket = this.zmqContext.socket(ZMQ.PUB);
        this.zmqPubSocket.connect(pubBrokerAddress);

        this.zmqSubSocket = this.zmqContext.socket(ZMQ.SUB);
        this.zmqSubSocket.connect(subBrokerAddress);

        Log.v(TAG, "start zmq connected to pub " + pubBrokerAddress+ " sub "+subBrokerAddress);

        this.isRunning = true;

        for(zmqListener listener : this.listenerArray){
            listener.zmqInitialized();
        }
    }

    public boolean subscribe(String topic) {
        if (this.zmqSubSocket == null) {
            return false;
        }
        return subscribe(topic.getBytes());
    }
    public boolean subscribe(byte[] topic) {
        if (this.zmqSubSocket == null) {
            return false;
        }
        Log.v(TAG, "subscribe to " + new String(topic));
        this.zmqSubSocket.subscribe(topic);
        return true;
    }

    public void terminate() {
        this.isRunning = false;

        Log.v(TAG, "unregisterReceiver...");
        context.unregisterReceiver(bReceiver);

        terminateTask task = new terminateTask();
        task.execute();
    }

    private class terminateTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... data) {
            Log.v(TAG,"terminateTask() shutting down ZMQ");
            if (zmqSubSocket != null) {
                zmqSubSocket.close();
            }
            zmqSubSocket = null;

            if (zmqPubSocket != null) {
                synchronized(pubSocketLock) {
                    zmqPubSocket.close();
                    zmqPubSocket = null;
                }
            }

            if (zmqContext != null) {
                zmqContext.term();
            }
            zmqContext = null;

            return null;
        }
    }

    @Override
    public void run() {

        bReceiver = new BReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(zmqManager.SEND_STRING);
        context.registerReceiver(bReceiver, intentFilter);

        start();

        byte[] byteMsg = null;
        while(!Thread.currentThread().isInterrupted() && isRunning) {

            List<byte[]> multiMsg = new ArrayList<byte[]>();

            try {
                byteMsg = this.zmqSubSocket.recv();
                multiMsg.add(byteMsg);


                while (isRunning && this.zmqSubSocket.hasReceiveMore()) {

                    byteMsg = this.zmqSubSocket.recv();
                    multiMsg.add(byteMsg);
                }
            } catch (ZMQException e) {
                if (e.getErrorCode () == ZMQ.Error.ETERM.getCode ()) {
                    return;
                }
            }

            //Log.v(TAG, "received msg consisting of " +multiMsg.size()+" parts");
            if (isRunning) {
                for (zmqListener listener : listenerArray) {
                    listener.zmqReceive(multiMsg);
                }
            }
        }
    }

    public void sendMessage(String msg) {
        List<byte[]> multiMsg = new ArrayList<byte[]>();
        multiMsg.add(msg.getBytes());

        SendTask task = new SendTask(multiMsg);
        task.execute();
    }

    public void sendMessage( List<byte[]> multiMsg) {
        SendTask task = new SendTask(multiMsg);
        task.execute();
    }

    private class SendTask extends AsyncTask<Void, Void, Void> {
        private List<byte[]> multiMsg;

        public SendTask(List<byte[]> multiMsg) {
            this.multiMsg = multiMsg;
        }

        protected Void doInBackground(Void... data) {
            byte[] msg = null;

            boolean printMessage = true;
            boolean printBody    = false;
            if (WishFulService.ignoreHelloMessages) {
                if (multiMsg != null && multiMsg.size() >= 1) {
                    msg = multiMsg.get(0);
                    String topic = new String(msg);

                    if (topic.equalsIgnoreCase(WishFulService.helloMsgTopic)) {
                        printMessage = false;
                    }
                }
            }

            synchronized(pubSocketLock) {

                if (zmqPubSocket == null) {
                    Log.v(TAG, "sendMessage zmqPubSocket null");
                    return null;
                }
                if (multiMsg == null) {
                    Log.v(TAG, "sendMessage multiMsg null");
                    return null;
                }
                if (printMessage) {
                    Log.v(TAG, "sendMessage #" + multiMsg.size());
                }

                for (int i=0; i<multiMsg.size() - 1; i++) {

                    msg = multiMsg.get(i);
                    zmqPubSocket.sendMore(msg);
                    if (printMessage) {
                        Log.v(TAG, "\t " + new String(msg));
                    }

                    if (i==0) {
                        String topic = new String(msg);
                        if (topic.equalsIgnoreCase(WishFulService.helloMsgTopic)) {
                            printMessage = false;

                        } else if (    topic.equalsIgnoreCase(D2DAddContentNotificationEvent.class.getSimpleName())
                                    || topic.equalsIgnoreCase(D2DAddInterestNotificationEvent.class.getSimpleName())
                                    || topic.equalsIgnoreCase(D2DRemoveContentNotificationEvent.class.getSimpleName())
                                    || topic.equalsIgnoreCase(D2DRemoveInterestNotificationEvent.class.getSimpleName())) {
                            printBody = true;
                        }
                    }
                }

                msg = multiMsg.get(multiMsg.size()-1);
                if (printBody) {
                    Log.v(TAG, "\t " + new String(msg));
                }
                zmqPubSocket.send(msg);

            }
            return null;
        }

        protected void onProgressUpdate() {
        }

        protected void onPostExecute() {
        }
    }

    private class BReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            Log.v(TAG, "BReiceiver action "+action);

            if (action.equals(zmqManager.SEND_STRING)) {
               sendMessage("start");
            }
        }
    }
}

