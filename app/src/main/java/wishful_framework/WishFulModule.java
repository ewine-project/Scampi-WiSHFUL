package wishful_framework;

import de.tu_berlin.tkn.ewine.WishFulService;

public abstract class WishFulModule extends WishFulEntity {
    public static final String packageName = "wishful_framework.Modules";

    public WishFulModule(WishFulService wishFulService) {
        super();
        this.wishFulService = wishFulService;
    }

    public void print(String prefix) {
        super.print("Module", prefix);
    }

    public abstract void handleInEvent(WishFulEvent ev);

    public abstract void onDestroy();
}
