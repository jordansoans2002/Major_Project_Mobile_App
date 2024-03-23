package com.example.majorproject.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.majorproject.R;
import com.google.android.material.textfield.TextInputEditText;

public class EmergencyDetailsFragment extends Fragment {

    AutoCompleteTextView bloodGroup, gender;
    TextInputEditText medicalConditions;
    TextInputEditText[] contactInput = new TextInputEditText[3];
    Button saveDetails;
    long[] contacts = new long[3];

    public EmergencyDetailsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_emergency_response, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bloodGroup = view.findViewById(R.id.blood_group);
        gender = view.findViewById(R.id.gender);
        medicalConditions = view.findViewById(R.id.medical_conditions);
        contactInput[0] = view.findViewById(R.id.contact1);
        contactInput[1] = view.findViewById(R.id.contact2);
        contactInput[2] = view.findViewById(R.id.contact3);
        saveDetails = view.findViewById(R.id.save_details);

        SharedPreferences emergencyDetails = getContext().getSharedPreferences("emergency_details", Context.MODE_PRIVATE);
        String bloodGrp = emergencyDetails.getString("blood_group","");
        String gndr = emergencyDetails.getString("gender","");
        String medicalConditionsString = emergencyDetails.getString("medical_conditions","");
        contacts[0] = emergencyDetails.getLong("contact1",0);
        contacts[1] = emergencyDetails.getLong("contact2",0);
        contacts[2] = emergencyDetails.getLong("contact3",0);


        String[] grps = {"","A-","A+","B-","B+","AB-","AB+","O-","O+"};
        ArrayAdapter<String> bloodGrpAdapter = new ArrayAdapter<>(this.getContext(),R.layout.dropdown_list_textview,grps);
        bloodGroup.setAdapter(bloodGrpAdapter);
        bloodGroup.setText(bloodGrp,false);

        String[] genders = {"","Male","Female"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this.getContext(),R.layout.dropdown_list_textview,genders);
        gender.setAdapter(genderAdapter);
        gender.setText(gndr,false);

        medicalConditions.setText(medicalConditionsString);
        contactInput[0].setText((contacts[0]==0)? "":contacts[0]+"");
        contactInput[1].setText((contacts[1]==0)? "":contacts[1]+"");
        contactInput[2].setText((contacts[2]==0)? "":contacts[2]+"");

        saveDetails.setOnClickListener((v) -> {
            SharedPreferences.Editor editDetails = emergencyDetails.edit();
            editDetails.putString("blood_group",bloodGroup.getText().toString());
            editDetails.putString("gender",gender.getText().toString());
            editDetails.putString("medical_conditions",medicalConditions.getText().toString());
            for(int i=0;i<contacts.length;i++){
                try {
                    contacts[i] = Long.parseLong(contactInput[i].getText().toString());
                } catch (NumberFormatException e){
                    contacts[i] = 0;
                }
                editDetails.putLong("contact"+(i+1),contacts[i]);
            }
            editDetails.apply();

            //TODO move this to NavigationService
            //include user details in the sms
            //TODO video send in future scope
            SmsManager smsManager = SmsManager.getDefault();
            for(long contact : contacts) {
                if(contact>999999999)
                    smsManager.sendTextMessage("+91"+contact, null, "Smart helmet has detected a possible incident at http://maps.google.com/maps?saddr"
                            +NavigationFragment.currentLocation.getLatitude()+','+NavigationFragment.currentLocation.getLongitude(),
                            null, null);
            }
        });
    }
}