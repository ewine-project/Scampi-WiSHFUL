package wishful_framework.Events;


import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import de.tu_berlin.tkn.ewine.WishfulMessage;
import wishful_framework.Modules.D2DContentControlModule;
import wishful_framework.WishFulEntity;

public class D2DExchangeContentCommandEvent extends CommandEvent {
    private static final String TAG =  "D2DExchangeContentCmd";

    List<D2DExchangeContentCommandEvent.Command> commandList;

    public D2DExchangeContentCommandEvent() {
        this.commandList = new LinkedList<D2DExchangeContentCommandEvent.Command>();
    }

    @Override
    public boolean parseWishFulMessage(WishfulMessage msg) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(new String(msg.getContent(), "UTF-8"));

            JSONArray scheduleListArray = jsonObject.getJSONArray("scheduleList");

            for (int i=0; i<scheduleListArray.length(); i++) {
                JSONObject scheduleEntry = (JSONObject) scheduleListArray.get(i);

                String destDeviceId = scheduleEntry.getString("destDeviceId");
                String contentId = scheduleEntry.getString("contentId");
                long time;
                try {
                    time = Long.parseLong(scheduleEntry.getString("time"));
                } catch(NumberFormatException ex) {
                    Log.e(TAG, "parseWishFulMessage cannot parse time "+scheduleEntry.getString("time")); return false;
                }
                this.commandList.add(new Command(destDeviceId, contentId, time));
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
        return this.getClass().getSimpleName();
    }

    @Override
    public JSONObject serialize(WishFulEntity requestingEntity) {
        //will not be serialized..
        return null;
    }

    public static class Command {
        String destDeviceId;
        String contentId;
        long time;

        public Command(String destDeviceId, String contentId, long time) {
            this.destDeviceId = destDeviceId;
            this.contentId    = contentId;
            this.time         = time;
        }
    }
}
