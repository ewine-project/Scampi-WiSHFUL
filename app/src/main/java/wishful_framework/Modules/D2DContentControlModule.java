package wishful_framework.Modules;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.tu_berlin.tkn.ewine.WishFulService;
import fi.tkk.netlab.dtn.scampi.applib.SCAMPIMessage;
import wishful_framework.Events.D2DAddContentNotificationEvent;
import wishful_framework.Events.D2DAddInterestNotificationEvent;
import wishful_framework.Events.D2DExchangeContentCommandEvent;
import wishful_framework.Events.D2DInfoRequestCommandEvent;
import wishful_framework.Events.D2DRemoveContentNotificationEvent;
import wishful_framework.Events.D2DRemoveInterestNotificationEvent;
import wishful_framework.Events.TestEvent;
import wishful_framework.WishFulEvent;
import wishful_framework.WishFulModule;


public class D2DContentControlModule extends WishFulModule {

    private static final String TAG =  "D2DWiFiControlModule";

    private List<Content> contentList;
    private List<String> interestList;

    private int testIndex = 0;

    public D2DContentControlModule(WishFulService wishFulService, String name) {
        super(wishFulService);

        this.name = name;

        this.contentList = new LinkedList<Content>();
        this.interestList = new LinkedList<String>();

        registerInEvent(D2DExchangeContentCommandEvent.class);
        registerInEvent(D2DInfoRequestCommandEvent.class);
        registerInEvent(TestEvent.class);
    }

    @Override
    public void handleInEvent(WishFulEvent ev) {
        Log.v(TAG, "handleInEvent event " + ev.getClass());

        if (ev instanceof D2DExchangeContentCommandEvent) {
            // for now this will be ignored, since in Y1 specifying message exchange will not work with scampi

        } else if (ev instanceof D2DInfoRequestCommandEvent) {
            sendInfo();

        } else if (ev instanceof TestEvent) {
            //send a dummy
            runTest();

        } else {
            Log.e(TAG, "handleInEvent unknown event " + ev.getClass());
        }
    }

    /*
    Controller is requesting current state
    send content and interest even if lists are empty
    do not set deltaUpdate flag
     */
    private void sendInfo() {
        Log.v(TAG, "sendInfo() contentListSize "+this.contentList.size()+" interestListSize "+this.interestList.size());

        D2DAddContentNotificationEvent addContentEvent = new D2DAddContentNotificationEvent(false);
        addContentEvent.contentList = this.contentList;
        this.sendEvent(addContentEvent);

        D2DAddInterestNotificationEvent addInterestEvent = new D2DAddInterestNotificationEvent(false);
        addInterestEvent.interestList = this.interestList;
        this.sendEvent(addInterestEvent);
    }

    /**
     * Full content vector update, i.e., not a delta update.
     *
     * @param contents
     *      Full content vector.
     */
    public final void fullContent( final Collection <Content> contents ) {
        Log.d( TAG, "Sending full content update with " + contents.size() + " item(s)." );

        this.contentList.clear();
        this.contentList.addAll(contents);

        final D2DAddContentNotificationEvent addEvent = new D2DAddContentNotificationEvent( false );
        addEvent.contentList.addAll( contents );
        this.sendEvent( addEvent );
    }

    /**
     * Delta content update for added content.
     *
     * @param contents
     *      New content.
     */
    public void contentAdded( final Collection <Content> contents ) {
        Log.d( TAG, "Sending delta content update with " + contents.size() + " item(s) added." );

        this.contentList.addAll(contents);

        // delta update flag set
        final D2DAddContentNotificationEvent addEvent = new D2DAddContentNotificationEvent( true );
        addEvent.contentList.addAll( contents );
        this.sendEvent( addEvent );
    }

    /**
     * Delta content update for removed content.
     *
     * @param contents
     *      Removed content.
     */
    public void contentRemoved( final Collection <Content> contents ) {
        Log.d( TAG, "Sending delta content update with " + contents.size() + " item(s) removed." );

        for (Content removedContent : contents){
            Iterator<Content> iter = contentList.iterator();
            while (iter.hasNext()) {
                Content content = iter.next();
                if (content.id.equalsIgnoreCase(removedContent.id)) {
                    iter.remove();
                    break;
                }
            }
        }

        // delta update flag set
        final D2DRemoveContentNotificationEvent removeEvent
                = new D2DRemoveContentNotificationEvent( true );
        removeEvent.contentList.addAll( contents );
        this.sendEvent( removeEvent );
    }

    /**
     * Full interest update, i.e., not a delta update.
     *
     * @param tags
     *      All the tags that the local user is interested in.
     */
    public final void fullInterest( final Collection <String> tags ) {
        Log.d( TAG, "Sending full interest update with " + tags.size() + " item(s)." );

        this.interestList.clear();
        this.interestList.addAll(tags);

        final D2DAddInterestNotificationEvent event = new D2DAddInterestNotificationEvent( false );
        event.interestList.addAll( tags );
        this.sendEvent( event );
    }

