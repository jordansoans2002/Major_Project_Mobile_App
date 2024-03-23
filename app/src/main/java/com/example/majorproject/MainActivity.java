package com.example.majorproject;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.majorproject.fragments.EmergencyDetailsFragment;
import com.example.majorproject.fragments.HomeFragment;
import com.example.majorproject.fragments.NavigationFragment;
import com.example.majorproject.fragments.RecordingsFragment;
import com.example.majorproject.fragments.SettingsFragment;
import com.example.majorproject.utils.LocationSettingsManager;
import com.example.majorproject.utils.ObjectWrapperForBinder;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mappls.sdk.maps.Mappls;
import com.mappls.sdk.services.account.MapplsAccountManager;
import com.mappls.sdk.services.api.directions.models.DirectionsRoute;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
//            Timber.plant(new FileLoggingTree());
        }

        initMapplsAPI();
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        if(intent.getBooleanExtra("journeyStarted",false)) {
            final DirectionsRoute route = (DirectionsRoute) ((ObjectWrapperForBinder) intent.getExtras().getBinder("route")).getData();
            String dest = intent.getStringExtra("destination");
            replaceFragment(new NavigationFragment(route,dest,intent.getStringExtra("stop")));
        }else
            replaceFragment(new HomeFragment());

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setOnItemSelectedListener(item -> {
            switch (item.getItemId()){
                case R.id.home:
                    replaceFragment(new HomeFragment());
                    break;
                case R.id.recording:
                    replaceFragment(new RecordingsFragment());
                    break;
                case R.id.navigation:
                    //check for permission if needed
                    LocationSettingsManager.isLocationEnabled(this);
                    replaceFragment(new NavigationFragment());
                    break;
                case R.id.emergency:
                    replaceFragment(new EmergencyDetailsFragment());
                    break;
                case R.id.settings:
                    replaceFragment(new SettingsFragment());
                    break;
            }
            return true;
        });
    }

    void initMapplsAPI() {
        MapplsAccountManager.getInstance().setRestAPIKey("d066b76c53719c5da933efda3d206f2e");
        MapplsAccountManager.getInstance().setMapSDKKey("d066b76c53719c5da933efda3d206f2e");
        MapplsAccountManager.getInstance().setAtlasClientId("33OkryzDZsJj4eyhr45GxphMIT2kP74pVRNOEXPtG01l1XzNOu_r5HTziE2fYyd2HxmAa7XIo3dhuCOKpabYxA==");
        MapplsAccountManager.getInstance().setAtlasClientSecret("lrFxI-iSEg_APneCWpZsAntFvuMCu3fM26TZ3Wzql4HQiPF7mVIiRkMurOIhjwDLKw68Cg4aQ9V3kbA-GM_Z95fZWge9S_-N");
        Mappls.getInstance(getApplicationContext());
    }

    void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout,fragment);
        fragmentTransaction.commit();
    }
}
