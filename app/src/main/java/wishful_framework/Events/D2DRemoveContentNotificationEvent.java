package wishful_framework.Events;


import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

import de.tu_berlin.tkn.ewine.WishfulMessage;
import wishful_framework.Modules.D2DContentControlModule;
import wishful_framework.WishFulEntity;
import wishful_framework.WishFulEvent;

public class D2DRemoveContentNotificationEvent extends WishFulEvent {
    private static final String TAG =  "D2DRemoveContentEvent";

    public List<D2DContentControlModule.Content> contentList;
    public boolean deltaUpdate;

    public D2DRemoveContentNotificationEvent(boolean deltaUpdate) {
        this.contentList = new LinkedList<D2DContentControlModule.Content>();
        this.deltaUpdate = deltaUpdate;
    }

    @Override
    public boolean parseWishFulMessage(WishfulMessage msg) {
        return true;
    }

    @Override
    public JSONObject serialize(WishFulEntity requestingEntity) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("srcDevUUID", requestingEntity.getWishFulService().getUUID());
            jsonObject.put("deltaUpdate", String.valueOf(this.deltaUpdate));

            JSONArray contentArray = new JSONArray();
            for (D2DContentControlModule.Content content : this.contentList) {
                contentArray.put(content.id);
            }
            jsonObject.put("contentIdList", contentArray);

        } catch (JSONException ex) {
            Log.e(TAG, "serialize exception");
            return null;
        }
        return jsonObject;
    }

    @Override
    public String toString() {
        String s = this.getClass().getSimpleName()+" deltaUpdate "+this.deltaUpdate+" contentList ";
        for (D2DContentControlModule.Content content : this.contentList) {
            s += content.id+" ";
        }
        return s;
    }
}
