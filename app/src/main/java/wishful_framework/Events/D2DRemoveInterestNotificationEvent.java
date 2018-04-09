package wishful_framework.Events;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

import de.tu_berlin.tkn.ewine.WishfulMessage;
import wishful_framework.WishFulEntity;
import wishful_framework.WishFulEvent;


public class D2DRemoveInterestNotificationEvent extends WishFulEvent {
    private static final String TAG =  "D2DRemInterestEvent";

    public List<String> interestList;
    public boolean deltaUpdate;

    public D2DRemoveInterestNotificationEvent(boolean deltaUpdate) {

        this.interestList = new LinkedList<String>();
        this.deltaUpdate  = deltaUpdate;
    }

    @Override
    public boolean parseWishFulMessage(WishfulMessage msg) {
        return true;
    }

    @Override
    public JSONObject serialize(WishFulEntity requestingEntity) {
        JSONObject jsonObject = new JSONObject();
        try {
            //ToDo add missing fields
            jsonObject.put("srcDevUUID", requestingEntity.getWishFulService().getUUID());
            jsonObject.put("deltaUpdate", String.valueOf(this.deltaUpdate));

            JSONArray tagArray = new JSONArray();
            for (String tag : interestList) {
                tagArray.put(tag);
            }
            jsonObject.put("tagList", tagArray);

        } catch (JSONException ex) {
            Log.e(TAG, "serialize exception");
            return null;
        }
        return jsonObject;
    }

    @Override
    public String toString() {
        String tagString = this.getClass().getSimpleName()+" deltaUpdate "+this.deltaUpdate+" interestList ";
        for (String tag : interestList) {
            tagString += tag+" ";
        }
        return tagString;
    }
}
