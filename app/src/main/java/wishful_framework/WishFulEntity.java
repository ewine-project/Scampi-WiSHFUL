package wishful_framework;

import android.util.Log;

import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.tu_berlin.tkn.ewine.WishFulService;
import de.tu_berlin.tkn.ewine.WishfulMessage;

public abstract class WishFulEntity {
    private static final String TAG =  "WishFulEntity";

    private static int entityId = 0;

    protected UUID uuid           = null;
    protected int id              = -1;
    protected String name         = ""; //
    protected String file         = "";
    protected String className    = ""; //class in file, multiples classes per file
    protected String module       = ""; //file name
    protected List<WishFulDevice> devices   = null;
    protected WishFulKwargs kwargs          = null;

    protected LinkedList<Class> outEventList = null;
    protected LinkedList<Class> inEventList  = null;

    protected WishFulService wishFulService;

    WishFulEntity() {
        uuid    = UUID.randomUUID();
        kwargs  = new WishFulKwargs();
        id      = getNewId();

        outEventList = new LinkedList<Class>();
        inEventList  = new LinkedList<Class>();
    }

    public UUID getUuid() {return uuid;};
    public String getName() {return name;}
    public int getId() {return id;}
    public static int getNewId() {
        entityId++;
        return entityId;
    }
    public LinkedList<Class> getInEvents() {return inEventList;}
    public LinkedList<Class> getOutEvents() {return outEventList;}
    public WishFulService getWishFulService() {return wishFulService;}

    public abstract void handleInEvent(WishFulEvent event);

    public abstract void onDestroy();

    protected void registerInEvent(Class cls) {
        for (Class c : inEventList) {
            if (cls.equals(c)) {
                return;
            }
        }
        inEventList.add(cls);
    }
    protected void registerOutEvent(Class cls) {
        for (Class c : outEventList) {
            if (cls.equals(c)) {
                return;
            }
        }
        outEventList.add(cls);
    }
    public boolean isInterestedInEvent(Class cls) {
        for (Class c : inEventList) {
            if (cls.equals(c)) {
                return true;
            }
        }
        return false;
    }

    protected void print(String type, String prefix) {
        Log.v(TAG, prefix+type);
        Log.v(TAG, prefix+"\t name "+name);
        Log.v(TAG, prefix+"\t\t file "+file);
        Log.v(TAG, prefix+"\t\t class_name "+className);
        Log.v(TAG, prefix+"\t\t module "+module);
        String devString = prefix+"\t\t devices ";
        if (devices == null) {
            devString += "null";
        } else {
            for (WishFulDevice dev : devices) {
                devString += dev.name + " ";
            }
        }
        Log.v(TAG, devString);
        kwargs.print(prefix+"\t\t");
    }

    protected boolean parse(Map<String,Object> map) {
        for (String tmpKey : map.keySet()) {
            if (tmpKey.equals("class_name")) {
                className = (String) map.get(tmpKey);
            } else if (tmpKey.equals("module")) {
                module = (String) map.get(tmpKey);
            } else if (tmpKey.equals("devices") || tmpKey.equals("interfaces")) {

                if ((devices = WishFulDevice.parse(map.get(tmpKey))) == null) {
                    return false;
                }
            } else if (tmpKey.equals("kwargs")) {

                if ((kwargs = WishFulKwargs.parse(map.get(tmpKey))) == null) {
                    return false;
                }
            } else {
                Log.e(TAG, "entity.parse unknown yaml key " + tmpKey);
                return false;
            }
        }
        return true;
    }

    protected static WishFulEntity getEntityObject(WishFulService wishFulService,
                                                   String entityClassName,
                                                   String entityName,
                                                   Map<String,Object> map) {

        String className    = "";
        for (String tmpKey : map.keySet()) {
           if (tmpKey.equals("class_name")) {
               className = (String) map.get(tmpKey);
               break;
           }
        }
        if (className.equalsIgnoreCase("")) {
            Log.e(TAG, "className not set");
            return null;
        }

        //locate class with package name
        if (entityClassName.equalsIgnoreCase(WishFulController.class.getSimpleName())) {
            if (!className.startsWith(WishFulModule.packageName)) {
                className = WishFulController.packageName+ "." + className;
            }
        } else if (entityClassName.equalsIgnoreCase(WishFulModule.class.getSimpleName())) {
            if (!className.startsWith(WishFulModule.packageName)) {
                className = WishFulModule.packageName+ "." + className;
            }
        } else {
            Log.e(TAG, "unknown class name "+entityClassName);
            return null;
        }

        //locate entity class
        WishFulEntity entity = null;
        try {
            Class entityClass = Class.forName(className);
            Constructor<?> cons = entityClass.getConstructor(WishFulService.class, String.class);
            entity = (WishFulEntity)cons.newInstance(wishFulService, entityName);
        } catch (ClassNotFoundException ex) {
            Log.e(TAG, "parse class not found "+className);
            return null;
        } catch (NoSuchMethodException ex) {
            Log.e(TAG, "parse constructor not found "+className);
            return null;
        }  catch (IllegalAccessException ex) {
            Log.e(TAG, "IllegalAccessException "+className);
            return null;
        } catch (InstantiationException ex) {
            Log.e(TAG, "InstantiationException "+className);
            return null;
        } catch (InvocationTargetException ex) {
            Log.e(TAG, "InvocationTargetException "+className);
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            Log.e(TAG, errors.toString());
            return null;
        }

        return entity;
    }

    public void sendEvent(WishFulEvent event) {
        sendEvent(event, event.getClass().getSimpleName());
    }

    public void sendEvent(WishFulEvent event, String topic) {
        Log.v(TAG, "sendEvent "+event.toString()+" on topic "+topic);

        JSONObject jsonObject = event.serialize(this);

        WishfulMessage msg = null;
        try {
            msg  = new WishfulMessage(topic, jsonObject.toString().getBytes("UTF-8"));
            Log.v(TAG, "sendEvent "+jsonObject.toString());
        } catch (UnsupportedEncodingException ex) {
            Log.e(TAG, "sendEvent UnsupportedEncodingException");
            return;
        }

        msg.setDescription(new WishfulMessage.Description(  event.getClass().getSimpleName(),
                this.wishFulService.getUUID(),
                WishfulMessage.SerializationType.JSON));

        this.wishFulService.sendMsg(msg);
    }
}
