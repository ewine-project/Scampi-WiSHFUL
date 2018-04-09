package de.tu_berlin.tkn.ewine;


public class WifiHelper {
    static public String WifiP2PManagerErrorToString(int id) {
        switch(id) {
            case android.net.wifi.p2p.WifiP2pManager.P2P_UNSUPPORTED:
                return "P2P_UNSUPPORTED";
            case android.net.wifi.p2p.WifiP2pManager.ERROR:
                return "ERROR";
            case android.net.wifi.p2p.WifiP2pManager.BUSY:
                return "BUSY";
        }
        return "Unknown error id "+id;
    }
}
