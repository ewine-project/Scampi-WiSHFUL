package wishful_framework;

import android.util.Log;

import java.util.Map;

public class WishFulAgent {
    private static final String TAG =  "WishFulAgent";

    public String   name     = "",
            info    = "",
            type    = "",
            iface   = "",
            sub     = "",
            pub     = "";

    public void print(String prefix) {
        Log.v(TAG, prefix+"Agent");
        Log.v(TAG, prefix+"\t name " + name);
        Log.v(TAG, prefix+"\t info " + info);
        Log.v(TAG, prefix+"\t iface " + iface);
        Log.v(TAG, prefix+"\t sub " + sub);
        Log.v(TAG, prefix+"\t pub " + pub);
    }

    public static WishFulAgent parse(Object obj) {

        if (!(obj instanceof Map)) {
            Log.e(TAG, "agent.parse obj not a map ");
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) obj;

        WishFulAgent agent = new WishFulAgent();
        for (String tmpKey : map.keySet()) {
            if (tmpKey.equals("name")) {
                agent.name = (String) map.get(tmpKey);
            } else if (tmpKey.equals("info")) {
                agent.info = (String) map.get(tmpKey);
            } else if (tmpKey.equals("type")) {
                agent.type = (String) map.get(tmpKey);
            } else if (tmpKey.equals("iface")) {
                agent.iface = (String) map.get(tmpKey);
            } else if (tmpKey.equals("sub")) {
                agent.sub = (String) map.get(tmpKey);
            } else if (tmpKey.equals("pub")) {
                agent.pub = (String) map.get(tmpKey);
            } else {
                Log.e(TAG, "agent.parse unknown yaml key " + tmpKey);
                return null;
            }
        }
        return agent;
    }
}
