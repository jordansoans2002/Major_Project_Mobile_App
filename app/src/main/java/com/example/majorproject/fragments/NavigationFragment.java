package com.example.majorproject.fragments;

import static android.app.Activity.RESULT_OK;
import static java.lang.Double.parseDouble;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.majorproject.R;
import com.example.majorproject.activity.PlaceInputActivity;
import com.example.majorproject.adapters.StepsAdapter;
import com.example.majorproject.plugins.DirectionPolylinePlugin;
import com.example.majorproject.service.NavigationService;
import com.example.majorproject.utils.BluetoothConnectionManager;
import com.example.majorproject.utils.CheckInternet;
import com.example.majorproject.utils.ObjectWrapperForBinder;
import com.mappls.sdk.geojson.Point;
import com.mappls.sdk.geojson.utils.PolylineUtils;
import com.mappls.sdk.maps.MapView;
import com.mappls.sdk.maps.MapplsMap;
import com.mappls.sdk.maps.OnMapReadyCallback;
import com.mappls.sdk.maps.annotations.MarkerOptions;
import com.mappls.sdk.maps.annotations.Polyline;
import com.mappls.sdk.maps.annotations.PolylineOptions;
import com.mappls.sdk.maps.camera.CameraPosition;
import com.mappls.sdk.maps.camera.CameraUpdateFactory;
import com.mappls.sdk.maps.geometry.LatLng;
import com.mappls.sdk.maps.geometry.LatLngBounds;
import com.mappls.sdk.maps.location.LocationComponent;
import com.mappls.sdk.maps.location.LocationComponentActivationOptions;
import com.mappls.sdk.maps.location.engine.LocationEngine;
import com.mappls.sdk.services.api.OnResponseCallback;
import com.mappls.sdk.services.api.directions.DirectionsCriteria;
import com.mappls.sdk.services.api.directions.MapplsDirectionManager;
import com.mappls.sdk.services.api.directions.MapplsDirections;
import com.mappls.sdk.services.api.directions.models.DirectionsResponse;
import com.mappls.sdk.services.api.directions.models.DirectionsRoute;
import com.mappls.sdk.services.api.directions.models.DirectionsWaypoint;
import com.mappls.sdk.services.api.directions.models.LegStep;
import com.mappls.sdk.services.api.directions.models.RouteLeg;
import com.mappls.sdk.services.utils.Constants;

