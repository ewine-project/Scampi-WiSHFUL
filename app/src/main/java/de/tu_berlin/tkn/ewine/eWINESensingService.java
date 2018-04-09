package de.tu_berlin.tkn.ewine;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;



public class eWINESensingService extends Service {

    public static final String TAG = "eWINESense";
    public static final String INTENT_SERVICE_STARTED = "EWINE_SENSING_SERVICE_STARTED";
    public static final String INTENT_SERVICE_STOPPED = "EWINE_SENSING_SERVICE_STOPPED";

    private boolean locationUpdatesRequested = false;
    protected static eWINESensingService serviceInstance = null;

    protected static WishFulService wishFulServiceInstance = null;

    /*
        Service related
     */

    /** indicates how to behave if the service is killed */
    private int mStartMode =  START_STICKY; //START_STICKY_COMPATIBILITY

    /** interface for clients that bind */
    private IBinder mBinder;

    /** indicates whether onRebind should be used */
    private boolean mAllowRebind;

    public static eWINESensingService getInstance() {
        return serviceInstance;
    }
    public static void setWishfulServiceInstance(WishFulService instance) {
        wishFulServiceInstance = instance;
    }

    /** Called when the service is being created. */
    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate");
        super.onCreate();

        this.serviceInstance = this;

        //todo add location support
    }

    /** The service is starting, due to a call to startService() */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");

        //notify app that instance is available
        sendBroadcast(new Intent(eWINESensingService.INTENT_SERVICE_STARTED));

        //todo report info to wishful agent server
        //- location
        // - device info

        return mStartMode;
    }

    /** A client is binding to the service with bindService() */
    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");

        //return mBinder;
        return null;
    }

    /** Called when all clients have unbound with unbindService() */
    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG, "onUnbind");

        return mAllowRebind;
    }

    /** Called when a client is binding to the service with bindService()*/
    @Override
    public void onRebind(Intent intent) {

    }

    /** Called when The service is no longer used and is being destroyed */
    @Override
    public void onDestroy() {
        super.onDestroy();

        this.serviceInstance = null;
        sendBroadcast(new Intent(eWINESensingService.INTENT_SERVICE_STOPPED));

    }

}

