package wishful_framework;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;


public class WishFulKwargs {
    private static final String TAG =  "WishFulKwargs";

    public Map<String,String> map;

    public WishFulKwargs() {
        map = new HashMap<String,String>();
    }

    void print(String prefix) {
        Log.v(TAG, prefix+" kwargs");
        for (String tmpKey : map.keySet()) {
            Log.v(TAG, prefix+"\t \""+tmpKey+"\"-\""+map.get(tmpKey)+"\"");
        }
    }

    static WishFulKwargs parse(Object obj) {
        if (!(obj instanceof Map)) {
            Log.e(TAG, "kwargs.parse obj not a map");
            return null;
        }
        WishFulKwargs kwargs = new WishFulKwargs();
        kwargs.map = (Map<String,String>)obj;
        return kwargs;
    }
}
