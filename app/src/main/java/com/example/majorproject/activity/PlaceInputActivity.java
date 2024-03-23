package com.example.majorproject.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.majorproject.R;
import com.example.majorproject.adapters.PlaceSuggestAdapter;
import com.example.majorproject.utils.CheckInternet;
import com.mappls.sdk.services.api.OnResponseCallback;
import com.mappls.sdk.services.api.autosuggest.MapplsAutoSuggest;
import com.mappls.sdk.services.api.autosuggest.MapplsAutosuggestManager;
import com.mappls.sdk.services.api.autosuggest.model.AutoSuggestAtlasResponse;
import com.mappls.sdk.services.api.autosuggest.model.ELocation;
import com.mappls.sdk.services.api.textsearch.MapplsTextSearch;
import com.mappls.sdk.services.api.textsearch.MapplsTextSearchManager;

import java.util.ArrayList;

public class PlaceInputActivity extends AppCompatActivity implements TextWatcher, TextView.OnEditorActionListener {

    private EditText placeSearch;
    private CardView currentLocation;
    private RecyclerView placeSuggest;
    boolean isOrigin;
    double latitude,longitude;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_input);

        isOrigin = getIntent().getBooleanExtra("isOrigin",false);
        latitude = getIntent().getDoubleExtra("latitude",19.044525);
        longitude = getIntent().getDoubleExtra("longitude",72.820674);

        initViews();
        initListeners();
    }

    void initViews(){
        placeSearch = findViewById(R.id.place_search);
        currentLocation = findViewById(R.id.current_location);
        currentLocation.setVisibility(isOrigin? View.VISIBLE : View.GONE);
        placeSuggest = findViewById(R.id.places_suggestions);
        placeSuggest.setLayoutManager(new LinearLayoutManager(this));
        handler = new Handler(Looper.getMainLooper());

        placeSearch.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @SuppressLint("ClickableViewAccessibility")
    void initListeners(){
        placeSearch.addTextChangedListener(this);
        placeSearch.setOnEditorActionListener(this);
        placeSearch.setOnTouchListener(((view, motionEvent) -> {
            TextView textView = (TextView) view;
            if(motionEvent.getAction() == MotionEvent.ACTION_UP){
                if(motionEvent.getRawX() >= textView.getRight() - textView.getCompoundDrawables()[0].getBounds().width()){
                    setResult(Activity.RESULT_CANCELED);
                    finish();
                    return true;
                }
            }
            return false;
        }));

        currentLocation.setOnClickListener(view -> {
            Intent returnPlace = new Intent();
            returnPlace.putExtra("placeName","Current location");
            returnPlace.putExtra("mapplsPin","");
            setResult(Activity.RESULT_OK,returnPlace);
            finish();
        });
    }

    private void callAutoSuggestApi(String searchString) {
        MapplsAutoSuggest mapplsAutoSuggest = MapplsAutoSuggest.builder()
                .setLocation(latitude,longitude)
                .hyperLocal(true)
                .query(searchString)
                .build();
        MapplsAutosuggestManager.newInstance(mapplsAutoSuggest).call(new OnResponseCallback<AutoSuggestAtlasResponse>() {
            @Override
            public void onSuccess(AutoSuggestAtlasResponse autoSuggestAtlasResponse) {
                if (autoSuggestAtlasResponse != null) {
                    ArrayList<ELocation> suggestedList = autoSuggestAtlasResponse.getSuggestedLocations();
                    if (suggestedList.size() > 0) {
                        placeSuggest.setVisibility(View.VISIBLE);
                        PlaceSuggestAdapter placeSuggestAdapter = new PlaceSuggestAdapter(PlaceInputActivity.this,suggestedList);
                        placeSuggest.setAdapter(placeSuggestAdapter);
                    }
                } else {
                    Toast.makeText(
                        PlaceInputActivity.this,
                        "Not able to get value, Try again."
                        ,Toast.LENGTH_SHORT
                    ).show();
                }
            }


            @Override
            public void onError(int i, String s) {
                Toast.makeText(
                        PlaceInputActivity.this,
                        "Not able to get value, Try again."
                        ,Toast.LENGTH_SHORT
                ).show();
            }
        });

    }

    private void callTextSearchApi(String searchString) {
        MapplsTextSearch mapplsTextSearch = MapplsTextSearch.builder()
                .query(searchString)
                .build();
        MapplsTextSearchManager.newInstance(mapplsTextSearch).call(new OnResponseCallback<AutoSuggestAtlasResponse>() {
            @Override
            public void onSuccess(AutoSuggestAtlasResponse autoSuggestAtlasResponse) {
                if (autoSuggestAtlasResponse != null) {
                    ArrayList<ELocation> suggestedList = autoSuggestAtlasResponse.getSuggestedLocations();
                    if (suggestedList.size() > 0) {
                        placeSuggest.setVisibility(View.VISIBLE);
                        PlaceSuggestAdapter placeSuggestAdapter = new PlaceSuggestAdapter(PlaceInputActivity.this,suggestedList);
                        placeSuggest.setAdapter(placeSuggestAdapter);
                    }
                } else {
                    Toast.makeText(
                            PlaceInputActivity.this,
                            "Not able to get value, Try again."
                            ,Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onError(int i, String s) {
                Toast.makeText(
                        PlaceInputActivity.this,
                        "Not able to get value, Try again."
                        ,Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence s, int i, int i1, int i2) {
        handler.postDelayed(() -> {
            placeSuggest.setVisibility(View.GONE);

            if(s != null && s.toString().trim().length() < 2){
                placeSuggest.setAdapter(null);
                return;
            }

            if(s.length()>2){
                if(CheckInternet.isNetworkAvailable(PlaceInputActivity.this))
                    callAutoSuggestApi(s.toString());
                else
                    Toast.makeText(
                            PlaceInputActivity.this,
                            "Please check internet connection."
                            ,Toast.LENGTH_SHORT
                    ).show();
            }
        },700);
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if(i == EditorInfo.IME_ACTION_SEARCH){
            callTextSearchApi(textView.getText().toString());
            placeSearch.clearFocus();
            InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            in.hideSoftInputFromWindow(placeSearch.getWindowToken(),0);
            return true;
        }
        return false;
    }
}