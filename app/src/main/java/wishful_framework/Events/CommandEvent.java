package wishful_framework.Events;


import org.json.JSONObject;

import de.tu_berlin.tkn.ewine.WishfulMessage;
import wishful_framework.WishFulEntity;
import wishful_framework.WishFulEvent;

public abstract class CommandEvent extends WishFulEvent {
    protected long executionTime;

    public abstract boolean parseWishFulMessage(WishfulMessage msg);
    public abstract String toString();
    public abstract JSONObject serialize(WishFulEntity requestingEntity);
}
