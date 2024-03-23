package com.example.majorproject.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.majorproject.R;
import com.mappls.sdk.plugin.directions.DirectionsUtils;
import com.mappls.sdk.plugin.directions.view.ManeuverView;
import com.mappls.sdk.services.api.directions.models.LegStep;

import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.List;

import timber.log.Timber;

/**
 * Created by Saksham on 17/1/20.
 */
public class StepsAdapter extends RecyclerView.Adapter<StepsAdapter.ViewHolder> {

    Context context;
    private List<LegStep> legSteps;
    OutputStream outputStream;
    long startTime,downTime;


    public StepsAdapter(List<LegStep> legSteps, OutputStream outputStream, Context applicationContext) {
        this.legSteps = legSteps;
        this.outputStream = outputStream;
        context = applicationContext;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adpter_steps, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LegStep legStep = legSteps.get(position);
        holder.stepsText.setText(DirectionsUtils.getTextInstructions((legStep)));
        holder.maneuverView.setManeuverTypeAndModifier(legStep.maneuver().type(), legStep.maneuver().modifier());

        String type = legStep.maneuver().type();
        if (type != null) {
            if (type.equalsIgnoreCase("roundabout") || type.equalsIgnoreCase("rotary")) {
                if(legSteps.get(position).maneuver().degree() != null) {
                    holder.maneuverView.setRoundaboutAngle(legStep.maneuver().degree().floatValue());
                } else {
                    holder.maneuverView.setRoundaboutAngle(180f);
                }
            }
        }

        holder.distanceText.setText(String.format("GO  %s", convertMetersToText(legSteps.get(position).distance())));

        holder.stepContainer.setOnTouchListener((view,motionEvent) -> {
//            if(outputStream == null)
//                return true;
            try {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    startTime = System.currentTimeMillis();
                }
                downTime = System.currentTimeMillis() - startTime;
                Timber.i("%s", downTime);
                if (downTime > 1000 && downTime < 1020 || downTime > 2000 && downTime < 2020 || downTime > 3080 && downTime < 3020) {
//                    outputStream.write((""+downTime/1000 + legStep.maneuver().modifier() + "\r\n").getBytes());
                    Toast.makeText(
                            context,
                            downTime/1000 + " " + legSteps.get(position).maneuver().modifier()+" angle: b"
                            +
                                    legSteps.get(position).maneuver().bearingBefore()+" a"+legSteps.get(position).maneuver().bearingAfter(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }catch (Exception e){

            }

            return true;
        });
    }

    private String convertMetersToText(double dist) {
        if ((int) (dist) <= 1000) {
            String distt = dist + "";
            if (distt.indexOf(".") > -1) {
                String distance = distt.substring(0, distt.indexOf("."));
                return distance + " mt";
            } else {
                return distt + " mt";
            }

        } else {
            double distance = (dist / 1000);
            DecimalFormat df = new DecimalFormat("#.#");
            distance = Double.valueOf(df.format(distance));
            return distance + " km";
        }
    }

    @Override
    public int getItemCount() {
        if (legSteps == null) {
            return 0;
        }
        return legSteps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private LinearLayout stepContainer;
        private TextView stepsText;
        private TextView distanceText;
        private ManeuverView maneuverView;


        ViewHolder(@NonNull View itemView) {
            super(itemView);
            stepContainer = itemView.findViewById(R.id.step_container);
            stepsText = itemView.findViewById(R.id.steps_text);
            distanceText = itemView.findViewById(R.id.distance_text);
            maneuverView = itemView.findViewById(R.id.navigate_icon);
        }
    }
}
