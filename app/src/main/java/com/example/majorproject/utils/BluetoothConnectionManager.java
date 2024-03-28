package com.example.majorproject.utils;

import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import timber.log.Timber;

public class BluetoothConnectionManager {

    private static String UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB";
    static BluetoothDevice helmet;
    static BluetoothAdapter bluetoothAdapter;
    static BluetoothSocket bluetoothSocket;
    public static InputStream inputStream;
    public static OutputStream outputStream;

    static SharedPreferences btSettings;

    public static boolean isBluetoothOn(){
        return bluetoothAdapter.isEnabled();
    }

    public static boolean setupBluetooth(Activity activity){
        btSettings = androidx.preference.PreferenceManager.getDefaultSharedPreferences(activity);

        BluetoothManager bluetoothManager = activity.getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();

        return btSettings.getBoolean("autoBluetooth", true);
    }

    @Nullable
    public static List<BluetoothDevice> getPairedDevices(Activity activity){
        String lastHelmetMAC = btSettings.getString("lastHelmet", null);
        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            Timber.d(pairedDevices.toString());
            List<BluetoothDevice> devices = new ArrayList<>(pairedDevices.size());
            for (BluetoothDevice device : pairedDevices) {
                //TODO show only our helmets
                if (device.getAddress().equals(lastHelmetMAC)) {
                    devices.add(0, device);
                    if (btSettings.getBoolean("autoReConnect", false))
                        createBluetoothConnection(device,activity);
                } else
                    devices.add(device);
            }
            return devices;
        }catch (SecurityException se){
            //Give some error in prompt textview
            return null;
        }
    }

    public static boolean startDiscovery(Activity activity) throws SecurityException{
        if(bluetoothAdapter.isDiscovering())
            bluetoothAdapter.cancelDiscovery();
        LocationSettingsManager.isLocationEnabled(activity);
        return bluetoothAdapter.startDiscovery();
    }
    public static void stopDiscovery() throws SecurityException{
        bluetoothAdapter.cancelDiscovery();
    }

    public static void createBluetoothConnection(BluetoothDevice device, Activity activity) {
        helmet = device;
        new Thread(() -> {
            try {
                bluetoothSocket = helmet.createRfcommSocketToServiceRecord(UUID.fromString(UUID_STRING));
                stopDiscovery();
                //TODO indicate which device is being connected to in some way
                bluetoothSocket.connect();
                //TODO identify the device that is connected
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
                //TODO take name from user and pass it to here
                outputStream.write("Helmet says hello".getBytes(StandardCharsets.UTF_8));
                Timber.i(Arrays.toString(helmet.getUuids()));

                bluetoothAdapter.getProfileProxy(activity.getApplicationContext(),
                        new BluetoothProfile.ServiceListener() {
                            @Override
                            public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
                                BluetoothA2dp headset = (BluetoothA2dp) bluetoothProfile;
                                try {
                                    Method connect = null;
                                    connect = BluetoothA2dp.class.getDeclaredMethod("connect",BluetoothDevice.class);
                                    connect.invoke(headset, helmet);
                                } catch (Exception e){
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onServiceDisconnected(int i) {
                                Toast.makeText(
                                        activity,
                                        "Helmet disconnected",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        },
                        BluetoothProfile.A2DP);

                activity.runOnUiThread(() -> {
                    Toast.makeText(
                            activity,
                            "Connected to helmet",
                            Toast.LENGTH_SHORT
                    ).show();
                });
            } catch (SecurityException se){
                //TODO request for permission
            } catch (IOException e){
                //TODO connection request failed
                try {
                    bluetoothSocket.close();
                } catch (IOException ex) {
                    //error when closing failed connection
                } finally {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(
                                activity,
                                "Could not connect to helmet",
                                Toast.LENGTH_SHORT
                        ).show();
                    });
                }
            }
        }).start();
    }

    public static boolean sendData(String data){
        try {
            outputStream.write(data.getBytes(StandardCharsets.UTF_8));
            Timber.i("data sent to helmet");
            return true;
        } catch (IOException e){
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
}