import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class NavigationFragment extends Fragment implements OnMapReadyCallback, MapplsMap.OnMapLongClickListener, MapplsMap.OnPolylineClickListener {

    private MapView mapView;
    private CardView inputContainer;
    private TextView originInput, waypointInput, destInput;
    private ImageView setStop, stopIcon, getRoutes;
    private ConstraintLayout directionDetailsLayout;
    private LinearLayout journeyDetails;
    private Button startJourney;
    private TextView tvDistance, tvDuration;
    private RecyclerView steps;
    private DirectionPolylinePlugin directionPolylinePlugin;

    private MapplsMap mapplsMap;
    private final String profile = DirectionsCriteria.PROFILE_BIKING;
    private final String resource = DirectionsCriteria.RESOURCE_ROUTE;
    LocationComponent locationComponent;
    LocationEngine locationEngine;
    public static Location currentLocation;

    private String mDestination = "A5S60S";
    private String mSource;
    private String wayPoint;


    private String finalDest = "Neelkanth woods",finalStop;
    private DirectionsRoute route;
    private List<DirectionsRoute> routes;
    private List<PolylineOptions> polylines;

    OutputStream outputStream;


    ActivityResultLauncher<Intent> getDest = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();
                String placeName = data.getStringExtra("placeName");
                destInput.setText(placeName);
                if(!placeName.equalsIgnoreCase("current location"))
                    mDestination = data.getStringExtra("mapplsPin");
            }
        }
    });

    ActivityResultLauncher<Intent> getWaypoint = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();
                String placeName = data.getStringExtra("placeName");
                waypointInput.setText(placeName);
                if(!placeName.equalsIgnoreCase("current location"))
                    wayPoint = data.getStringExtra("mapplsPin");
            }
        }
    });

    ActivityResultLauncher<Intent> getOrigin = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();
                String placeName = data.getStringExtra("placeName");
                originInput.setText(placeName);
                if(!placeName.equalsIgnoreCase("current location")) {
                    mSource = data.getStringExtra("mapplsPin");
                }
            }
        }
    });

    public NavigationFragment() {
        // Required empty public constructor
    }

    public NavigationFragment(OutputStream outputStream){
        this.outputStream = outputStream;
    }

    public NavigationFragment(@NonNull DirectionsRoute route,@NonNull String dest,@Nullable String stop){
        this.route = route;

        finalDest = dest;
        finalStop = stop;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_navigation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        mapView.onCreate(savedInstanceState);

        initListeners();
    }

    void initViews(View view) {
        mapView = view.findViewById(R.id.map_view);

        inputContainer = view.findViewById(R.id.input_container);
        originInput = view.findViewById(R.id.origin_input);
        waypointInput = view.findViewById(R.id.waypoint_input);
        destInput = view.findViewById(R.id.destination_input);

        if(finalDest != null)
            destInput.setText(finalDest);
        if(finalStop != null)
            waypointInput.setText(finalStop);

        setStop = view.findViewById(R.id.set_stop);
        stopIcon = view.findViewById(R.id.stop_icon);
        getRoutes = view.findViewById(R.id.get_routes);

        journeyDetails = view.findViewById(R.id.journey_details);
        directionDetailsLayout = view.findViewById(R.id.direction_details_layout);
        startJourney = view.findViewById(R.id.start_journey);
        tvDistance = view.findViewById(R.id.tv_distance);
        tvDuration = view.findViewById(R.id.tv_duration);
        steps = view.findViewById(R.id.steps);
        steps.setLayoutManager(new LinearLayoutManager(this.getContext()));
        if(route != null) {
            List<RouteLeg> routeLegList = route.legs();
            List<LegStep> legSteps = new ArrayList<>();
            for (RouteLeg routeLeg : routeLegList)
                legSteps.addAll(routeLeg.steps());
            if (legSteps.size() > 0){
                steps.setAdapter(new StepsAdapter(legSteps, outputStream, NavigationFragment.this.getContext()));
            }


            if (route.geometry() != null) {
                ArrayList<LatLng> listOfLatLng = new ArrayList<>();
                for (Point point : PolylineUtils.decode(route.geometry(), Constants.PRECISION_6))
                    listOfLatLng.add(new LatLng(point.latitude(), point.longitude()));

                //TODO see if you can add polyline using their function
//                           drawPath(PolylineUtils.decode(route.geometry(), Constants.PRECISION_6));

                PolylineOptions options = new PolylineOptions()
                        .addAll(listOfLatLng)
                        .color(Color.parseColor("#FD9D3B"))
                        .width(7);
                polylines = new ArrayList<>(1);
                polylines.add(0,options);
            }
            updateRouteData(route);
//            steps.setAdapter(new StepsAdapter(legSteps, outputStream, NavigationFragment.this.getContext()));
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    void initListeners() {
        mapView.getMapAsync(this);

        mapView.setOnTouchListener(((view,motionEvent) -> {
            Timber.d("map click");
            if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                if (inputContainer.getVisibility() == View.VISIBLE) {
                    inputContainer.setVisibility(View.GONE);
                    directionDetailsLayout.setVisibility(View.GONE);
                } else {
                    inputContainer.setVisibility(View.VISIBLE);
//                    if (directionPolylinePlugin != null)
                    if(routes != null && routes.size()>0)
                        directionDetailsLayout.setVisibility(View.VISIBLE);
                }
            }
            return false;
        }));

        setStop.setOnClickListener(view -> {
            int visibility = (waypointInput.getVisibility() == View.GONE) ? View.VISIBLE : View.GONE;
            waypointInput.setVisibility(visibility);
            stopIcon.setVisibility(visibility);
            setStop.setImageResource((visibility == View.VISIBLE) ? R.drawable.baseline_wrong_location_24 : R.drawable.add_location);
        });

        originInput.setOnClickListener((view) -> {
            if (mapplsMap == null) {
                Toast.makeText(
                        this.getContext(),
                        "Map is not ready",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            Intent placeInput = new Intent(this.getContext(), PlaceInputActivity.class);
            currentLocation = locationComponent.getLastKnownLocation();
            placeInput.putExtra("isOrigin", true);
            getOrigin.launch(placeInput);
        });
        waypointInput.setOnClickListener((view) -> {
            if (mapplsMap == null) {
                Toast.makeText(
                        this.getContext(),
                        "Map is not ready",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            Intent placeInput = new Intent(this.getContext(), PlaceInputActivity.class);
            placeInput.putExtra("isOrigin", false);
            getWaypoint.launch(placeInput);
        });
        destInput.setOnClickListener((view) -> {
            if (mapplsMap == null) {
                Toast.makeText(
                        this.getContext(),
                        "Map is not ready",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            Intent placeInput = new Intent(this.getContext(), PlaceInputActivity.class);
            placeInput.putExtra("isOrigin", false);
            getDest.launch(placeInput);
        });

        getRoutes.setOnClickListener(view -> {
            String ori = originInput.getText().toString();
            if (ori.length() > 1 && destInput.getText().length() > 1) {
                if (ori.equalsIgnoreCase("Current location")) {
                    currentLocation = locationComponent.getLastKnownLocation();
                    mSource = currentLocation.getLatitude() + "," + currentLocation.getLongitude();
                }
                mapplsMap.clear();
                routes = null;
                polylines = null;
                finalDest = destInput.getText().toString();
                finalStop = (waypointInput.getVisibility() == View.VISIBLE)? waypointInput.getText().toString() : null;
                getDirections();
            }
        });

        directionDetailsLayout.setOnClickListener((view) -> {
            if (steps.getVisibility() == View.VISIBLE)
                steps.setVisibility(View.GONE);
            else
                steps.setVisibility(View.VISIBLE);
        });

        startJourney.setOnClickListener(view -> {
            //TODO check if location is enabled, if not prompt to enable
            if(originInput.getText().toString().equalsIgnoreCase("current location")){
                final Object objSent = route;
                final Bundle bundle = new Bundle();
                bundle.putBinder("route",new ObjectWrapperForBinder(objSent));
                bundle.putBinder("locationEngine",new ObjectWrapperForBinder(locationEngine));
                bundle.putBinder("inputStream",new ObjectWrapperForBinder(BluetoothConnectionManager.inputStream));
                bundle.putBinder("outputStream",new ObjectWrapperForBinder(BluetoothConnectionManager.outputStream));

                Intent startJourney = new Intent(this.getContext(), NavigationService.class);
                startJourney.putExtras(bundle);
                startJourney.putExtra("destination",finalDest);
                startJourney.putExtra("stop",finalStop);
                startJourney.setAction("START_SERVICE");

                this.getContext().startService(startJourney);
            } else {
                Toast.makeText(NavigationFragment.this.getContext(),"You need to be at origin to start a journey", Toast.LENGTH_LONG)
                        .show();
            }
        });
    }

    @Override
    public void onMapReady(MapplsMap mapplsMap) {
        this.mapplsMap = mapplsMap;

        mapplsMap.setPadding(20, 20, 20, 20);
        mapplsMap.addOnMapLongClickListener(this);
        mapplsMap.setOnPolylineClickListener(this);

        mapplsMap.getStyle(style -> {
            if(polylines != null && polylines.size()>0)
                mapplsMap.addPolyline(polylines.get(0));

            locationComponent = mapplsMap.getLocationComponent();
            LocationComponentActivationOptions locationComponentActivationOptions = LocationComponentActivationOptions.builder(this.getContext(), style)
                    .build();
            locationComponent.activateLocationComponent(locationComponentActivationOptions);

            //TODO request permission is not provided then disable start journey button
            if (ActivityCompat.checkSelfPermission(this.getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                return;

            locationComponent.setLocationComponentEnabled(true);
            locationEngine = locationComponent.getLocationEngine();

            currentLocation = locationComponent.getLastKnownLocation();
            if (currentLocation != null) {
                CameraPosition position = new CameraPosition.Builder()
                        .target(new LatLng(currentLocation)) // Sets the new camera position
                        .zoom(14) // Sets the zoom to level 14
                        .tilt(0) // Set the camera tilt to 45 degrees
                        .build();
                mapplsMap.setCameraPosition(position);
            }
        });

        if (CheckInternet.isNetworkAvailable(this.getContext())) {
            //TODO show some error screen
            Toast.makeText(this.getContext(), "Please Check Internet Connection", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onMapLongClick(@NonNull LatLng latLng) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this.getContext());
        alertDialog.setMessage("Select Point as Source or Destination");

        alertDialog.setPositiveButton("Source", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mSource = latLng.getLatitude() + "," + latLng.getLongitude();
            }
        });
        alertDialog.setNegativeButton("Destination", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mDestination = latLng.getLatitude() + "," + latLng.getLongitude();
            }
        });

        alertDialog.setCancelable(true);
        alertDialog.show();
        return false;
    }

    @Override
    public void onPolylineClick(@NonNull Polyline polyline) {
        for(int i=0;i<polylines.size();i++){
            Polyline line = polylines.get(i).getPolyline();
            if(line.getId() == polyline.getId()){
                route = routes.get(i);
                if(route != null) {
                    List<RouteLeg> routeLegList = route.legs();
                    List<LegStep> legSteps = new ArrayList<>();
                    for (RouteLeg routeLeg : routeLegList)
                        legSteps.addAll(routeLeg.steps());
                    if (legSteps.size() > 0) {
                        steps.setAdapter(new StepsAdapter(legSteps, outputStream, NavigationFragment.this.getContext()));
                    }

                    line.setWidth(8);
                    line.setColor(Color.parseColor("#FD9D3B"));
                    updateRouteData(route);
                }
            } else {
                line.setWidth(4);
                line.setColor(Color.parseColor("#DAD1D1"));
            }
        }
    }

    /**
     * Get Directions
     */
    private void getDirections() {
        Object dest = !mDestination.contains(",") ? mDestination : Point.fromLngLat(parseDouble(mDestination.split(",")[1]), parseDouble(mDestination.split(",")[0]));
        Object src = !mSource.contains(",") ? mSource : Point.fromLngLat(parseDouble(mSource.split(",")[1]), parseDouble(mSource.split(",")[0]));

        Timber.i("%s %s", dest, src);

        MapplsDirections.Builder builder = MapplsDirections.builder();

        if (src instanceof String) {
            builder.origin(String.valueOf(src));
        } else {
            builder.origin((Point) src);
        }

        if (dest instanceof String) {
            builder.destination(String.valueOf(dest));
        } else {
            builder.destination((Point) dest);
        }

        if (wayPoint != null && waypointInput.getVisibility() == View.VISIBLE) {
            if (!wayPoint.contains(";")) {
                if (!wayPoint.contains(",")) {
                    builder.addWaypoint(wayPoint);
                } else {
                    Point point = Point.fromLngLat(parseDouble(wayPoint.split(",")[1]), parseDouble(wayPoint.split(",")[0]));
                    builder.addWaypoint(point);
                }
            } else {
                String[] wayPointsArray = wayPoint.split(";");
                for (String value : wayPointsArray) {
                    if (!value.contains(",")) {
                        builder.addWaypoint(value);
                    } else {
                        Point point = Point.fromLngLat(parseDouble(value.split(",")[1]), parseDouble(value.split(",")[0]));
                        builder.addWaypoint(point);
                    }
                }
            }
        }

        builder.profile(profile)
                .resource(resource)
                .steps(true)
                .alternatives(true)
                .overview(DirectionsCriteria.OVERVIEW_FULL);

        MapplsDirectionManager.newInstance(builder.build()).call(new OnResponseCallback<DirectionsResponse>() {
            @Override
            public void onSuccess(DirectionsResponse directionsResponse) {
                if (directionsResponse != null) {
                    routes = directionsResponse.routes();
                    polylines = new ArrayList<>(routes.size());
                    mapplsMap.clear();
                    Timber.i(String.valueOf(routes.size()));

//                    if (results.size() > 0) {
//                        DirectionsRoute route = results.get(0);
                    DirectionsRoute route = null;
                    for(int i= routes.size()-1;i>=0;i--) {
                        route = routes.get(i);
                        if (route.geometry() != null) {
                            ArrayList<LatLng> listOfLatLng = new ArrayList<>();
                            for (Point point : PolylineUtils.decode(route.geometry(), Constants.PRECISION_6))
                                listOfLatLng.add(new LatLng(point.latitude(), point.longitude()));

                            //TODO see if you can add polyline using their function
//                           drawPath(PolylineUtils.decode(route.geometry(), Constants.PRECISION_6));

                            PolylineOptions options = new PolylineOptions()
                                    .addAll(listOfLatLng)
                                    .color(Color.parseColor("#DAD1D1"))
                                    .width(4);
                            polylines.add(0,options);
                            mapplsMap.addPolyline(options);
                        }
                    }

                    NavigationFragment.this.route = routes.get(0);
                    route = routes.get(0);
                    if(route != null) {
                        List<RouteLeg> routeLegList = route.legs();
                        List<LegStep> legSteps = new ArrayList<>();
                        for (RouteLeg routeLeg : routeLegList)
                            legSteps.addAll(routeLeg.steps());
                        if (legSteps.size() > 0) {
                            steps.setAdapter(new StepsAdapter(legSteps, outputStream, NavigationFragment.this.getContext()));
                        }

                        polylines.get(0).getPolyline().setWidth(4);
                        polylines.get(0).getPolyline().setColor(Color.parseColor("#3bb2d0"));
                        updateRouteData(route);
                    }

                    List<DirectionsWaypoint> directionsWaypoints = directionsResponse.waypoints();
                    if (directionsWaypoints != null && directionsWaypoints.size() > 0) {
                        for (DirectionsWaypoint directionsWaypoint : directionsWaypoints) {
                            mapplsMap.addMarker(new MarkerOptions().position(new LatLng(directionsWaypoint.location().latitude(), directionsWaypoint.location().longitude())));
                        }
                    }
                }
            }

            @Override
            public void onError(int i, String s) {
                Toast.makeText(NavigationFragment.this.getContext(), s + "----" + i, Toast.LENGTH_LONG).show();
            }
        });


    }

    /**
     * Update Route data
     *
     * @param directionsRoute route data
     */
    private void updateRouteData(@NonNull DirectionsRoute directionsRoute) {
        if (directionsRoute.distance() != null && directionsRoute.distance() != null) {
            journeyDetails.setVisibility(View.VISIBLE);
//            floatingActionButton.setVisibility(View.VISIBLE);
//            tvDuration.setText("(" + getFormattedDuration(directionsRoute.duration()) +" "+ mSource+","+mDestination+ ")");
            tvDuration.setText("(" + getFormattedDuration(directionsRoute.duration()) + ")");
            tvDistance.setText(getFormattedDistance(directionsRoute.distance()));
        }
    }

    /**
     * Get Formatted Distance
     *
     * @param distance route distance
     * @return distance in Kms if distance > 1000 otherwise in mtr
     */
    private String getFormattedDistance(double distance) {

        if ((distance / 1000) < 1) {
            return distance + "mtr.";
        }
        DecimalFormat decimalFormat = new DecimalFormat("#.#");
        return decimalFormat.format(distance / 1000) + "Km.";
    }

    /**
     * Get Formatted Duration
     *
     * @param duration route duration
     * @return formatted duration
     */
    private String getFormattedDuration(double duration) {
        long min = (long) (duration % 3600 / 60);
        long hours = (long) (duration % 86400 / 3600);
        long days = (long) (duration / 86400);
        if (days > 0L) {
            return days + " " + (days > 1L ? "Days" : "Day") + " " + hours + " " + "hr" + (min > 0L ? " " + min + " " + "min." : "");
        } else {
            return hours > 0L ? hours + " " + "hr" + (min > 0L ? " " + min + " " + "min" : "") : min + " " + "min.";
        }
    }

    /**
     * Add polyline along the points
     *
     * @param waypoints route points
     */
    private void drawPath(@NonNull List<Point> waypoints) {
        ArrayList<LatLng> listOfLatLng = new ArrayList<>();
        for (Point point : waypoints) {
            listOfLatLng.add(new LatLng(point.latitude(), point.longitude()));
        }

        if (directionPolylinePlugin == null) {
            directionPolylinePlugin = new DirectionPolylinePlugin(mapplsMap, mapView, profile);
            directionPolylinePlugin.createPolyline(listOfLatLng);
        } else {
            directionPolylinePlugin.updatePolyline(profile, listOfLatLng);

        }
        LatLngBounds latLngBounds = new LatLngBounds.Builder().includes(listOfLatLng).build();
        mapplsMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 30));
    }

    @Override
    public void onMapError(int i, String s) {

    }


    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}