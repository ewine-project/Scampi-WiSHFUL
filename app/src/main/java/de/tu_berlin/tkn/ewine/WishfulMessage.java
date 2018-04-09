package de.tu_berlin.tkn.ewine;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class WishfulMessage {

    public static final String TAG = "WishfulMessage";

    //plain text
    protected String topic;
    //JSON encoded description
    protected WishfulMessage.Description description;
    //binary content
    protected byte[] content;


    public WishfulMessage(String topic, byte[] content) {
        this.topic      = topic;
        description     = new Description();
        this.content    = content;
    }

    public String getTopic() {return topic;}
    public WishfulMessage.Description getDescription() {return description;}
    public byte[] getContent() {return content;}

    public void setDescription(WishfulMessage.Description description) {this.description = description;}

    public boolean deserialize() {
        if (description == null) {
            return false;
        }
        switch(description.serializationType) {
            case NONE:
            case JSON:
            case PICKLE:
            case MSGPACK:
                Log.d(TAG, "parse serialization type not supported "+description.serializationType);
                return false;
            case PROTOBUF:
               return true;
            default:
                Log.d(TAG, "parse unknown serialization type "+description.serializationType);
                return false;
        }
    }

    public static String getMsgType(Object obj) {
        if (obj instanceof wishful_framework.Messages.HelloMsg) {
            return wishful_framework.Messages.HelloMsg.class.getSimpleName();

        } else if (obj instanceof wishful_framework.Messages.NodeInfoRequest) {
            return wishful_framework.Messages.NodeInfoRequest.class.getSimpleName();

        } else if (obj instanceof wishful_framework.Messages.NodeInfoMsg) {
            return wishful_framework.Messages.NodeInfoMsg.class.getSimpleName();

        } else if (obj instanceof wishful_framework.Messages.NodeAddNotification) {
            return wishful_framework.Messages.NodeAddNotification.class.getSimpleName();

        } else {
            Log.e(TAG, "unknown object type "+obj.getClass().getSimpleName());
            return null;
        }
    }

    public static enum SerializationType {
        NONE (0),
        JSON (1),
        PICKLE (2),
        MSGPACK (3),
        PROTOBUF (4);

        private final int id;
        SerializationType(int id) { this.id = id; }
        public int getValue() { return id; }

        public String toString() {
            switch(id) {
                case 0:
                    return "NONE";
                case 1:
                    return "JSON";
                case 2:
                    return "PICKLE";
                case 3:
                    return "MSGPACK";
                case 4:
                    return "PROTOBUF";
                default:
                   return "UNKNOWN";
            }
        }

        public static SerializationType get(String typeString) {
            try {
                int typeInt = Integer.parseInt(typeString);
                return fromInt(typeInt);
            } catch (NumberFormatException e) {
                return fromString(typeString);
            }
        }

        public static SerializationType fromInt(int x) {
            switch(x) {
                case 0:
                    return NONE;
                case 1:
                    return JSON;
                case 2:
                    return PICKLE;
                case 3:
                    return MSGPACK;
                case 4:
                    return PROTOBUF;
                default:
                    Log.e(TAG, "SerializationType unknown type "+x);
                    return null;
            }
        }
        public static SerializationType fromString(String x) {
            if (x.equals(NONE.toString())) {
                return NONE;
            } else if (x.equals(JSON.toString())) {
                return JSON;
            } else if (x.equals(PICKLE.toString())) {
                return PICKLE;
            } else if (x.equals(MSGPACK.toString())) {
                return MSGPACK;
            } else if (x.equals(PROTOBUF.toString())) {
                return  PROTOBUF;
            } else {
                Log.e(TAG, "SerializationType unknown type "+x);
                return null;
            }
        }
    }

    public static class Description {
        String msgType;
        String sourceUuid;
        WishfulMessage.SerializationType serializationType;

        public Description() {
            serializationType = SerializationType.NONE;
        }

        public Description(   String msgType,
                              String sourceUuid,
                              WishfulMessage.SerializationType serializationType) {
            this.msgType            = msgType;
            this.sourceUuid         = sourceUuid;
            this.serializationType  = serializationType;
        }

        public String toString() {
            return "msgType "+msgType+" serialType "+serializationType.toString()+" uuid "+sourceUuid;
        }

        public JSONObject serialize() {
            JSONObject jObj = new JSONObject();
            try {
                jObj.put("msgType", msgType);
                jObj.put("sourceUuid", sourceUuid);
                jObj.put("serializationType", serializationType.getValue());
            } catch (JSONException ex) {
                Log.e(TAG, "serialize exception");
                return null;
            }
            return jObj;
        }

        public boolean parse(String desc) {
            try {
                JSONObject json = new JSONObject(desc);

                msgType = json.getString("msgType");
                sourceUuid = json.getString("sourceUuid");
                if ((serializationType = SerializationType.get(json.getString("serializationType"))) == null) {
                    return false;
                }

            } catch(JSONException ex) {
                Log.e(TAG, "parse exception "+desc);
                Log.e(TAG, ex.toString());
                return false;
            }
            return true;
        }
    }
}
