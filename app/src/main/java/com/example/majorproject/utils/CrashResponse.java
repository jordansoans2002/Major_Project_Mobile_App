package com.example.majorproject.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.telephony.SmsManager;

import java.util.Arrays;

import timber.log.Timber;

public class CrashResponse {
    static String bloodGrp, gndr, medicalConditionsString;
    static long[] contacts = new long[3];

    public static void initDetails(Activity activity){
        SharedPreferences emergencyDetails = activity.getSharedPreferences("emergency_details", Context.MODE_PRIVATE);
        bloodGrp = emergencyDetails.getString("blood_group","");
        gndr = emergencyDetails.getString("gender","");
        medicalConditionsString = emergencyDetails.getString("medical_conditions","");
        contacts[0] = emergencyDetails.getLong("contact1",0);
        contacts[1] = emergencyDetails.getLong("contact2",0);
        contacts[2] = emergencyDetails.getLong("contact3",0);

        Timber.d("Emergency information of the person:" +
                "Blood group "+bloodGrp +
                "Gender " + gndr +
                "Health conditions " + medicalConditionsString +
                "contacts "+ Arrays.toString(contacts));
    }

    public static void sendSms(Location currentLocation){
        Timber.i("crash response started");

        String location ="Smart helmet detected an incident at Location http://maps.google.com?q=" +
                currentLocation.getLatitude()+','+currentLocation.getLongitude();
        String information = "Emergency information of the person:" +
                "Blood group "+bloodGrp +
                "Gender " + gndr +
                "Health conditions " + medicalConditionsString;

        SmsManager smsManager = SmsManager.getDefault();
        for(long contact : contacts) {
            if(contact>999999999) {
                smsManager.sendTextMessage("+91" + contact, null, location,
                        null, null);
                smsManager.sendTextMessage("+91" + contact, null, information,
                        null, null);
                Timber.d("msg sent to %s", contact);
            }
        }
        Timber.i("crash response finished");
    }
}
