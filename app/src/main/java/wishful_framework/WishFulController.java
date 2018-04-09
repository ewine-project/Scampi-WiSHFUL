package wishful_framework;

import de.tu_berlin.tkn.ewine.WishFulService;
import de.tu_berlin.tkn.ewine.WishfulMessage;

public abstract class WishFulController extends WishFulEntity {
    private static final String TAG =  "WishFulController";

    public static final String packageName = "wishful_framework.Controllers";

    public WishFulController(WishFulService wishFulService) {
        super();
        this.wishFulService = wishFulService;
    }

    public void print(String prefix) {
        super.print("Controller", prefix);
    }

    public abstract void handleInEvent(WishFulEvent ev);

    public abstract void onDestroy();
}
