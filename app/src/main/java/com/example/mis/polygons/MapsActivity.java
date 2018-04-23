package com.example.mis.polygons;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mis.polygons.models.PlaceInfo;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/* REF: API Course
 * Title : <android Google Maps API & Google Places API Course>
 * Author: <Mitch Tabian>
 * Date Accessed: <April 20, 2018>
 * Code version : <last commit: Oct 3, 2017>
 * Availability : <https://github.com/mitchtabian/Google-Maps-Google-Places.git>
 ********************************************************************************/

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener{

    private Marker mapMarker;
    private PlaceInfo placeInfo;
    private GoogleMap layoutMap;
    private GoogleApiClient googleAPIClient;
    private Boolean locationPermissionGranted = false;
    private FusedLocationProviderClient fusedLPC;
    private PlaceAutoCompleteAdapter autocompleteAdapter;

    //Layout Widgets
    private ImageView imageViewGPS, imageViewInfo;
    private AutoCompleteTextView autoCompleteTextView;

    private static final float DEFAULT_ZOOM = 15f;
    private static final String TAG = "MapActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;

    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40, -168), new LatLng(71, 136));

    private static final int ERROR_DIALOG_REQUEST = 9001;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.input_search);
        imageViewGPS = (ImageView) findViewById(R.id.ic_gps);
        imageViewInfo = (ImageView) findViewById(R.id.place_info);

        // --confirm correct google play store services version
        if(correctGoogleServices()){
            getLocationPermission();
        }
    }

    // --confirm correct google play store services version
    public boolean correctGoogleServices(){
        Log.d(TAG, "correctGoogleServices: verifying google services version");
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MapsActivity.this);

        if(available == ConnectionResult.SUCCESS){
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MapsActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "Google Play Services unresolved", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    // --explicitly checking for location permissions
    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                initializeMap();
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    // -- evaluating|verifying : request permission result
    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] permission, @NonNull int[] results) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        locationPermissionGranted = false;
        // -- checking the request code
        switch (code) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (results.length > 0) {

                    // -- looping through all the results (grant results), initializing map if
                    //    all permissions granted.
                    for (int i = 0; i < results.length; i++) {
                        if (results[i] != PackageManager.PERMISSION_GRANTED) {
                            locationPermissionGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    locationPermissionGranted = true;

                    //initialize our map
                    initializeMap();
                }
            }
        }
    }

    // -- initializing the map based on the Google API documentation code snippet
    // https://developer.android.com/guide/components/fragments.html
    private void initializeMap() {
        // --
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);
    }

    // -- this interface method is called when map is ready
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map Ready", Toast.LENGTH_SHORT).show();
        layoutMap = googleMap;

        // -- if location granted
        if (locationPermissionGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            // -- set location properties (blue dot on the map)
            layoutMap.setMyLocationEnabled(true);

            // -- disabling default my location button (screen estate to be used for text-field)
            layoutMap.getUiSettings().setMyLocationButtonEnabled(false);
            initializeFindLocation();
        }

        // -- long press implementation
        // https://stackoverflow.com/questions/42401131/add-marker-on-long-press-in-google-maps-api-v3
        layoutMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {

                Double l1=latLng.latitude;
                Double l2=latLng.longitude;
                String coordl1 = l1.toString();
                String coordl2 = l2.toString();

                layoutMap.clear();
               cameraPosition(latLng, DEFAULT_ZOOM, "New locationL Latitude: " + coordl1
                       + " Longitude: " +coordl2 );
            }
        });
    }

    // -- initializing 'find location' using editor action listener
    private void initializeFindLocation() {
        Log.d(TAG, "initializeFindLocation: initializing");


        googleAPIClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        autoCompleteTextView.setOnItemClickListener(mAutocompleteClickListener);

        autocompleteAdapter = new PlaceAutoCompleteAdapter(this, googleAPIClient,
                LAT_LNG_BOUNDS, null);

        autoCompleteTextView.setAdapter(autocompleteAdapter);

        autoCompleteTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER) {

                    //execute geographical location based on the entered search string
                    geoLocate();
                }
                return false;
            }
        });

        imageViewGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked gps icon");
                getDeviceLocation();
            }
        });

        imageViewInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked place info");
                try {
                    if (mapMarker.isInfoWindowShown()) {
                        mapMarker.hideInfoWindow();
                    } else {
                        Log.d(TAG, "onClick: place info: " + placeInfo.toString());
                        mapMarker.showInfoWindow();
                    }
                } catch (NullPointerException e) {
                    Log.e(TAG, "onClick: NullPointerException: " + e.getMessage());
                }
            }
        });
        hideSoftKeyboard();
    }

    // -- acquiring Device location
    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        // -- initialize the fused location provider client (fusedLPC)
        fusedLPC = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (locationPermissionGranted) {
                final Task location = fusedLPC.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();

                            cameraPosition(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                    DEFAULT_ZOOM,
                                    "My Location");

                        } else {
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapsActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    //moving the position of the camera based in latitude and longitude
    private void cameraPosition(LatLng latLng, float zoom, String title) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        layoutMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        // -- pin not to be dropped at my location
        if (!title.equals("My Location")) {
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            layoutMap.addMarker(options);
        }
        hideSoftKeyboard();
    }

    // -- geographical location
    private void geoLocate() {
        Log.d(TAG, "geoLocate: geolocating");
        String searchString = autoCompleteTextView.getText().toString();
        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> list = new ArrayList<>();

        try {
            list = geocoder.getFromLocationName(searchString, 1);
        } catch (IOException e) {
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage());
        }
        if (list.size() > 0) {
            Address address = list.get(0);
            cameraPosition(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM,
                    address.getAddressLine(0));
        }
    }

    //moving the position of the camera based in latitude and longitude ( ~ method overloading)
    private void cameraPosition(LatLng latLng, float zoom, PlaceInfo placeInfo) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        layoutMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        //  -- clear map for each camera re-position
        layoutMap.clear();
        layoutMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(MapsActivity.this));

        // -- null pointer guard
        if (placeInfo != null) {
            try {
                String snippet = "Address: " + placeInfo.getAddress() + "\n" +
                        "Phone Number: " + placeInfo.getPhoneNumber() + "\n" +
                        "Website: " + placeInfo.getWebsiteUri() + "\n" +
                        "Price Rating: " + placeInfo.getRating() + "\n";

                MarkerOptions options = new MarkerOptions()
                        .position(latLng)
                        .title(placeInfo.getName())
                        .snippet(snippet);
                mapMarker = layoutMap.addMarker(options);

            } catch (NullPointerException e) {
                Log.e(TAG, "moveCamera: NullPointerException: " + e.getMessage());
            }
        } else {
            layoutMap.addMarker(new MarkerOptions().position(latLng));
        }
        hideSoftKeyboard();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    // -- hack to hide the keyboard display
    private void hideSoftKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }



    /* REF: API Course
     * Title : <android Google Maps API & Google Places API Course>
     * Author: <Mitch Tabian>
     * Date Accessed: <April 22, 2018>
     * Code version : <last commit: Oct 3, 2017>
     * Availability : <https://github.com/mitchtabian/Google-Maps-Google-Places.git>
     ********************************************************************************/

    private AdapterView.OnItemClickListener mAutocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            hideSoftKeyboard();

            final AutocompletePrediction item = autocompleteAdapter.getItem(i);
            final String placeId = item.getPlaceId();

            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(googleAPIClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
        }
    };
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.d(TAG, "onResult: Place query did not complete successfully: " + places.getStatus().toString());
                places.release();
                return;
            }
            final Place place = places.get(0);

            try {
                placeInfo = new PlaceInfo();
                placeInfo.setName(place.getName().toString());
                Log.d(TAG, "onResult: name: " + place.getName());
                placeInfo.setAddress(place.getAddress().toString());
                Log.d(TAG, "onResult: address: " + place.getAddress());
                placeInfo.setId(place.getId());
                Log.d(TAG, "onResult: id:" + place.getId());
                placeInfo.setLatlng(place.getLatLng());
                Log.d(TAG, "onResult: latlng: " + place.getLatLng());
                placeInfo.setRating(place.getRating());
                Log.d(TAG, "onResult: rating: " + place.getRating());
                placeInfo.setPhoneNumber(place.getPhoneNumber().toString());
                Log.d(TAG, "onResult: phone number: " + place.getPhoneNumber());
                placeInfo.setWebsiteUri(place.getWebsiteUri());
                Log.d(TAG, "onResult: website uri: " + place.getWebsiteUri());

                Log.d(TAG, "onResult: place: " + placeInfo.toString());
            } catch (NullPointerException e) {
                Log.e(TAG, "onResult: NullPointerException: " + e.getMessage());
            }
            cameraPosition(new LatLng(place.getViewport().getCenter().latitude,
                    place.getViewport().getCenter().longitude), DEFAULT_ZOOM, placeInfo);
            places.release();
        }
    };
}