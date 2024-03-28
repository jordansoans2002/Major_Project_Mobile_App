package com.example.majorproject.service;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.majorproject.MainActivity;
import com.example.majorproject.R;
import com.example.majorproject.fragments.NavigationFragment;
import com.example.majorproject.utils.CrashResponse;
import com.example.majorproject.utils.ObjectWrapperForBinder;
import com.mappls.sdk.geojson.Point;
import com.mappls.sdk.maps.location.engine.LocationEngine;
import com.mappls.sdk.maps.location.engine.LocationEngineCallback;
import com.mappls.sdk.maps.location.engine.LocationEngineRequest;
import com.mappls.sdk.maps.location.engine.LocationEngineResult;
import com.mappls.sdk.plugin.directions.DirectionsUtils;
import com.mappls.sdk.services.api.directions.models.DirectionsRoute;
import com.mappls.sdk.services.api.directions.models.LegStep;
import com.mappls.sdk.services.api.directions.models.RouteLeg;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class NavigationService extends Service {

    NotificationManager notificationManager;
    NotificationCompat.Builder journey;

    Location currentLocation;
    DirectionsRoute route;
    LegStep currentStep;
    int currentStepNo = 0;
    List<LegStep> steps;

    InputStream inputStream;
    OutputStream outputStream;

    LocationEngine locationEngine;
    static final long DEFAULT_INTERVAL_IN_ms = 1000L;
    static final long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_ms * 3;

    BroadcastReceiver btMessages = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() == null)
                return;
            else if(intent.getAction().equals("BT_DATA_IN")){
                String msg = intent.getStringExtra("msg");
                int bytes = intent.getIntExtra("msg_size",-1);
                if(msg==null)
                    return;
                Timber.i("BT message %s size, %i",msg,bytes);

                if(msg.startsWith("CRASH")){
                    CrashResponse.sendSms(currentLocation);
                }
            }
        }
    };

    LocationEngineCallback<LocationEngineResult> locationEngineCallback = new LocationEngineCallback<LocationEngineResult>() {
        @Override
        public void onSuccess(LocationEngineResult locationEngineResult) {
            if (locationEngineResult.getLastLocation() != null) {
                double moved = 0;
                if(currentLocation != null)
                    moved = currentLocation.distanceTo(locationEngineResult.getLastLocation());
                currentLocation = locationEngineResult.getLastLocation();

                Point stepManeuver = currentStep.maneuver().location();
                Location turnHere = new Location("");
                turnHere.setLatitude(stepManeuver.latitude());
                turnHere.setLongitude(stepManeuver.longitude());

                double dist = currentLocation.distanceTo(turnHere);
                Timber.i("distance to first maneuver is %f \n dist moved is %f", dist,moved);
                Timber.d(NavigationFragment.currentLocation.toString());


                if(dist < 10 && currentStepNo<steps.size()-1)
                    currentStep = steps.get(++currentStepNo);

                String msg = currentStep.maneuver().modifier()+" angle: b"
                        + currentStep.maneuver().bearingBefore()+" a"+currentStep.maneuver().bearingAfter();

                Timber.i("Sending data to bt %s",msg);

                Intent data_out = new Intent(NavigationService.this, BluetoothService.class);
                data_out.setAction("SEND_DATA");
                data_out.putExtra("BT_DATA",msg);
                startService(data_out);


                journey.setContentTitle("Turn upcoming in "+(int)dist+" m");
                journey.setContentText(DirectionsUtils.getTextInstructions((currentStep)));
//                journey.setProgress(steps.size(),currentStepNo,false);
                notificationManager.notify(517,journey.build());
            }
        }

        @Override
        public void onFailure(@NonNull Exception e) {
            Timber.e("location fetch failed");
            e.printStackTrace();
        }
    };

    public NavigationService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(btMessages,new IntentFilter("BT_DATA_IN"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(btMessages);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getAction().equals("START_SERVICE")) {
            final Object objReceived = ((ObjectWrapperForBinder) intent.getExtras().getBinder("route")).getData();
            Timber.i("received object=" + objReceived);
            route = (DirectionsRoute) objReceived;
            locationEngine = (LocationEngine) ((ObjectWrapperForBinder) intent.getExtras().getBinder("locationEngine")).getData();
            inputStream = (InputStream) ((ObjectWrapperForBinder) intent.getExtras().getBinder("inputStream")).getData();
            outputStream = (OutputStream) ((ObjectWrapperForBinder) intent.getExtras().getBinder("outputStream")).getData();

            getRouteDetails();
            createNotification(intent);
            startForeground(517, journey.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);

            //continuously fetches the current location
            LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_ms)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                    .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
                    .build();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationEngine.requestLocationUpdates(request, locationEngineCallback, getMainLooper());
                locationEngine.getLastLocation(locationEngineCallback);
            }
        } else if(intent.getAction().equals("STOP_SERVICE")) {
            stopForeground(true);
            stopSelfResult(517);
            return START_NOT_STICKY;
        }

        return START_REDELIVER_INTENT;
    }

    void getRouteDetails(){
        List<RouteLeg> routeLegList = route.legs();
        steps = new ArrayList<>();
        for (RouteLeg routeLeg : routeLegList)
            steps.addAll(routeLeg.steps());
        currentStep = steps.get(0);
    }
    void createNotification(Intent intent){
        NotificationChannel notificationChannel = new NotificationChannel("directions", "journey", NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel);

        final Bundle bundle = new Bundle();
        bundle.putBinder("route", new ObjectWrapperForBinder(route));
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.putExtra("journeyStarted", true);
        notificationIntent.putExtra("destination", intent.getStringExtra("destination"));
        notificationIntent.putExtra("destinationPin",intent.getStringExtra("destinationPin"));
        notificationIntent.putExtra("stop", intent.getStringExtra("stop"));
        notificationIntent.putExtras(bundle);

        PendingIntent openAppPending = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent exitNavigation = new Intent(this, NavigationService.class);
        exitNavigation.setAction("STOP_SERVICE");
        PendingIntent exitPending = PendingIntent.getService(this,1,exitNavigation,0);

        journey = new NotificationCompat.Builder(this, "directions")
                .setSmallIcon(R.drawable.start) //put app icon or direction
                .setContentTitle("Journey with smart helmet")
                .setContentText("Turn right")
                .setOngoing(true)
//                    .setProgress(steps.size(),0,false)
                .setContentIntent(openAppPending) //what to do when notification is pressed
                .addAction(R.drawable.baseline_cancel_presentation_24,"End navigation",exitPending);
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}