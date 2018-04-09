package de.tu_berlin.tkn.ewine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;

public class WifiConnection {

    public static final String TAG = WifiConnection.class.getSimpleName();

    private static final int ConectionStateNONE          = 0;
    private static final int ConectionStatePreConnecting = 1;
    private static final int ConectionStateConnecting    = 2;
    private static final int ConectionStateConnected     = 3;
    private static final int ConectionStateDisconnected  = 4;

    private int  mConectionState = ConectionStateNONE;

    private boolean hadConnection = false;

    private WifiManager wifiManager         = null;
    private WifiConfiguration wifiConfig    = null;
    private Context context                 = null;
    private int netId                       = 0;

    private WiFiConnectionReceiver receiver;
    private IntentFilter filter;

    public static void disableAllNetworks(Context contxt) {
        WifiManager wifiMng = (WifiManager)contxt.getSystemService(contxt.WIFI_SERVICE);
        if (wifiMng == null) {
            return;
        }

        List<WifiConfiguration> wifiList = wifiMng.getConfiguredNetworks();
        for (WifiConfiguration wifiConf : wifiList) {
            Log.v(TAG, "SSID "+wifiConf.SSID+" disabling....");
            wifiMng.disableNetwork(wifiConf.networkId);
        }
    }

    public WifiConnection(Context Context, String SSIS, String password) {
        this.context = Context;

        receiver = new WiFiConnectionReceiver();
        filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        this.context.registerReceiver(receiver, filter);

        this.wifiManager = (WifiManager)this.context.getSystemService(this.context.WIFI_SERVICE);

        this.wifiConfig = new WifiConfiguration();
        this.wifiConfig.SSID = String.format("\"%s\"", SSIS);
        this.wifiConfig.preSharedKey = String.format("\"%s\"", password);

        this.netId = this.wifiManager.addNetwork(this.wifiConfig);

        this.wifiManager.enableNetwork(this.netId, true);
        this.wifiManager.reconnect();
    }

    public void Stop() {
        this.context.unregisterReceiver(receiver);
        this.wifiManager.disableNetwork(this.netId);
        this.wifiManager.disconnect();
        Log.v(TAG, "state disconnected");
    }

    private class WiFiConnectionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                if (info != null) {

                    if (info.isConnected()) {
                        hadConnection = true;
                        mConectionState = ConectionStateConnected;
                        Log.v(TAG, "state isConnected");

                    } else if (info.isConnectedOrConnecting()) {
                        mConectionState = ConectionStateConnecting;
                        Log.v(TAG, "state isConnectedOrConnecting");

                    } else {
                        if (hadConnection) {
                            mConectionState = ConectionStateDisconnected;
                            Log.v(TAG, "state disconnected");

                        } else {
                            mConectionState = ConectionStatePreConnecting;
                            Log.v(TAG, "state preConnecting");

                        }
                    }
                }
            }
        }
    }
}
