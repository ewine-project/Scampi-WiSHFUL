package wishful_framework.Events;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import de.tu_berlin.tkn.ewine.WishfulMessage;
import wishful_framework.WishFulEntity;
import wishful_framework.WishFulEvent;

public class TestEvent extends WishFulEvent {
    private static final String TAG =  "TestEvent";

    public static final int TEST_DEFAULT        = 0x00;
    public static final int TEST_WIFI_START_AP  = 0x01;
    public static final int TEST_WIFI_STOP_AP   = 0x02;
    public static final int TEST_WIFI_JOIN_AP   = 0x03;
    public static final int TEST_WIFI_LEAVE_AP  = 0x04;

    public static final int TEST_JOIN_NEXUS5_1    = 0xC1;
    public static final int TEST_JOIN_G2_2        = 0xC2;

    public TestEvent() {
    }

    @Override
    public boolean parseWishFulMessage(WishfulMessage msg) {
        return true;
    }

    @Override
    public JSONObject serialize(WishFulEntity requestingEntity) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("testKey", "TestValue");

        } catch (JSONException ex) {
            Log.e(TAG, "serialize exception");
            return null;
        }
        return jsonObject;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
