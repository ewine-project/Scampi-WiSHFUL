package wishful_framework;

import android.util.Log;

import java.util.Map;


public class WishFulBroker {
    private static final String TAG =  "WishFulBroker";

    private String      xpub = "",
                        xsub = "";

    public void WishFulBroker() {}

    public void print(String prefix) {
        Log.v(TAG, prefix+"Broker");
        Log.v(TAG, prefix+"\t xpub "+xpub);
        Log.v(TAG, prefix+"\t xsub "+xsub);
    }

    public static WishFulBroker parse(Object obj) {

        if (!(obj instanceof Map)) {
            Log.e(TAG, "broker.parse obj not a map ");
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) obj;

        WishFulBroker broker = new WishFulBroker();
        for (String tmpKey : map.keySet()) {
            if (tmpKey.equals("xpub")) {
                broker.xpub = (String) map.get(tmpKey);
            } else if (tmpKey.equals("xsub")) {
                broker.xsub = (String) map.get(tmpKey);
            } else {
                Log.e(TAG, "broker.parse unknown yaml key " + tmpKey);
                return null;
            }
        }
        return broker;
    }
}
