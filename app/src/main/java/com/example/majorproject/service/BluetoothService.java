package com.example.majorproject.service;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.majorproject.utils.LocationSettingsManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import timber.log.Timber;

public class BluetoothService extends Service {
    Handler handler;
    final int handlerState = 0;
    boolean stopThread;
    private StringBuilder recDataString = new StringBuilder();
    private ConnectingThread connectingThread;
    private ConnectedThread connectedThread;
    private static String UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB";
    static BluetoothAdapter bluetoothAdapter;
//    static BluetoothSocket bluetoothSocket;
//    public static InputStream inputStream;
//    public static OutputStream outputStream;

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
                    if (btSettings.getBoolean("autoReConnect", false));
                        // createBluetoothConnection(device,activity); // call via thread
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

//    public static void createBluetoothConnection(BluetoothDevice device, Activity activity) {
//        helmet = device;
//        new Thread(() -> {
//            try {
//                bluetoothSocket = helmet.createRfcommSocketToServiceRecord(UUID.fromString(UUID_STRING));
//                stopDiscovery();
//                //TODO indicate which device is being connected to in some way
//                bluetoothSocket.connect();
//                //TODO identify the device that is connected
//                inputStream = bluetoothSocket.getInputStream();
//                outputStream = bluetoothSocket.getOutputStream();
//                //TODO take name from user and pass it to here
//                outputStream.write("Helmet says hello".getBytes(StandardCharsets.UTF_8));
//                Timber.i(Arrays.toString(helmet.getUuids()));
//
//                bluetoothAdapter.getProfileProxy(activity.getApplicationContext(),
//                        new BluetoothProfile.ServiceListener() {
//                            @Override
//                            public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
//                                BluetoothA2dp headset = (BluetoothA2dp) bluetoothProfile;
//                                try {
//                                    Method connect = null;
//                                    connect = BluetoothA2dp.class.getDeclaredMethod("connect",BluetoothDevice.class);
//                                    connect.invoke(headset, helmet);
//                                } catch (Exception e){
//                                    e.printStackTrace();
//                                }
//                            }
//
//                            @Override
//                            public void onServiceDisconnected(int i) {
//                                Toast.makeText(
//                                        activity,
//                                        "Helmet disconnected",
//                                        Toast.LENGTH_LONG
//                                ).show();
//                            }
//                        },
//                        BluetoothProfile.A2DP);
//
//                activity.runOnUiThread(() -> {
//                    Toast.makeText(
//                            activity,
//                            "Connected to helmet",
//                            Toast.LENGTH_SHORT
//                    ).show();
//                });
//            } catch (SecurityException se){
//                //TODO request for permission
//            } catch (IOException e){
//                //TODO connection request failed
//                try {
//                    bluetoothSocket.close();
//                } catch (IOException ex) {
//                    //error when closing failed connection
//                } finally {
//                    activity.runOnUiThread(() -> {
//                        Toast.makeText(
//                                activity,
//                                "Could not connect to helmet",
//                                Toast.LENGTH_SHORT
//                        ).show();
//                    });
//                }
//            }
//        }).start();
//    }


    @Override
    public void onCreate() {
        super.onCreate();
        Timber.i("bluetooth service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.i("bluetooth service created");
        handler = new Handler() {
            public void handleMessage(android.os.Message msg){
                Timber.i("handler message");
                if(msg.what == handlerState){
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);
                }
                recDataString.delete(0,recDataString.length());
            }
        };

        String helmet_MAC = intent.getStringExtra("helmet_MAC");
        if(bluetoothAdapter.isEnabled() && helmet_MAC != null || !Objects.equals(helmet_MAC, "")){
            try {
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(helmet_MAC);
                connectingThread = new ConnectingThread(device);
                connectingThread.start();
            } catch (IllegalArgumentException e){
                Timber.e("Illegal mac %s", helmet_MAC);
            }
        } else {
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class ConnectingThread extends Thread{
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectingThread(BluetoothDevice device){
            this.device = device;
            BluetoothSocket tempSocket = null;
            try {
                tempSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(UUID_STRING));
            } catch (IOException | SecurityException e){
                stopSelf();
            }
            socket = tempSocket;
        }

        @Override
        public void run() {
            super.run();
            Timber.i("connecting to device");

            stopDiscovery();

            try {
                socket.connect();
                connectedThread = new ConnectedThread(socket);
                connectedThread.start();
            } catch (IOException | SecurityException e) {
                try {
                    socket.close();
                    stopSelf();
                } catch (IOException e2) {
                    stopSelf();
                }
            } catch (IllegalStateException e) {
                stopSelf();
            }
        }

        public void closeSocket(){
            try {
                socket.close();
            } catch (IOException e) {
                stopSelf();
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tempIn = null;
            OutputStream tempOut = null;
            try{
                tempIn = socket.getInputStream();
                tempOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                stopSelf();
            }
            inputStream = tempIn;
            outputStream = tempOut;
        }

        public void run() {
            Log.d("DEBUG BT", "IN CONNECTED THREAD RUN");
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (!stopThread) {
                try {
                    bytes = inputStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    Log.d("DEBUG BT PART", "CONNECTED THREAD " + readMessage);
                    // Send the obtained bytes to the UI Activity via handler
                    handler.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    Log.d("DEBUG BT", e.toString());
                    Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
                    stopSelf();
                    break;
                }
            }
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                outputStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Log.d("DEBUG BT", "UNABLE TO READ/WRITE " + e.toString());
                Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
                stopSelf();
            }
        }

        public void closeStreams() {
            try {
                //Don't leave Bluetooth sockets open when leaving activity
                inputStream.close();
                outputStream.close();
            } catch (IOException e2) {
                //insert code to deal with this
                Log.d("DEBUG BT", e2.toString());
                Log.d("BT SERVICE", "STREAM CLOSING FAILED, STOPPING SERVICE");
                stopSelf();
            }
        }
    }
}


