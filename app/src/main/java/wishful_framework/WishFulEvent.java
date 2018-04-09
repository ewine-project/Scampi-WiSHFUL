package wishful_framework;

import org.json.JSONObject;

import de.tu_berlin.tkn.ewine.WishfulMessage;

public abstract class WishFulEvent {

    public static final String packageName = "wishful_framework.Events";

    public WishFulEvent() { }

    public abstract boolean parseWishFulMessage(WishfulMessage msg);

    public abstract String toString();

    public abstract JSONObject serialize(WishFulEntity requestingEntity);
}
