package wishful_framework.Modules;

import de.tu_berlin.tkn.ewine.WishFulService;
import wishful_framework.Events.TestEvent;
import wishful_framework.WishFulEvent;
import wishful_framework.WishFulModule;

public class TestModule extends WishFulModule {
    private static final String TAG =  "TestModule";

    public TestModule(WishFulService wishFulService, String name) {
        super(wishFulService);

        this.name = name;

        registerInEvent(TestEvent.class);
    }

    @Override
    public void handleInEvent(WishFulEvent ev) {

    }

    @Override
    public void onDestroy() {

    }
}
