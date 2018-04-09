package de.tu_berlin.tkn.ewine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.Collection;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION;

public class WifiAccessPoint implements WifiP2pManager.ConnectionInfoListener,WifiP2pManager.ChannelListener,WifiP2pManager.GroupInfoListener{

    private static final String TAG = WifiAccessPoint.class.getSimpleName();

    private WifiAccessPoint that = this;
    private Context context;

    private WifiP2pManager p2p;
    private WifiP2pManager.Channel channel;

    private String mNetworkName     = "";
    private String mPassphrase      = "";
    private String mInetAddress     = "";

    private BroadcastReceiver receiver;
    private IntentFilter filter;

    private boolean createGroupOnSuccessfulRemove = false;

    public WifiAccessPoint(Context Context) {
        this.context = Context;
    }

    public void Start() {

        p2p = (WifiP2pManager) this.context.getSystemService(this.context.WIFI_P2P_SERVICE);

        if (p2p == null) {
            Log.e(TAG, "This device does not support Wi-Fi Direct");
        } else {

            channel = p2p.initialize(this.context, this.context.getMainLooper(), this);
            Log.v(TAG, "Initializing AP");

            receiver    = new AccessPointReceiver();
            filter      = new IntentFilter();
            filter.addAction(WIFI_P2P_STATE_CHANGED_ACTION);
            filter.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION);
            this.context.registerReceiver(receiver, filter);

            this.createGroup();
        }
    }

    private void createGroup() {
        p2p.createGroup(channel,new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                Log.v(TAG, "createGroup() success");
            }

            public void onFailure(int reason) {
                Log.e(TAG, "createGroup() failed, error code " + WifiHelper.WifiP2PManagerErrorToString(reason));

                //group is still running, we remove it and then start the group
                if (reason == android.net.wifi.p2p.WifiP2pManager.BUSY) {
                    createGroupOnSuccessfulRemove = true;
                    removeGroup();
                }
            }
        });
    }

    public void Stop() {
        this.context.unregisterReceiver(receiver);
        receiver = null;
        removeGroup();
    }

    public void removeGroup() {
        p2p.removeGroup(channel,new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                Log.v(TAG, "removeGroup() success");
                if (createGroupOnSuccessfulRemove) {
                    createGroupOnSuccessfulRemove = false;
                    createGroup();
                }
            }

            public void onFailure(int reason) {
                Log.e(TAG, "removeGroup() failed, error code " + WifiHelper.WifiP2PManagerErrorToString(reason));
            }
        });
    }

    @Override
    public void onChannelDisconnected() {
        Log.e(TAG, "onChannelDisconnected()");
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        if (group == null) {
            Log.e(TAG, "onGroupInfoAvailable, WifiP2pGroup is null?");
            return;
        }
        try {
            Collection<WifiP2pDevice> devlist = group.getClientList();

            int num = 0;
            for (WifiP2pDevice peer : group.getClientList()) {
                num++;
                Log.v(TAG, "Client " + num + " : "  + peer.deviceName + " " + peer.deviceAddress);
            }

            mNetworkName    = group.getNetworkName();
            mPassphrase     = group.getPassphrase();
            Log.v(TAG, "onGroupInfoAvailable SSID {" + mNetworkName + "} Passphrase {"  +mPassphrase+"}");

        } catch(Exception e) {
            Log.e(TAG, "onGroupInfoAvailable, error: " + e.toString());
        }
    }

    //will be called by requestConnectionInfo
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        try {
            if (info.isGroupOwner) {
                mInetAddress = info.groupOwnerAddress.getHostAddress();
                Log.v(TAG, "onConnectionInfoAvailable GroupOwner with address "+mInetAddress);

                p2p.requestGroupInfo(channel,this);
            } else {
                Log.v(TAG, "onConnectionInfoAvailable Client, address of group owner address " + info.groupOwnerAddress.getHostAddress());
            }
        } catch(Exception e) {
            Log.e(TAG, "onConnectionInfoAvailable, error: " + e.toString());
        }
    }

    private class AccessPointReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //check if WiFi p2p is enabled or disabled
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            }  else if (WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                Log.v(TAG, "AP ACTION STATE CH - networkInfo " + networkInfo.toString());

                if (networkInfo.isConnected()) {
                    Log.v(TAG, "We are connected, will check info now");
                    p2p.requestConnectionInfo(channel, that);
                    // callback -> onConnectionInfoAvailable
                } else{
                    Log.v(TAG, "We are DIS-connected");
                }
            }
        }
    }
}
