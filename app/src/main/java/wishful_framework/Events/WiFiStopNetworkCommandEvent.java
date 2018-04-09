package wishful_framework.Events;

import org.json.JSONObject;

import de.tu_berlin.tkn.ewine.WishfulMessage;
import wishful_framework.WishFulEntity;

public class WiFiStopNetworkCommandEvent extends CommandEvent {
    private static final String TAG =  "WiFiStopNetworkEvent";

    public WiFiStopNetworkCommandEvent() {

    }

    @Override
    public boolean parseWishFulMessage(WishfulMessage msg) {
        return true;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public JSONObject serialize(WishFulEntity requestingEntity) {
        //will not be serialized..
        return null;
    }
}
