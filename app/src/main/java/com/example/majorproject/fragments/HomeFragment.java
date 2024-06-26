package com.example.majorproject.fragments;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.majorproject.R;
import com.example.majorproject.adapters.BluetoothDevicesAdapter;
import com.example.majorproject.service.BluetoothService;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class HomeFragment extends Fragment {

    BluetoothDevicesAdapter adapter;
    RecyclerView btDevices;
    TextView enableBTPrompt;
    ImageView reScan;
    ProgressBar scanning;

    public static List<BluetoothDevice> devices = new ArrayList<>();

    BroadcastReceiver btUpdates = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null)
                return;
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        case BluetoothAdapter.STATE_ON:
                            enableBTPrompt.setVisibility(View.GONE);
                            btDevices.setVisibility(View.VISIBLE);
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            BluetoothService.startDiscovery(HomeFragment.this.getActivity());
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            enableBTPrompt.setText(R.string.enable_bluetooth_prompt);
                            enableBTPrompt.setVisibility(View.VISIBLE);
                            btDevices.setVisibility(View.GONE);
                            reScan.setVisibility(View.VISIBLE);
                            scanning.setVisibility(View.GONE);
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Timber.d("discovery started broadcast reciever");
                    reScan.setVisibility(View.GONE);
                    scanning.setVisibility(View.VISIBLE);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Timber.d("discovery stopped broadcast reciever");
                    reScan.setVisibility(View.VISIBLE);
                    scanning.setVisibility(View.GONE);
                    if(devices.size() == 0)
                        enableBTPrompt.setText(R.string.no_helmet_nearby);
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //TODO check if device is a helmet
                    try {
                        if (!devices.contains(device) && device.getName() != null) {
                            devices.add(device);
                            adapter.notifyItemInserted(devices.size() - 1);
                            btDevices.scrollToPosition(devices.size() - 1);
                            Timber.d(String.valueOf(devices.size()));
                        }
                    }catch (SecurityException se){
                        Timber.e("Bluetooth permissions missing");
                    }
                    break;
            }
        }
    };

    BroadcastReceiver btMessages = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() == null)
                return;
            else if(intent.getAction().equals("BT_DATA_IN")){
                String msg = intent.getStringExtra("msg");
                int bytes = intent.getIntExtra("msg_size",-1);
                Timber.i("BT message %s size, %d",msg,bytes);
                //update helmet info like battery left etc
            }
        }
    };

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btDevices = view.findViewById(R.id.bt_devices);
        enableBTPrompt = view.findViewById(R.id.bt_enable_prompt);
        reScan = view.findViewById(R.id.re_scan);
        scanning = view.findViewById(R.id.scan_in_progress);

        adapter = new BluetoothDevicesAdapter(this.getActivity());
        btDevices.setAdapter(adapter);
        btDevices.setLayoutManager(new LinearLayoutManager(this.getContext()));

        reScan.setOnClickListener((v) -> {
            //this searches for available devices
//            boolean f = BluetoothConnectionManager.startDiscovery(this.getActivity());
            boolean f = BluetoothService.startDiscovery(this.getActivity());
            if (!f)
                Toast.makeText(this.getContext(),
                        "Couldn't start scanning",
                        Toast.LENGTH_LONG
                ).show();
            //this makes phone visible to devices looking to pair
//            Intent discoverIntent = new Intent(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
//            discoverIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30);
//            startActivity(discoverIntent);
        });

        scanning.setOnClickListener((v) -> {
            reScan.setVisibility(View.VISIBLE);
            scanning.setVisibility(View.GONE);
//            BluetoothConnectionManager.stopDiscovery();
            BluetoothService.stopDiscovery();
        });

    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter updatesIntentFilter = new IntentFilter();
        updatesIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        updatesIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        updatesIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        updatesIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);

        IntentFilter messagesIntentFilter = new IntentFilter("BT_DATA_IN");

        this.getActivity().registerReceiver(btUpdates, updatesIntentFilter);
        this.getActivity().registerReceiver(btMessages, messagesIntentFilter);

//        if(BluetoothConnectionManager.isBluetoothOn()){
        if(BluetoothService.isBluetoothOn()){
            btDevices.setVisibility(View.VISIBLE);
            enableBTPrompt.setVisibility(View.GONE);
        } else {
            btDevices.setVisibility(View.GONE);
            enableBTPrompt.setText(R.string.enable_bluetooth_prompt);
            enableBTPrompt.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        this.getActivity().unregisterReceiver(btUpdates);
        this.getActivity().unregisterReceiver(btMessages);
//        BluetoothConnectionManager.stopDiscovery();
        BluetoothService.stopDiscovery();
    }
}