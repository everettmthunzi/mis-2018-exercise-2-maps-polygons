package com.example.mis.polygons;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.LinkedList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback{

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;

        if (mLocationPermissionsGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);

        }
    }

    private static final String TAG = "MapActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;

    //vars
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    // PolygonOptions, will hold all vertices of the polygon
    private PolygonOptions polyOpts = new PolygonOptions();
    // var referencing button that initiates polygon capture/drawing
    private Button polygonButton;
    // Boolean signifying ability to add vertices (markers) to Polygon
    private Boolean polygonCaptureEnabled = false;

    /* OnClickListener for starting a Polygon
     * (general syntax via https://stackoverflow.com/a/26147707)
     */
    private View.OnClickListener startPolygon = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            // Notify the user that a Polygon has started
            Toast polygonStartToast = Toast.makeText(MapsActivity.this,
                    "Polygon capture started", Toast.LENGTH_SHORT);
            polygonStartToast.show();

            // Change the text of the button
            polygonButton.setText("End Polygon");

            // Set its onClick listener to endPolygon
            polygonButton.setOnClickListener(endPolygon);

            // Clear out the existing PolygonOptions
            polyOpts = new PolygonOptions();

            // make it semitransparent blue
            polyOpts.fillColor(0x800000FF);

            // enable Polygon capture
            polygonCaptureEnabled = true;
        }

    };

    // OnClickListener for ending a Polygon
    private View.OnClickListener endPolygon = new View.OnClickListener() {

        @Override
        public void onClick(View view){
            // Notify the user that a polygon has been constructed
            Toast polygonEndToast = Toast.makeText(MapsActivity.this,
                    "Drawing Polygon...", Toast.LENGTH_SHORT);
            polygonEndToast.show();

            // Change the text of the button
            polygonButton.setText("Start Polygon");

            // Set its onClick listener to startPolygon
            polygonButton.setOnClickListener(startPolygon);

            // disable Polygon capture
            polygonCaptureEnabled = false;

            if (polyOpts.getPoints().size() > 1) {
                // show Polygon
                Polygon polygon = mMap.addPolygon(polyOpts);

                // calculate surface area of Polygon
                Thread surfaceCalculation = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Double area = calculatePolygonArea(polyOpts.getPoints());

                        // make units sane
                        String title;
                        if (area > 1000) {
                            title = (area / 1000) + "km²";
                        } else {
                            title = area + "m²";
                        }
                        final String displayArea = title;

                        final LatLng centroid = polygonCentroid(polyOpts.getPoints());

                        // display area as marker at Polygon centroid
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mMap.addMarker(new MarkerOptions()
                                        .position(centroid)
                                        .title(displayArea));
                            }
                        });
                    }
                });
            }
            else {
                Toast tooFewMarkersCaptured = Toast.makeText(MapsActivity.this,
                        "Too few markers captured!", Toast.LENGTH_SHORT);
                tooFewMarkersCaptured.show();
            }
        }

    };

    // Method to calculate the area of a Polygon, base unit m²
    private Double calculatePolygonArea(List<LatLng> vertices) {
        // List of vertices relative to the first one, distance in meters
        List<Pair<Double, Double>> vm = new LinkedList<>();
        // add first vertex
        vm.add(new Pair<>(0.0, 0.0));

        // first vertex, all others relative to this one
        LatLng v0 = vertices.get(0);
        // see below
        Double xFactor1 = Math.cos(Math.toRadians(v0.latitude)) * 111321;

        // for loop traversing through vertices and making them relative
        for (int i = 1; i < vertices.size(); i++) {
            // get vertex
            LatLng v1 = vertices.get(i);

            /* Find out how long (in m) a degree of longitude is and use it as
             * a factor to find out the x distance between two LatLongs
             * (via https://gis.stackexchange.com/a/142327)
             */
            Double xFactor2 = Math.cos(Math.toRadians(v1.latitude)) * 111321;
            // average the two factors to account for differences in latitude
            Double xFactor = (xFactor1 + xFactor2) / 2.0;

            // Calculate x and y distances (coordinates) relative to v0
            Double x = (v1.longitude - v0.longitude) * xFactor;
            Double y = (v1.latitude - v0.latitude) * 111000;

            // add new relative vertex to list
            vm.add(new Pair<>(x, y));
        }

        // accumulated area, to be divided by 2 later
        Double area = 0.0;

        /* for loop traversing through relative vertices and calculating the
         * area of the polygon in m²
         * (via https://www.mathopenref.com/coordpolygonarea.html)
         */
        for (int i = 0; i < vm.size()-1; i = i+2) {
            // get pair of vertices
            Pair<Double, Double> vm1 = vm.get(i);
            Pair<Double, Double> vm2;
            if (i+1 < vm.size()) {
                vm2 = vm.get(i+1);
            }
            else {
                vm2 = vm.get(0);
            }

            // calculate area fragment (not yet divided by 2)
            Double frag = (vm1.first * vm2.second) - (vm1.second * vm2.first);
            area += frag;
        }

        return Math.abs(area/2.0);
    }

    /* Find centroid of polygon
     * (via https://math.stackexchange.com/a/699413)
     */
    private LatLng polygonCentroid(List<LatLng> vertices) {
        Double latitudes = 0.0;
        Double longitudes = 0.0;

        for (LatLng v : vertices) {
            latitudes += v.latitude;
            longitudes += v.longitude;
        }

        return new LatLng(latitudes/vertices.size(),
                longitudes/vertices.size());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        getLocationPermission();

        // Reference the polygon button from activity_main.xml
        polygonButton = findViewById(R.id.polygonButton);
        /* somehow unable to set onClick listener in Android Studio UI,
         * doing it the hard way
         */
        polygonButton.setOnClickListener(startPolygon);
    }

    private void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try{
            if(mLocationPermissionsGranted){

                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();

                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                    DEFAULT_ZOOM);

                        }else{
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapsActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage() );
        }
    }

    private void moveCamera(LatLng latLng, float zoom){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    private void initMap(){
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(MapsActivity.this);
    }

    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
                initMap();
            }else{
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;

        switch(requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for(int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    //initialize our map
                    initMap();
                }
            }
        }
    }


}