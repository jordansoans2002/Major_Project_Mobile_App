package com.example.majorproject.adapters;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.majorproject.R;
import com.example.majorproject.activity.PlaceInputActivity;
import com.mappls.sdk.services.api.autosuggest.model.ELocation;

import java.util.ArrayList;

public class PlaceSuggestAdapter extends RecyclerView.Adapter<PlaceSuggestAdapter.Holder> {

    PlaceInputActivity activity;
    private ArrayList<ELocation> list;


    public PlaceSuggestAdapter(PlaceInputActivity activity,ArrayList<ELocation> list){
        this.activity = activity;
        this.list = list;
    }
    @NonNull
    @Override
    public PlaceSuggestAdapter.Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.place_suggest_row,parent,false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceSuggestAdapter.Holder holder, int position) {
        holder.placeName.setText(list.get(position).placeName);
        holder.place_address.setText(list.get(position).placeAddress);

        holder.suggestionContainer.setOnClickListener(view -> {
            ELocation eLocation = list.get(holder.getBindingAdapterPosition());
            Intent returnPlace = new Intent();
            returnPlace.putExtra("placeName",eLocation.placeName);
            returnPlace.putExtra("mapplsPin",eLocation.getMapplsPin());
            activity.setResult(Activity.RESULT_OK,returnPlace);
            activity.finish();
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class Holder extends RecyclerView.ViewHolder {
        TextView placeName, place_address;
        ConstraintLayout suggestionContainer;

        public Holder(View itemView){
            super(itemView);
            placeName = itemView.findViewById(R.id.place_name);
            place_address = itemView.findViewById(R.id.place_address);
            suggestionContainer = itemView.findViewById(R.id.suggestion_container);
        }
    }
}
