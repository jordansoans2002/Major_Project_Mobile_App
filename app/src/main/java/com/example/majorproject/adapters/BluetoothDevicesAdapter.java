package com.example.majorproject.adapters;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.majorproject.R;
import com.example.majorproject.fragments.HomeFragment;
import com.example.majorproject.service.BluetoothService;

public class BluetoothDevicesAdapter extends RecyclerView.Adapter<BluetoothDevicesAdapter.Holder> {

    Activity activity;

    public BluetoothDevicesAdapter(Activity activity) {
        this.activity = activity;
    }

    @NonNull
    @Override
    public BluetoothDevicesAdapter.Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bt_device_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull BluetoothDevicesAdapter.Holder holder, int position) {
        try{
            holder.deviceName.setText(HomeFragment.devices.get(position).getName()+position);
            //TODO set helmet icon on devices that are helmets

            holder.item.setOnClickListener(view -> {
                //create connection is blocking call so it creates  new thread
                //indicate the device is trying to connect and highlight if connection succeeds
//                BluetoothService.createBluetoothConnection(HomeFragment.devices.get(holder.getBindingAdapterPosition()),activity);
                Intent connectHelmet = new Intent(activity.getApplicationContext(), BluetoothService.class);
                connectHelmet.putExtra("helmet_MAC",HomeFragment.devices.get(holder.getBindingAdapterPosition()).getAddress());
                activity.startService(connectHelmet);

            });
        } catch (SecurityException e){
            //TODO handle permission denied
        }
    }

    @Override
    public int getItemCount() {
        return (HomeFragment.devices != null)? HomeFragment.devices.size():0;
    }

    class Holder extends RecyclerView.ViewHolder {
        ConstraintLayout item;
        TextView deviceName;
        ImageView icon;

        public Holder(@NonNull View itemView) {
            super(itemView);

            item = itemView.findViewById(R.id.list_item);
            deviceName = itemView.findViewById(R.id.item_text);
            icon = itemView.findViewById(R.id.icon);
        }
    }
}