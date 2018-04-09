package de.tu_berlin.tkn.ewine;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.spacetimenetworks.scampiandroidlib.AndroidCellularMonitorPlugin;
import com.spacetimenetworks.scampiandroidlib.ScampiService;
import com.spacetimenetworks.scampiandroidlib.ServiceConfig;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import fi.tkk.netlab.dtn.scampi.core.Core;
import fi.tkk.netlab.dtn.scampi.core.monitor.SubscriptionRecord;
import wishful_framework.Events.TestEvent;
import wishful_framework.Modules.D2DContentControlModule;

public class MainActivity extends AppCompatActivity {

    //============================================================================================//
    // Static vars
    //============================================================================================//
    public static final String TAG = "eWINEApp";

    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION    = 0x01;
    private static final int PERMISSION_REQUEST_ACCESS_COARSE_LOCATION  = 0x02;
    private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE   = 0x03;
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE  = 0x04;
    private static final int PERMISSION_REQUEST_WRITE_SETTINGS          = 0x05;

    private static final int PERMISSION_REQUEST_READ_PHONE_STATE        = 0x06;

    public static final String DEFAULT_MONITOR_ADDRESS = "52.212.207.78";
    public static final int DEFAULT_MONITOR_PORT = 7676;
    //============================================================================================//


    //============================================================================================//
    // Instance vars
    //============================================================================================//
    private boolean isRunningEWINESense;
    private boolean isRunningWishful;

    private eWINESensingService eWINESensingServiceInstance;

    private WishFulService wishFulServiceInstance;
    private ServiceConnection wishfulServiceConnection;
    private boolean wishFulServiceBound = false;

    private D2DContentControlModule contentControlModule;

    private BReceiver bReceiver = null;

    private MainActivity that = this;

    private FrameLayout contentFrame;

    private Controller currentController;
    private final ScampiController scampiController = new ScampiController();
    private final TestController testController = new TestController();
    private final WishFulController wishFulController = new WishFulController();

    protected LinkedList<Spannable> wishfulDebugList = new LinkedList<Spannable>();
    protected int wishfulDebugListLimit = 5;

    private TextView currentTab;

    private ScampiService scampiService;
    private ServiceConnection serviceConnection;


    // Colors
    private int tabBackgroundColor;
    private int tabTextSelectedColor;
    private int tabTextColor;
    //============================================================================================//


    //============================================================================================//
    // Lifecycle
    //============================================================================================//
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        // Setup references
        this.tabBackgroundColor = getResources().getColor( R.color.ewineDarkGrey );
        this.tabTextSelectedColor = getResources().getColor( R.color.ewineWhite );
        this.tabTextColor = getResources().getColor( R.color.ewineLightGrey );
        this.contentFrame = ( FrameLayout )super.findViewById( R.id.contentFrame );

        // Setup the initial view
        this.contentFrame.addView( this.scampiController.getView() );
        this.currentController = this.scampiController;
        this.currentTab = ( TextView ) super.findViewById( R.id.scampiTab );

        super.findViewById( R.id.scampiTab )
                .setOnClickListener( createTabHandler( R.id.scampiTab, this.scampiController ) );
        super.findViewById( R.id.testTab )
                .setOnClickListener( createTabHandler( R.id.testTab, this.testController ) );
        super.findViewById( R.id.wishfulTab )
                .setOnClickListener( createTabHandler( R.id.wishfulTab, this.wishFulController ) );

        // Start ScampiService
        this.doStartService();

        isRunningEWINESense     = false;
        isRunningWishful        = false;

        eWINESensingServiceInstance = null;
        wishFulServiceInstance      = null;
        wishfulServiceConnection    = getWishfulServiceConnection();

        bReceiver = new BReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(eWINESensingService.INTENT_SERVICE_STARTED);
        intentFilter.addAction(eWINESensingService.INTENT_SERVICE_STOPPED);
        intentFilter.addAction(WishFulService.INTENT_SERVICE_STARTED);
        intentFilter.addAction(WishFulService.INTENT_SERVICE_STOPPED);
        intentFilter.addAction(WishFulService.INTENT_DEBUG_TEXT);

        registerReceiver(bReceiver, intentFilter);

