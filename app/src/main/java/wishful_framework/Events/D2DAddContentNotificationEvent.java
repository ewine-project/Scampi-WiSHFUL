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

public class D2DAddContentNotificationEvent extends WishFulEvent {

    private static final String TAG =  "D2DAddContentEvent";

    public List<D2DContentControlModule.Content> contentList;
    public boolean deltaUpdate;

    public D2DAddContentNotificationEvent(boolean deltaUpdate) {
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
                JSONObject contentObject = new JSONObject();

                contentObject.put("contentId", content.id);
                contentObject.put("size", content.size);
                contentObject.put("type", "TYPE_PHOTO");

                JSONArray tagArray = new JSONArray();
                for (D2DContentControlModule.Tag tag : content.tagList) {
                    JSONObject jsonTag = new JSONObject();
                    jsonTag.put("name", tag.name);
                    jsonTag.put("quality", tag.quality);

                    tagArray.put(jsonTag);
                }
                contentObject.put("tagList", tagArray);

                contentArray.put(contentObject);
            }

            jsonObject.put("contentList", contentArray);
        } catch (JSONException ex) {
            Log.e(TAG, "serialize exception");
            return null;
        }
        return jsonObject;
    }

    @Override
    public String toString() {
        String s = this.getClass().getSimpleName()+" deltaUpdate "+this.deltaUpdate;
        for (D2DContentControlModule.Content content : this.contentList) {
            String tagString = "";
            for (D2DContentControlModule.Tag tag : content.tagList) {
                tagString += tag.name + "-" + tag.quality + "  ";
            }
            if (s.length() > this.getClass().getSimpleName().length()) {
                s += "\n";
            }
            s+= " contentId " + content.id + " tagList " + tagString;
        }
        return s;
    }
}
