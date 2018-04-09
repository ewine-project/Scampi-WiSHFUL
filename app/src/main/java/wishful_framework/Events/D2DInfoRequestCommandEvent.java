package wishful_framework.Events;


import org.json.JSONObject;

import de.tu_berlin.tkn.ewine.WishfulMessage;
import wishful_framework.WishFulEntity;

/*
    Send by the D2DController if it discovers a new device
    Device will send info about all current content / interest / device
    in separate events
 */
public class D2DInfoRequestCommandEvent extends CommandEvent {
    private static final String TAG =  "D2DNodeInfoRequestCommand";

    public D2DInfoRequestCommandEvent() { }

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
