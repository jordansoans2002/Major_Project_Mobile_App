package com.example.majorproject.utils;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

import androidx.core.location.LocationManagerCompat;

public class LocationSettingsManager {

    public static void isLocationEnabled(Context context){
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if(!LocationManagerCompat.isLocationEnabled(locationManager))
            enableLocation(context);
    }

    public static void enableLocation(Context context){
        context.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }
}