        if (Build.VERSION.SDK_INT >= 23) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "missing permission ACCESS_FINE_LOCATION");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_ACCESS_FINE_LOCATION);

            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "missing permission ACCESS_COARSE_LOCATION");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_ACCESS_COARSE_LOCATION);

            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "missing permission READ_EXTERNAL_STORAGE");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);

            }  else if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "missing permission WRITE_EXTERNAL_STORAGE");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);

            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "missing permission READ_PHONE_STATE");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_REQUEST_READ_PHONE_STATE);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                autoFillMonitorAddress();
            }

            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite( this ) ) {
                Log.v(TAG, "missing permission WRITE_SETTINGS");

                this.askWriteSettingsPermissions();
            }
            //since only one permission at a time can be requested, the order is the following
            //fine location -> coarse location -> read sd -> write sd -> read phone state
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        this.doBindService();
    }

    @Override
    protected void onStop() {
        super.onStop();

        this.doUnbindService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (wishFulServiceBound) {
            unbindService(wishfulServiceConnection);
            wishFulServiceBound = false;

        }
        stopService(new Intent(that, WishFulService.class));

        LocalBroadcastManager.getInstance(this).unregisterReceiver(bReceiver);
    }
    //============================================================================================//

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "onRequestPermissionsResult granted ACCESS_FINE_LOCATION");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_ACCESS_COARSE_LOCATION);
                } else {
                    Log.e(TAG, "onRequestPermissionsResult DENIED ACCESS_FINE_LOCATION");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
                }
                return;
            }
            case PERMISSION_REQUEST_ACCESS_COARSE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "onRequestPermissionsResult granted ACCESS_COARSE_LOCATION");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
                } else {
                    Log.e(TAG, "onRequestPermissionsResult DENIED ACCESS_COARSE_LOCATION");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_ACCESS_COARSE_LOCATION);
                }
                return;
            }
            case PERMISSION_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "onRequestPermissionsResult granted READ_EXTERNAL_STORAGE");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
                } else {
                    Log.e(TAG, "onRequestPermissionsResult DENIED READ_EXTERNAL_STORAGE");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
                }
                return;
            }
            case PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "onRequestPermissionsResult granted WRITE_EXTERNAL_STORAGE");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_REQUEST_READ_PHONE_STATE);
                } else {
                    Log.e(TAG, "onRequestPermissionsResult DENIED WRITE_EXTERNAL_STORAGE");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
                }
                return;
            }
            case PERMISSION_REQUEST_READ_PHONE_STATE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "onRequestPermissionsResult granted READ_PHONE_STATE");
                    autoFillMonitorAddress();

                } else {
                    Log.e(TAG, "onRequestPermissionsResult DENIED READ_PHONE_STATE");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_REQUEST_READ_PHONE_STATE);
                }
                return;
            }
        }
    }

    //=========================================================================//
    // Private - Permission handling
    //=========================================================================//
    private PermissionResult askWriteSettingsPermissions() {
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite( this ) ) {
            new AlertDialog.Builder( this )
                    .setMessage( "Allow reading/writing the system settings? "
                            + "Necessary to set up access points." )
                    .setPositiveButton( "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick( DialogInterface dialog, int which ) {
                            Intent intent = new Intent( Settings.ACTION_MANAGE_WRITE_SETTINGS );
                            intent.setData( Uri.parse( "package:" + getPackageName() ) );
                            intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );

                            startActivityForResult( intent, PERMISSION_REQUEST_WRITE_SETTINGS );
                        }
                    } ).show();
            return PermissionResult.ASKED_USER;
        }
        return PermissionResult.HAVE_PERMISSIONS;
    }

    @Override
    protected void onActivityResult(
            final int requestCode,
            final int resultCode,
            final Intent data ) {
        switch ( requestCode ) {
            case PERMISSION_REQUEST_WRITE_SETTINGS:
                Log.d( TAG, "Got WRITE_SETTINGS permission." );
                //this.setupSoftApManager();
                break;
        }
    }

    private enum PermissionResult {
        HAVE_PERMISSIONS,
        ASKED_USER
    }
    //=========================================================================//

    private void autoFillMonitorAddress() {
        String address = "";
        TelephonyManager mngr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);

        int port = DEFAULT_MONITOR_PORT;
        String imei = "n/a";
        try {
            imei = mngr.getDeviceId();
			//map different imei to different ports

        } catch(SecurityException ex) {
            Log.d(TAG, "Cannot read IMEI");
        }
        Log.v(TAG, "IMEI "+imei+" using port "+port);


        this.scampiController.setMonitorString(DEFAULT_MONITOR_ADDRESS+":"+port);
    }

    private void runTest(int testId) {
        if (wishFulServiceInstance == null) {
            Log.v(TAG, "runTest wishFulServiceInstance not started");
            return;
        }
        wishFulServiceInstance.runTest(testId);
    }

    private void setServiceInstances() {
        if (eWINESensingServiceInstance != null) {
            eWINESensingServiceInstance.setWishfulServiceInstance(wishFulServiceInstance);

        }
        if (wishFulServiceInstance != null) {
            wishFulServiceInstance.setEWINESensingServiceInstance(eWINESensingServiceInstance);

            this.contentControlModule = (D2DContentControlModule)wishFulServiceInstance.getWishFulEntity("D2DContentControlModule");
            if (this.contentControlModule == null) {
                Log.e(TAG, "setServiceInstances contentControlModule cannot be found");
            }
        }
    }

    public static boolean deleteDirectory(File path) {
        if( path.exists() ) {
            File[] files = path.listFiles();
            if (files == null) {
                return true;
            }
            for(int i=0; i<files.length; i++) {
                if(files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                }
                else {
                    files[i].delete();
                }
            }
        }
        return( path.delete() );
    }

    private class BReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            //Log.v(TAG, "BReiceiver action "+action);

            if (action.equals(eWINESensingService.INTENT_SERVICE_STARTED)) {
                eWINESensingServiceInstance = eWINESensingService.getInstance();
                isRunningEWINESense = true;
                setServiceInstances();

            } else if (action.equals(eWINESensingService.INTENT_SERVICE_STOPPED)) {
                eWINESensingServiceInstance = null;
                isRunningEWINESense = false;
                setServiceInstances();

            } else if (action.equals(WishFulService.INTENT_SERVICE_STARTED)) {
                setServiceInstances();
                isRunningWishful = true;

                final Button button = ( Button ) findViewById( R.id.wishfulStartButton );
                if (button != null) {
                    button.setEnabled(true);
                }

            } else if (action.equals(WishFulService.INTENT_SERVICE_STOPPED)) {
                setServiceInstances();
                isRunningWishful = false;

                //enable button
                final Button button = ( Button ) findViewById( R.id.wishfulStartButton );
                if (button != null) {
                    button.setEnabled(true);
                }


            } else if (action.equals(WishFulService.INTENT_DEBUG_TEXT)) {
                String msg = intent.getStringExtra("text");
                int msgType = intent.getIntExtra("type", WishFulService.MSG_TYPE_VERBOSE);

                final Spannable spanMsg = new SpannableString(msg+"\n");
                switch (msgType) {
                    case WishFulService.MSG_TYPE_ERROR:
                        spanMsg.setSpan(new ForegroundColorSpan(Color.RED), 0, spanMsg.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case WishFulService.MSG_TYPE_DEBUG:
                    case WishFulService.MSG_TYPE_VERBOSE:
                        break;
                    default:
                }

                synchronized (wishfulDebugList) {
                    if (wishfulDebugList.size() > wishfulDebugListLimit) {
                        wishfulDebugList.removeLast();
                    }
                    wishfulDebugList.add(spanMsg);
                }
                TextView tv = (TextView)findViewById(R.id.wishfulDebugTextView);
                if (tv != null) {
                    tv.append(spanMsg);

                    wishFulDebugScrollBottom();
                }
            }
        }
    }

    //============================================================================================//
    // Private - GUI management
    //============================================================================================//
    private View.OnClickListener createTabHandler(
            final int tabId,
            final Controller controller ) {
        return new View.OnClickListener() {
            private final TextView tabView = ( TextView ) findViewById( tabId );

            @Override
            public void onClick( final View v ) {
                if ( currentController == controller ) return;

                // Clear old state
                currentTab.setTypeface( null, Typeface.NORMAL );
                currentTab.setBackgroundColor( tabBackgroundColor );
                currentTab.setTextColor( tabTextColor );
                contentFrame.removeAllViews();

                // Set new state
                currentController = controller;
                currentTab = this.tabView;
                currentTab.setTypeface( null, Typeface.BOLD );
                //currentTab.setBackgroundColor( tabSelectedColor );
                currentTab.setBackground( ContextCompat.getDrawable(
                        MainActivity.this, R.drawable.bright_red_gradient ) );
                currentTab.setTextColor( tabTextSelectedColor );
                contentFrame.addView( controller.getView() );
            }
        };
    }

    private void wishFulDebugScrollBottom() {
        if (wishFulController.autoScrollEnabled) {
            final ScrollView scrollview = ((ScrollView) findViewById(R.id.wishhfulScrollView));
            if (scrollview != null) {

                scrollview.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollview.fullScroll(ScrollView.FOCUS_DOWN);
                        //scrollview.scrollTo(0, scrollview.getBottom());
                    }
                });
            }
        }
    }

    //============================================================================================//
    //============================================================================================//
    // Public - GUI management
    //============================================================================================//
    public void addWishFulDebug(Spannable span) {
        this.wishfulDebugList.add(span);
    }
    //============================================================================================//
    // Private Service Handling
    //============================================================================================//
    private ServiceConnection getServiceConnection() {
        return new ServiceConnection() {

            @Override
            public void onServiceConnected(
                    final ComponentName className,
                    final IBinder service ) {
                Log.d( TAG, "onServiceConnected" );
                MainActivity.this.scampiService =
                        ( ( ScampiService.ScampiBinder ) service ).getService();

                // Pass the ScampiService to the ScampiController that will handle it
                scampiController.setScampiService( scampiService );
            }

            @Override
            public void onServiceDisconnected( ComponentName arg0 ) {
                Log.d( TAG, "onServiceDisconnected" );
                MainActivity.this.scampiService = null;
            }

        };
    }

    private ServiceConnection getWishfulServiceConnection() {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(final ComponentName className, final IBinder service ) {
                Log.d( TAG, "onServiceConnected Wishful" );

                MainActivity.this.wishFulServiceInstance = ( ( WishFulService.wishfulBinder ) service ).getService();
                wishFulServiceBound = true;

                //enable button
                final Button button = ( Button ) findViewById( R.id.wishfulStartButton );
                if (button != null) {
                    button.setEnabled(true);
                }
            }

            @Override
            public void onServiceDisconnected( ComponentName arg0 ) {
                Log.d( TAG, "onServiceDisconnected Wishful" );
                MainActivity.this.wishFulServiceInstance = null;
                wishFulServiceBound = false;

                //enable button
                final Button button = ( Button ) findViewById( R.id.wishfulStartButton );
                if (button != null) {
                    button.setEnabled(true);
                }
            }
        };
    }

    private void doStartService() {
        this.startService( new Intent( this, ScampiService.class ) );
    }

    private void doBindService() {
        this.serviceConnection = getServiceConnection();
        this.bindService( new Intent( this, ScampiService.class ), this.serviceConnection, Context.BIND_AUTO_CREATE );

        this.bindService(new Intent(this, WishFulService.class), this.wishfulServiceConnection, 0);
    }

    private void doUnbindService() {
        if ( this.serviceConnection != null ) {
            super.unbindService(this.serviceConnection);
            this.serviceConnection = null;
        }
    }
    //============================================================================================//


    //============================================================================================//
    // Interface for Controllers that manage the GUI views.
    //============================================================================================//
    private interface Controller {
        /**
         * Returns the view managed by this controller.
         *
         * @return
         *      the view of this controller.
         */
        View getView();
    }
    //============================================================================================//


    //============================================================================================//
    // Scampi Controller
    //--------------------------------------------------------------------------------------------//
    // Controller responsible for the Scampi portion of the GUI. Basically like Android
    // fragments, but with a less insane design.
    //============================================================================================//
    private final class ScampiController
    implements Controller {

        //========================================================================================//
        // Static vars (can't have actually static vars in inner classes)
        //========================================================================================//
        public final String TAG = ScampiController.class.getSimpleName();

        private final short DETAIL_VIEW_LINKS = 0;
        private final short DETAIL_VIEW_MSGS = 1;
        //========================================================================================//


        //========================================================================================//
        // Instance vars
        //========================================================================================//
        private View rootView;
        private FrameLayout detailView;

        private ImageView startButton;
        private TextView statusText;
        private TextView statsText;
        private ImageView linksButton;
        private ImageView messagesButton;
        private EditText monitorAddressField;

        private ScampiService scampiService;

        private Handler mainThread = new Handler();

        private short detailViewState = DETAIL_VIEW_LINKS;

        private final Map <String, SubscriptionRecord> subRecords = new LinkedHashMap <>();
        //========================================================================================//


        //========================================================================================//
        // API
        //========================================================================================//
        /**
         * Sets the {@code ScampiService} to be used by this controller. The MainActivity is
         * responsible for starting the service and getting a reference to it through binding. The
         * ScampiController is responsible for displaying the status of the ScampiService and
         * allowing the user to control it.
         *
         * @param scampiService
         *      The ScampiService that this controller uses.
         */
        void setScampiService( final ScampiService scampiService ) {
            // Setup the state
            this.scampiService = scampiService;
            this.scampiService.addStateChangeCallback( scampiStateChanged );
            this.scampiService.addBundleReceivedCallback( scampiBundleReceived );
            this.scampiService.addBundleRemovedCallback( scampiBundleRemoved );
            this.scampiService.addLinkDownCallback( scampiLinkDown);
            this.scampiService.addLinkUpCallback( scampiLinkUp );
            this.scampiService.addSubscriptionsChangedCallback( scampiSubsChanged );

            // Update GUI
            this.updateGuiForScampiState( scampiService.getRouterState() );
            this.updateGuiScampiStats( scampiService );
        }
        //========================================================================================//


        //========================================================================================//
        // Controller implementation
        //========================================================================================//
        @Override
        public final View getView() {
            if ( this.rootView == null ) {
                this.rootView = this.setupRootView( MainActivity.this );
            }
            return this.rootView;
        }

        public final void setMonitorString(String address) {
            if (this.monitorAddressField != null) {
                this.monitorAddressField.setText(address);
            }
        }

        //========================================================================================//


        //========================================================================================//
        // Scampi Callbacks
        //========================================================================================//
        /** Callback invoked by the ScampiService when the Scampi state changes. */
        private final ScampiService.StateChangeCallback scampiStateChanged
            = new ScampiService.StateChangeCallback() {
            @Override
            public void stateChanged( final ScampiService.RouterState routerState ) {
                mainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        updateGuiForScampiState( routerState );
                        updateGuiScampiStats( scampiService );

                        // Update detail view state
                        switch ( routerState ) {
                            case Running: {
                                detailViewState = DETAIL_VIEW_LINKS;
                                linksButton.setImageResource( R.drawable.arrows_bright_red );
                                messagesButton.setImageResource( R.drawable.folder_sharing_grey );

                                // Update subscription state
                                if ( subRecords.size() != 0 ) {
                                    Log.e( TAG, "Sub records are not empty after router " +
                                            "bootup. Clearing the records." );
                                    subRecords.clear();
                                }
                                final Collection <SubscriptionRecord> currentSubs
                                        = scampiService.getSubscriptions();
                                addRecordsToMap( currentSubs, subRecords );

                                // Update WiSHFUL with the full state.
                                if ( contentControlModule != null ) {
                                    // Content
                                    final Collection<Core.Monitor_BundleRecord> bundles
                                            = scampiService.getBundles();
                                    final Collection<D2DContentControlModule.Content> contents
                                            = createContents(bundles, 1.0);
                                    contentControlModule.fullContent( contents );

                                    // Subscriptions
                                    final Collection <String> activeSubs
                                            = getActiveSubs( subRecords );
                                    contentControlModule.fullInterest( activeSubs );

                                } else {
                                    Log.e( TAG, "No D2DContentControlModule reference found! "
                                            + "(stateChanged( Running ))");
                                }
                                break;
                            }
                            case Stopped:
                                // Update WiSHFUL to clear all the state.
                                if ( contentControlModule != null ) {
                                    contentControlModule.fullContent(
                                        Collections.<D2DContentControlModule.Content>emptyList() );
                                    contentControlModule.fullInterest(
                                        Collections.<String>emptyList() );
                                } else {
                                    Log.e( TAG, "No D2DContentControlModule reference found! "
                                            + "(stateChanged( Stopped ))");
                                }
                                // Fall through
                            case Starting:
                            case Stopping: {
                                // Update GUI state
                                detailView.removeAllViews();
                                linksButton.setImageResource( R.drawable.arrows_dark_grey );
                                messagesButton.setImageResource(
                                R.drawable.folder_sharing_dark_grey );

                                // Update subscription state
                                subRecords.clear();
                                break;
                            }
                            default: {
                                Log.e( TAG, "Unknown router state: " + routerState );
                            }

                        }
                    }
                } );
            }
        };

        /** Callback invoked by the ScampiService when Scampi receives a new bundle. */
        private final ScampiService.BundleReceivedCallback scampiBundleReceived
            = new ScampiService.BundleReceivedCallback() {
            @Override
            public void bundleReceived(
                    final long timestamp, final String id,
                    final String senderEID, final String destinationEID,
                    final long payloadSize ) {
                mainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        // Send the new content via WiSHFUL
                        if ( contentControlModule != null ) {
                            final D2DContentControlModule.Content content
                                = createContent( id, payloadSize, destinationEID, 1.0 );
                            contentControlModule.contentAdded(
                                    Collections.singletonList( content ) );
                        } else {
                            Log.e( TAG, "No D2DContentControlModule reference found! "
                                + "(bundleReceived())" );
                        }

                        // Update the GUI state
                        updateGuiScampiStats( scampiService );
                    }
                } );
            }
        };

        /** Callback invoked by the ScampiService when Scampi removes a bundle. */
        private final ScampiService.BundleRemovedCallback scampiBundleRemoved
            = new ScampiService.BundleRemovedCallback() {
            @Override
            public void bundleRemoved(
                    final long timestamp, final String id,
                    final String senderEID, final String destinationEID,
                    final long payloadSize ) {
                mainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        // Send the removed content via WiSHFUL
                        if ( contentControlModule != null ) {
                            final D2DContentControlModule.Content content
                                    = createContent( id, ( int )payloadSize, destinationEID, 1.0 );
                            contentControlModule.contentRemoved(
                                    Collections.singletonList( content ) );
                        } else {
                            Log.e( TAG, "No D2DContentControlModule reference found! "
                                + "(bundleRemoved())" );
                        }

                        // Update the GUI state
                        updateGuiScampiStats( scampiService );
                    }
                } );
            }
        };

        /** Callback invoked by the ScampiService when Scampi open a new link. */
        private final ScampiService.LinkDownCallback scampiLinkDown
            = new ScampiService.LinkDownCallback() {
            @Override
            public void linkDown( long timestamp, String remoteEID, String remoteAddress ) {
                mainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        updateGuiScampiStats( scampiService );
                    }
                } );
            }
        };

        /** Callback invoked by the ScampiService when Scampi closes a link. */
        private final ScampiService.LinkUpCallback scampiLinkUp
            = new ScampiService.LinkUpCallback() {
            @Override
            public void linkUp( long timestamp, String remoteEID, String remoteAddress ) {
                mainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        updateGuiScampiStats( scampiService );
                    }
                } );
            }
        };

        private final ScampiService.SubscriptionChangeCallback scampiSubsChanged
            = new ScampiService.SubscriptionChangeCallback() {
                @Override
                public void subscriptionChanged( final SubscriptionRecord record ) {
                    mainThread.post( new Runnable() {
                    @Override
                    public void run() {
                        updateGuiScampiStats( scampiService );

                        // Update subscription state
                        final SubscriptionRecord oldRecord
                                = subRecords.get( record.serviceName );
                        subRecords.put( record.serviceName, record );

                        // Update WiSHFUL
                        if ( contentControlModule != null ) {
                            final boolean oldActive
                                    = ( oldRecord != null && oldRecord.subscriptionCount > 0 );
                            final boolean newActive
                                    = ( record.subscriptionCount > 0 );

                            if ( oldActive && !newActive ) {
                                // Subscription removed
                                contentControlModule.removeInterest( record.serviceName );
                            } else if ( !oldActive && newActive ) {
                                // Subscription added
                                contentControlModule.addInterest( record.serviceName );
                            }
                        } else {
                            Log.e( TAG, "No D2DContentControlModule reference found! "
                                    + "(scampiSubsChanged())" );
                        }
                    }
                    } );
                }
        };
        //========================================================================================//


        //========================================================================================//
        // Private - GUI
        //========================================================================================//
        private void updateGuiForScampiState( final ScampiService.RouterState state ) {
            switch ( state ) {
                case Running: {
                    this.statusText.setText( R.string.scampi_status_running );
                    this.startButton.setImageResource( R.drawable.power2_bright_red );
                    break;
                }
                case Stopped: {
                    this.statusText.setText( R.string.scampi_status_stopped );
                    this.startButton.setImageResource( R.drawable.power2_green );
                    break;
                }
                case Starting: {
                    this.statusText.setText( R.string.scampi_status_starting );
                    this.startButton.setImageResource( R.drawable.power2_light_grey );
                    break;
                }
                case Stopping: {
                    this.statusText.setText( R.string.scampi_status_stopping );
                    this.startButton.setImageResource( R.drawable.power2_light_grey );
                    break;
                }
                default: {
                    Log.e( TAG, "Unknown ScampiService.RouterState: " + state );
                }
            }
        }

        private void updateGuiScampiStats( final ScampiService service ) {
            // Only show stats when the router is running
            if ( service.getRouterState() != ScampiService.RouterState.Running ) {
                this.statsText.setText( "" );
                return;
            }

            // Update stats text
            final int linkCount = service.getLinks().size();
            final int bundleCount = service.getBundles().size();
            final Collection <SubscriptionRecord> localSubs = service.getSubscriptions();

            this.statsText.setText( "Link count: " + linkCount
                    + "\nMessage count: " + bundleCount
                    + "\nSubscriptions: " + this.countSubscribedServices( localSubs )
            );
        }

        private int countSubscribedServices( final Collection <SubscriptionRecord> subs ) {
            int count = 0;
            for ( final SubscriptionRecord record : subs ) {
                if ( record.subscriptionCount > 0 ) {
                    count += record.subscriptionCount;
                }
            }
            return count;
        }
        //========================================================================================//


        //========================================================================================//
        // GUI Callbacks
        //========================================================================================//
        private void startButtonPushed() {
            Log.d( TAG, "startButtonPushed()" );

            // Preconditions
            if ( this.scampiService == null ) return;

            switch ( this.scampiService.getRouterState() ) {
                case Stopped: {
                    final InetSocketAddress monitorAddress = this.getMonitorAddress(
                            DEFAULT_MONITOR_ADDRESS, DEFAULT_MONITOR_PORT );
                    final AndroidCellularMonitorPlugin.Configuration monitorPluginConfig
                            = new AndroidCellularMonitorPlugin.Configuration(
                                monitorAddress.getAddress(), monitorAddress.getPort(),
                                MainActivity.this, 5, TimeUnit.SECONDS );
                    final ServiceConfig config = ServiceConfig.builder( MainActivity.this )
                                .debugLogLevel()
                                .configFileAsset( "demo.conf" )
                                .scampiPlugin( AndroidCellularMonitorPlugin.class,
                                                monitorPluginConfig )
                                .build();

                    this.scampiService.start( config );
                    break;
                }
                case Running: {
                    this.scampiService.stop();
                    break;
                }
                default: {
                    Log.d( TAG, "Start button pushed in wrong state. Ignoring." );
                }
            }

        }

        private final View.OnClickListener linksButtonPushed = new View.OnClickListener() {
            @Override
            public void onClick( final View v ) {
                Log.d( TAG, "linksButtonPushed()" );

                // Preconditions
                if ( scampiService == null
                        || scampiService.getRouterState() != ScampiService.RouterState.Running
                        || detailViewState == DETAIL_VIEW_LINKS )
                    return;

                // Clear old state
                detailView.removeAllViews();
                messagesButton.setImageResource( R.drawable.folder_sharing_grey );

                // Set new state
                detailViewState = DETAIL_VIEW_LINKS;
                linksButton.setImageResource( R.drawable.arrows_bright_red );
            }
        };

        private final View.OnClickListener messageButtonPushed = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d( TAG, "messageButtonPushed()" );
                // Preconditions
                if ( scampiService == null
                        || scampiService.getRouterState() != ScampiService.RouterState.Running
                        || detailViewState == DETAIL_VIEW_MSGS )
                    return;

                // Clear old state
                detailView.removeAllViews();
                linksButton.setImageResource( R.drawable.arrows_grey );

                // Set new state
                detailViewState = DETAIL_VIEW_MSGS;
                messagesButton.setImageResource( R.drawable.folder_sharing_bright_red );

            }
        };
        //========================================================================================//


        //========================================================================================//
        // Private
        //========================================================================================//
        private View setupRootView( final Context context ) {
            final View inflatedView = this.inflateView( context );
            this.setupGuiReferences( inflatedView );
            return inflatedView;
        }

        private View inflateView( final Context context ) {
            final LayoutInflater inflater
                    = ( LayoutInflater )context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            return inflater.inflate( R.layout.scampi_control, null );
        }

        private void setupGuiReferences( final View rootView ) {
            this.startButton = ( ImageView )rootView.findViewById( R.id.scampiControlButton );
            this.statusText = ( TextView )rootView.findViewById( R.id.scampiControlStatusText );
            this.statsText = ( TextView )rootView.findViewById( R.id.scampiStatsText );
            this.linksButton = ( ImageView )rootView.findViewById( R.id.scampiDetailLinkButton );
            this.messagesButton = ( ImageView )rootView.findViewById( R.id.scampiDetailMsgsButton );
            this.detailView = ( FrameLayout )rootView.findViewById( R.id.scampiControlDetailView );
            this.monitorAddressField = ( EditText )rootView.findViewById( R.id.scampiMonitorField );

            this.startButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick( View v ) {
                    ScampiController.this.startButtonPushed();
                }
            });
            this.linksButton.setOnClickListener( this.linksButtonPushed );
            this.messagesButton.setOnClickListener( this.messageButtonPushed );
        }

        private D2DContentControlModule.Content createContent(
                final String id,
                final long size,
                final String scampiEid /* content tag is extracted from this */,
                final double tagQuality ) {
            final D2DContentControlModule.Content content
                    = new D2DContentControlModule.Content( id, size );
            final String tag = this.tagFromEid( scampiEid );
            content.addTag( tag, tagQuality );
            return content;
        }

        private D2DContentControlModule.Content createContent(
                final Core.Monitor_BundleRecord bundle,
                final double tagQuality ) {
            return this.createContent( bundle.uniqueID, bundle.payloadSize,
                    bundle.destinationEID, tagQuality );
        }

        private Collection <D2DContentControlModule.Content> createContents(
                final Collection <Core.Monitor_BundleRecord> bundles,
                final double tagQuality ) {
            final ArrayList <D2DContentControlModule.Content> contents
                    = new ArrayList<>( bundles.size() );

            for ( final Core.Monitor_BundleRecord bundle : bundles ) {
                contents.add( createContent( bundle, tagQuality ) );
            }

            return contents;
        }

        private String tagFromEid( final String eid ) {
            // EID is of the form "dtn://scampi.service/XYZ" where XYZ is the service name.
            return eid.substring( eid.lastIndexOf( '/' ) + 1 );
        }

        private void addRecordsToMap(
                final Collection <SubscriptionRecord> records,
                final Map <String, SubscriptionRecord> subMap ) {
            for ( final SubscriptionRecord record : records ) {
                subMap.put( record.serviceName, record );
            }
        }

        private Collection <String> getActiveSubs(
                final Map <String, SubscriptionRecord> records ) {
            final Collection <String> active = new ArrayList <>( records.size() );
            for ( final SubscriptionRecord record : records.values() ) {
                if ( record.subscriptionCount > 0 ) {
                    active.add( record.serviceName );
                }
            }
            return active;
        }

        private InetSocketAddress getMonitorAddress(
                final String defaultAddress,
                final int defaultPort ) {
            // Get the user input
            final String addressString = this.monitorAddressField.getText().toString();

            // Parse port
            final String addressPart;
            final int port;
            if ( addressString.contains( ":") ) {
                int dividerLocation = addressString.lastIndexOf( ':' );
                port = Integer.parseInt( addressString.substring( dividerLocation + 1 ) );
                addressPart = addressString.substring( 0, dividerLocation );
            } else {
                port = defaultPort;
                addressPart = addressString;
            }

            // Parse address
            InetAddress address;
            if ( addressPart.length() > 0 ) {
                try {
                    address = InetAddress.getByName( addressPart );
                } catch ( UnknownHostException e ) {
                    Log.d( TAG, "Couldn't parse address '" + addressPart
                            + "', using the default address instead." );
                    address = InetAddress.getLoopbackAddress();
                }
            } else {
                try {
                    address = InetAddress.getByName( defaultAddress );
                } catch ( UnknownHostException e ) {
                    Log.d( TAG, "Couldn't parse address '" + defaultAddress
                            + "', using the default address instead." );
                    address = InetAddress.getLoopbackAddress();
                }
            }

            return new InetSocketAddress( address, port );
        }

        //========================================================================================//
    }
    //============================================================================================//


    //============================================================================================//
    // Test Controller
    //--------------------------------------------------------------------------------------------//
    // Controller for the test view.
    //============================================================================================//
    private final class TestController
    implements Controller {

        //========================================================================================//
        // Instance vars
        //========================================================================================//
        private final String TAG = TestController.class.getSimpleName();
        private View rootView;
        //========================================================================================//


        //========================================================================================//
        // API
        //========================================================================================//
        @Override
        public final View getView() {
            if ( this.rootView == null ) {
                this.rootView = setupRootView( MainActivity.this );
            }
            return this.rootView;
        }
        //========================================================================================//


        //========================================================================================//
        // Callbacks
        //========================================================================================//
        private final View.OnClickListener START_ALL_CB = new View.OnClickListener() {
            @Override
            public void onClick( final View v ) {
                Log.d( TAG, "Start all button pushed." );
            }
        };

        private final View.OnClickListener START_SENSE_CB = new View.OnClickListener() {
            @Override
            public void onClick( final View v ) {
                Log.d( TAG, "Starting eWINESenseService." );

                startService(new Intent(that, eWINESensingService.class));
            }
        };

        private final View.OnClickListener TEST_CB = new View.OnClickListener() {
            @Override
            public void onClick( final View v ) {
                runTest(TestEvent.TEST_DEFAULT);
            }
        };

        private final View.OnClickListener START_AP_CB = new View.OnClickListener() {
            @Override
            public void onClick( final View v ) {
                runTest(TestEvent.TEST_WIFI_START_AP);
            }
        };

        private final View.OnClickListener STOP_AP_CB = new View.OnClickListener() {
            @Override
            public void onClick( final View v ) {
                runTest(TestEvent.TEST_WIFI_STOP_AP);
            }
        };

        private final View.OnClickListener JOIN_AP_CB = new View.OnClickListener() {
            @Override
            public void onClick( final View v ) {
                runTest(TestEvent.TEST_WIFI_JOIN_AP);
            }
        };

        private final View.OnClickListener LEAVE_AP_CB = new View.OnClickListener() {
            @Override
            public void onClick( final View v ) {
                runTest(TestEvent.TEST_WIFI_LEAVE_AP);
            }
        };

        private final View.OnClickListener JOIN_NEXUS5_1 = new View.OnClickListener() {
            @Override
            public void onClick( final View v ) {
                runTest(TestEvent.TEST_JOIN_NEXUS5_1);
            }
        };

        private final View.OnClickListener JOIN_G2_2 = new View.OnClickListener() {
            @Override
            public void onClick( final View v ) {
                runTest(TestEvent.TEST_JOIN_G2_2);
            }
        };

        private final View.OnClickListener RESET_CB = new View.OnClickListener() {
            @Override
            public void onClick( final View v ) {
                final Button button = ( Button ) rootView.findViewById( R.id.resetButton );

                String ewineFolder = "eWINE";
                //delete scampi cache and ewine cache
                File dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS );
                File eWINERootDir = new File( dlDir, ewineFolder );

                Log.v(TAG, "eWINERootDir "+eWINERootDir.toString()+ " exist "+eWINERootDir.exists()+" isDir "+eWINERootDir.isDirectory());
                Log.v(TAG, "\tdeleted : "+ deleteDirectory(eWINERootDir));

                File scampiLocalCache = new File( dlDir, "/ScampiRouter/router/local_cache/" );
                Log.v(TAG, "scampiLocalCache "+scampiLocalCache.toString()+ " exist "+scampiLocalCache.exists()+" isDir "+scampiLocalCache.isDirectory());
                Log.v(TAG, "\tdeleted : "+ deleteDirectory(scampiLocalCache));

                File scampiPeerCache = new File( dlDir, "/ScampiRouter/router/peer_cache/" );
                Log.v(TAG, "scampiPeerCache "+scampiPeerCache.toString()+ " exist "+scampiPeerCache.exists()+" isDir "+scampiPeerCache.isDirectory());
                Log.v(TAG, "\tdeleted : "+ deleteDirectory(scampiPeerCache));
            }
        };

        //========================================================================================//


        //========================================================================================//
        // Private
        //========================================================================================//
        private View setupRootView( final Context context ) {
            final View inflatedView = this.inflateView( context );
            this.setupGuiReferences( inflatedView );
            return inflatedView;
        }

        private View inflateView( final Context context ) {
            final LayoutInflater inflater
                    = ( LayoutInflater )context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            return inflater.inflate( R.layout.activity_main, null );
        }

        private void setupGuiReferences( final View rootView ) {
            this.setCallbackForButton( rootView, R.id.startAllButton,       START_ALL_CB );
            this.setCallbackForButton( rootView, R.id.eWINESenseButton,     START_SENSE_CB );
            this.setCallbackForButton( rootView, R.id.testButton,           TEST_CB );
            this.setCallbackForButton( rootView, R.id.startAPButton,        START_AP_CB );
            this.setCallbackForButton( rootView, R.id.stopAPButton,         STOP_AP_CB );
            this.setCallbackForButton( rootView, R.id.joinAPButton,         JOIN_AP_CB );
            this.setCallbackForButton( rootView, R.id.leaveAPButton,        LEAVE_AP_CB );

            this.setCallbackForButton( rootView, R.id.joinNexus5_1,         JOIN_NEXUS5_1 );
            this.setCallbackForButton( rootView, R.id.joinG2_2,             JOIN_G2_2 );
            this.setCallbackForButton( rootView, R.id.resetButton,          RESET_CB );
        }

        private void setCallbackForButton(
                final View rootView,
                final int buttonId,
                final View.OnClickListener callback ) {
            final Button button = ( Button ) rootView.findViewById( buttonId );
            button.setOnClickListener( callback );
        }
        //========================================================================================//
    }
    //============================================================================================//

    //============================================================================================//
    // Wishful Controller
    //--------------------------------------------------------------------------------------------//
    // Controller to show wishful oebug output
    //============================================================================================//
    private final class WishFulController
            implements Controller {

        //========================================================================================//
        // Instance vars
        //========================================================================================//
        private final String TAG = WishFulController.class.getSimpleName();
        private View rootView;
        private boolean autoScrollEnabled = true;
        //========================================================================================//


        //========================================================================================//
        // API
        //========================================================================================//
        @Override
        public final View getView() {
            if ( this.rootView == null ) {
                this.rootView = setupRootView( MainActivity.this );
            }

            TextView tv = (TextView) this.rootView.findViewById(R.id.wishfulDebugTextView);
            tv.setMovementMethod(new ScrollingMovementMethod());

            tv.setText("");
            synchronized (wishfulDebugList) {
                for (Spannable span : wishfulDebugList) {
                    tv.append(span);
                }
            }
            wishFulDebugScrollBottom();

            return this.rootView;
        }
        //========================================================================================//


        //========================================================================================//
        // Callbacks
        //========================================================================================//
        private final View.OnClickListener CLEAR_TEXTBOX_CB = new View.OnClickListener() {
            @Override
            public void onClick( final View v ) {
                Log.d( TAG, "Clear textbox button pushed." );
                TextView textView = (TextView)findViewById(R.id.wishfulDebugTextView);
                textView.setText("");
                synchronized (wishfulDebugList) {
                    wishfulDebugList.clear();
                }
            }
        };

        private final View.OnClickListener START_WISHFUL_CB = new View.OnClickListener() {
            @Override
            public void onClick( final View v ) {

                final Button button = ( Button ) rootView.findViewById( R.id.wishfulStartButton );
                if (!isRunningWishful) {
                    Log.v(TAG, "Starting WishFulAgent");

                    button.setText("Stop");
                    button.setEnabled(false);

                    startService(new Intent(that, WishFulService.class));
                } else {
                    Log.v(TAG, "Stopping WishFulAgent");
                    button.setText("Start");
                    button.setEnabled(false);

                    if (wishFulServiceBound) {
                        unbindService(wishfulServiceConnection);
                    }
                    wishFulServiceBound = false;
                    stopService(new Intent(that, WishFulService.class));
                }
            }
        };

        private final View.OnClickListener AUTO_SCROLL_CB = new View.OnClickListener() {
            @Override
            public void onClick( final View v ) {
                Log.v(TAG, "Setting autoscroll to "+!autoScrollEnabled);

                final Button button = ( Button ) rootView.findViewById( R.id.wishfulAutoScrollButton );
                if (autoScrollEnabled) {
                    button.setTextColor(Color.parseColor("grey"));
                } else {
                    button.setTextColor(Color.parseColor("white"));
                }
                autoScrollEnabled = !autoScrollEnabled;

            }
        };

        //========================================================================================//


        //========================================================================================//
        // Private
        //========================================================================================//
        private View setupRootView( final Context context ) {
            final View inflatedView = this.inflateView( context );
            this.setupGuiReferences( inflatedView );
            return inflatedView;
        }

        private View inflateView( final Context context ) {
            final LayoutInflater inflater
                    = ( LayoutInflater )context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            return inflater.inflate( R.layout.wishful, null );
        }

        private void setupGuiReferences( final View rootView ) {
            this.setCallbackForButton( rootView, R.id.wishfulClearTextButton,       CLEAR_TEXTBOX_CB );
            this.setCallbackForButton( rootView, R.id.wishfulStartButton,           START_WISHFUL_CB );
            this.setCallbackForButton( rootView, R.id.wishfulAutoScrollButton,      AUTO_SCROLL_CB );
        }

        private void setCallbackForButton(
                final View rootView,
                final int buttonId,
                final View.OnClickListener callback ) {
            final Button button = ( Button ) rootView.findViewById( buttonId );
            button.setOnClickListener( callback );
        }
        //========================================================================================//

    }
    //============================================================================================//
}
