package wishful_framework.Events;


import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import de.tu_berlin.tkn.ewine.WishfulMessage;
import wishful_framework.WishFulEntity;

public class WiFiJoinNetworkCommandEvent extends CommandEvent {
    private static final String TAG =  "WiFiJoinNetworkEvent";

    public String networkName;
    public String passphrase;
    public int channelIndex;

    @Override
    public boolean parseWishFulMessage(WishfulMessage msg) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(new String(msg.getContent(), "UTF-8"));

            networkName = jsonObject.getString("networkName");
            passphrase = jsonObject.getString("passphrase");
            try {
                channelIndex = Integer.parseInt(jsonObject.getString("channelIndex"));
            } catch(NumberFormatException ex) {
                Log.e(TAG, "parseWishFulMessage cannot parse channelIndex "+jsonObject.getString("channelIndex")); return false;
            }

        } catch (UnsupportedEncodingException ex) {
            Log.e(TAG, "parseWishFulMessage UnsupportedEncodingException "); return false;
        } catch (JSONException ex) {
            Log.e(TAG, "parseWishFulMessage JSONException"); return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+" net "+networkName+" pw "+passphrase+" chan "+channelIndex;
    }

    @Override
    public JSONObject serialize(WishFulEntity requestingEntity) {
        //will not be serialized..
        return null;
    }
}
