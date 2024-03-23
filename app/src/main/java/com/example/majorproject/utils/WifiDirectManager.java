package com.example.majorproject.utils;

import android.app.Activity;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;

import java.util.HashMap;
import java.util.Map;

public class WifiDirectManager {

    static WifiP2pManager.Channel channel;
    static WifiP2pManager manager;
    private static final int SERVER_PORT = 5555;
    private static final IntentFilter intentFilter = new IntentFilter();

    public static void initWifiDirect(Activity activity) {

        // Indicates a change in the Wi-Fi Direct status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        // Indicates the state of Wi-Fi Direct connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) activity.getSystemService(activity.WIFI_P2P_SERVICE);
        channel = manager.initialize(activity, activity.getMainLooper(), null);
    }

    private void startRegistration() {
        //  Create a string map containing information about your service.
        Map<String, String> record = new HashMap();
        record.put("listenport", String.valueOf(SERVER_PORT));
        record.put("buddyname", "John Doe" + (int) (Math.random() * 1000));
        record.put("available", "visible");

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance("_test", "_presence._tcp", record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        try {
            manager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    // Command successful! Code isn't necessarily needed here,
                    // Unless you want to update the UI or add logging statements.
                }

                @Override
                public void onFailure(int arg0) {
                    // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                }
            });
        } catch (SecurityException se){
            //if location is not enabled
        }
    }
}