    /**
     * Delta interest update to add new interest.
     *
     * @param tag
     *      Tag in which the local user is interested.
     */
    public final void addInterest( final String tag ) {
        Log.d( TAG, "Adding interest for '" + tag + "'." );

        this.interestList.add(tag);

        final D2DAddInterestNotificationEvent event = new D2DAddInterestNotificationEvent( true );
        event.interestList.add( tag );
        this.sendEvent( event );
    }

    /**
     * Delta interest update to remove an interest.
     *
     * @param tag
     *      Tag in which the local user is no longer interested.
     */
    public final void removeInterest( final String tag ) {
        Log.d( TAG, "Removing interest for '" + tag + "'." );

        Iterator<String> iter = interestList.iterator();
        while (iter.hasNext()) {
            String tmpTag = iter.next();
            if (tmpTag.equalsIgnoreCase(tag)) {
                iter.remove();
                break;
            }
        }

        final D2DRemoveInterestNotificationEvent event
                = new D2DRemoveInterestNotificationEvent( true );
        event.interestList.add( tag );
        this.sendEvent( event );
    }

    private void runTest() {
        switch(testIndex) {
            //remove non existing content
            case 0: {
                Log.d(TAG, "*** RunTest0 removing non existing content");
                D2DRemoveContentNotificationEvent contentRemEvent = new D2DRemoveContentNotificationEvent(true);
                D2DContentControlModule.Content content = new D2DContentControlModule.Content("someUUID", 500);
                content.addTag("cat", 0.8);
                content.addTag("dog", 0.2);
                contentRemEvent.contentList.add(content);
                this.sendEvent(contentRemEvent);
                break;
            }
            //add content
            case 1: {
                Log.d(TAG, "*** RunTest1 adding content");
                D2DAddContentNotificationEvent contentAddEvent = new D2DAddContentNotificationEvent(false);

                D2DContentControlModule.Content content = new D2DContentControlModule.Content("someUUID", 500);
                content.addTag("cat", 0.8);
                content.addTag("dog", 0.2);
                contentAddEvent.contentList.add(content);

                content = new D2DContentControlModule.Content("anotherUUID", 1500);
                content.addTag("bird", 0.8);
                content.addTag("bear", 0.2);
                contentAddEvent.contentList.add(content);

                content = new D2DContentControlModule.Content("blablaUUID", 1500);
                content.addTag("mouse", 0.8);
                content.addTag("horse", 0.2);
                contentAddEvent.contentList.add(content);

                this.sendEvent(contentAddEvent);
                break;
            }
            //remove  existing content
            case 2: {
                Log.d(TAG, "*** RunTest2 removing existing content");
                D2DRemoveContentNotificationEvent contentRemEvent = new D2DRemoveContentNotificationEvent(true);
                D2DContentControlModule.Content content = new D2DContentControlModule.Content("someUUID", 500);
                contentRemEvent.contentList.add(content);
                this.sendEvent(contentRemEvent);

                break;
            }
            //test update flag
            case 3: {
                Log.d(TAG, "*** RunTest3 add content empty with update=false");
                D2DAddContentNotificationEvent contentAddEvent = new D2DAddContentNotificationEvent(false);
                this.sendEvent(contentAddEvent);

                break;
            }
            /*
                Interest adding / removing
             */
            //remove non existing interest
            case 4: {
                Log.d(TAG, "*** RunTest4 removing non existing interest");
                D2DRemoveInterestNotificationEvent remEvent = new D2DRemoveInterestNotificationEvent(true);
                remEvent.interestList.add("test");
                remEvent.interestList.add("test2");
                this.sendEvent(remEvent);
                break;
            }
            //add interest
            case 5:{
                Log.d(TAG, "*** RunTest5 adding interest");
                D2DAddInterestNotificationEvent addEvent = new D2DAddInterestNotificationEvent(true);
                addEvent.interestList.add("test");
                addEvent.interestList.add("test2");
                addEvent.interestList.add("test3");
                this.sendEvent(addEvent);
                break;
            }
            //remove existing interest
            case 6: {
                Log.d(TAG, "*** RunTest6 removing  existing interest");
                D2DRemoveInterestNotificationEvent remEvent = new D2DRemoveInterestNotificationEvent(true);
                remEvent.interestList.add("test3");
                remEvent.interestList.add("test2");
                this.sendEvent(remEvent);
                break;
            }
            default:
        }
        testIndex = ++testIndex % 7;
    }

    @Override
    public void onDestroy() {

    }

    public static class Tag {
        public String name;
        public double quality;

        public Tag(String name, double quality) {
            this.name = name;
            this.quality = quality;
        }
    }

    public static class Content {
        public String id;
        public long size;
        public List<Tag> tagList;

        public Content(String id, long size) {
            this.tagList = new ArrayList<Tag>();
            this.id = id;
            this.size = size;
        }

        public void addTag(Tag t) {
            tagList.add(t);
        }
        public void addTag(String name, double quality)  {
            tagList.add(new Tag(name, quality));
        }
    }
}
