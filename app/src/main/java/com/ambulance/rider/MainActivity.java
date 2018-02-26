package com.ambulance.rider;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.ambulance.rider.Common.Common;
import com.ambulance.rider.Model.FCMResponse;
import com.ambulance.rider.Model.MessagingToken;
import com.ambulance.rider.Model.Notification;
import com.ambulance.rider.Model.Riders;
import com.ambulance.rider.Model.Sender;
import com.ambulance.rider.Remote.IFCMService;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, LocationListener,
        View.OnClickListener, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    // Permission Request codes
    private static final int LOCATION_PERMISSION_REQUEST = 2018;
    private static final int PLAY_SERVICE_REQUEST = 2017;
    private static final int LOCATION_ENABLE_RESOLUTION = 2016;

    // Location services
    private LocationRequest mLocationRequest;
    private LocationManager locationManager;
    private Marker mCurrent;
    private GoogleApiClient mGoogleApiClient;

    // Some static values for location update
    private static int UPDATE_INTERVAL = 5000;
    private static int FASTEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    // Google Map
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;

    // Views variable
    private Button btnBookNow;
    private FloatingActionButton myLocation;
    private PlaceAutocompleteFragment pickupLocation;
    private BottomSheetSlider mBottomSheetSlider;
    private ImageView bottomDrawer;

    // Driver finding
    boolean isDriverFound = false;
    String driverId = "";
    int radius = 1; // 1km
    int distance = 1; // 1km
    private static final int LIMIT = 5;

    // FCM Services
    IFCMService mService;

    // Presence System
    DatabaseReference driversAvailable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // IFCMService
        mService = Common.getFCMService();

        // Toolbar / AppBar

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("");

        // Navigation Drawer

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // LocationManager

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize Views

        bottomDrawer = findViewById(R.id.bottomDrawer);
        btnBookNow = findViewById(R.id.btnBookNow);
        myLocation = findViewById(R.id.myLocation);
        pickupLocation = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.pickUpLocation);

        // Button Listener
        btnBookNow.setOnClickListener(this);
        myLocation.setOnClickListener(this);
        bottomDrawer.setOnClickListener(this);

        // place change listener
        pickupLocation.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

                if (mCurrent != null) {
                    mCurrent.remove();
                }

                mCurrent = mMap.addMarker(new MarkerOptions().title("Your location")
                        .position(place.getLatLng()).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_location_on_red)));

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15.0f));

                mBottomSheetSlider = BottomSheetSlider.newInstance(place.getAddress().toString(),"Nearest Hospital");
                mBottomSheetSlider.show(getSupportFragmentManager(),mBottomSheetSlider.getTag());

            }

            @Override
            public void onError(Status status) {

                Toast.makeText(MainActivity.this, "Unable to connect to network", Toast.LENGTH_SHORT).show();

            }
        });

        requestingRuntimePermission();

        if (checkPlayServices()) {
            setupGoogleApiClient();
            setupLocationRequest();
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            showLocationEnableDialog();
            requestLocationUpdateFromProviders();
            displayLocationAndZoom();

        }

        updateFirebaseToken();

    }

    // Some custom function

    /**
     * Update firebase token
     */
    private void updateFirebaseToken() {

        DatabaseReference token_table = FirebaseDatabase.getInstance().getReference(Common.tokens);

        MessagingToken messagingToken = new MessagingToken(FirebaseInstanceId.getInstance().getToken());

        token_table.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(messagingToken);

    }

    /**
     * It will convert latitude and longitude to address
     */
    private void convertLatLangToAddress(double lat, double lng) {

        Geocoder geocoder = new Geocoder(this);

        try {

            List<Address> address = geocoder.getFromLocation(lat, lng, 1);
            String location = address.get(0).getAddressLine(0);
            pickupLocation.setText(location);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Check for play services in a device
     */
    private boolean checkPlayServices() {

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {

            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {

                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICE_REQUEST).show();

            } else {

                Toast.makeText(this, "Play service is not supported", Toast.LENGTH_SHORT).show();
                finish();

            }
            return false;
        }
        return true;

    }

    /**
     * It will setup google api client
     */
    private void setupGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

    }

    /**
     * It will setup location request
     */
    private void setupLocationRequest() {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }

    /**
     * Check for location permission at runtime
     */
    private void requestingRuntimePermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    && ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show explaination to users
                new AlertDialog.Builder(this)
                        .setTitle("Location permission needed")
                        .setMessage("We need your location to provide services with greater accuracy.")
                        .setNegativeButton("Nah", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton("Grant!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
                                }, LOCATION_PERMISSION_REQUEST);

                            }
                        })
                        .create()
                        .show();

            } else {

                // Request permission

                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
                }, LOCATION_PERMISSION_REQUEST);

            }

        }
    }

    /**
     * It will request for location update from location manager
     */
    private void requestLocationUpdateFromProviders() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if (locationManager != null) {

                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){

                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_INTERVAL, DISPLACEMENT, this);

                }

                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){

                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_INTERVAL, DISPLACEMENT, this);

                }

                /*Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                criteria.setPowerRequirement(Criteria.POWER_LOW);
                criteria.setAltitudeRequired(false);
                criteria.setBearingRequired(false);
                criteria.setCostAllowed(true);
                String provider = locationManager.getBestProvider(criteria, true);

                if (provider != null) {

                    locationManager.requestLocationUpdates(provider, UPDATE_INTERVAL, DISPLACEMENT, this);

                }*/

            }

        }

    }

    /**
     * It will remove location update from location manager
     */
    private void removeLccationUpdateFromProviders() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if (locationManager != null) {

                locationManager.removeUpdates(this);

            }

        }

    }

    /**
     * It will check for GPS_PROVIDER/NETWORK_PROVIDER for their enabling
     */
    private void showLocationEnableDialog() {
        LocationSettingsRequest.Builder locationSettingRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        locationSettingRequest.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> locationSettingsResult = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, locationSettingRequest.build());

        locationSettingsResult.setResultCallback(new ResultCallback<LocationSettingsResult>() {

            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(MainActivity.this, LOCATION_ENABLE_RESOLUTION);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        });
    }

    /**
     * It will display location on the map
     */
    private void displayLocationAndZoom() {

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if (Common.mLastLocation != null) {

                final double latitude = Common.mLastLocation.getLatitude();
                final double longitude = Common.mLastLocation.getLongitude();

                // Presence System
                driversAvailable = FirebaseDatabase.getInstance().getReference(Common.driverLoc);
                driversAvailable.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        loadAllAvailableDrivers();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                //convertLatLangToAddress(latitude, longitude);

                // Update Firebase

                /*geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {

                    @Override
                    public void onComplete(String key, DatabaseError error) {

                        // Add Marker*/

                if (mCurrent != null) {
                    mCurrent.remove();
                }

                mCurrent = mMap.addMarker(new MarkerOptions().title("Your location")
                        .position(new LatLng(latitude, longitude)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_location_on_red)));

                // Move camera to location

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15.0f));

                loadAllAvailableDrivers();

                    /*}
                });*/

            } else {

                Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show();

            }

        }

    }

    /**
     * Load all ambulance near your location
     */
    private void loadAllAvailableDrivers() {

        mMap.clear();
        mMap.addMarker(new MarkerOptions().title("Your location")
                .position(new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()))
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_location_on_red)));


        GeoFire driver_location = new GeoFire(FirebaseDatabase.getInstance().getReference(Common.driverLoc));

        GeoQuery query = driver_location.queryAtLocation(new GeoLocation(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()), distance);

        query.removeAllListeners();
        query.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, final GeoLocation location) {

                FirebaseDatabase.getInstance().getReference(Common.driverInfo)
                        .child(key)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                Riders riders = dataSnapshot.getValue(Riders.class);

                                // Add to map
                                mMap.addMarker(new MarkerOptions().title(riders.getName()).snippet(riders.getPhone())
                                        .position(new LatLng(location.latitude, location.longitude))
                                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car_black)));

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

                if (distance <= LIMIT) {

                    distance++;
                    loadAllAvailableDrivers();

                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    // Some overridden functions

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setPadding(0, 0, 0, 165);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
    }

    // Location Listener methods

    @Override
    public void onLocationChanged(Location location) {

        Log.d("FROM", location.toString());

        Common.mLastLocation = location;
        displayLocationAndZoom();

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    // Google Api Client listener

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        requestLocationUpdateFromProviders();
        displayLocationAndZoom();

    }

    @Override
    public void onConnectionSuspended(int i) {

        mGoogleApiClient.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        mGoogleApiClient.reconnect();

    }

    // Click listener

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.btnBookNow:

                /*Toast.makeText(this, "It is not functional yet", Toast.LENGTH_SHORT).show();*/
                if (!isDriverFound) {

                    GeoFire geoFire = new GeoFire(FirebaseDatabase.getInstance().getReference(Common.requestRide));
                    geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()));
                    btnBookNow.setText("Getting Drivers...");

                    findDrivers();

                } else {

                    sendRequestToDriver(driverId);

                }

                break;

            case R.id.myLocation:

                showLocationEnableDialog();
                requestLocationUpdateFromProviders();
                displayLocationAndZoom();

                break;

            case R.id.bottomDrawer:



                break;

        }

    }

    private void sendRequestToDriver(String driverId) {

        DatabaseReference db = FirebaseDatabase.getInstance().getReference(Common.tokens);

        db.orderByKey()
                .equalTo(driverId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {

                            MessagingToken token = postSnapshot.getValue(MessagingToken.class);

                            // Making payload notification for fcm
                            String riderLatLng = new Gson().toJson(new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()));

                            Log.d("RiderLocationOnReqDr", riderLatLng);

                            Notification notification = new Notification(FirebaseInstanceId.getInstance().getToken(), riderLatLng);

                            Sender sender = new Sender(notification, token.getToken());

                            mService.sendMessage(sender)
                                    .enqueue(new Callback<FCMResponse>() {
                                        @Override
                                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {

                                            if (response.body().success == 1) {

                                                Toast.makeText(MainActivity.this, "Request Sent!", Toast.LENGTH_SHORT).show();

                                            } else {

                                                Toast.makeText(MainActivity.this, "Request failed!", Toast.LENGTH_SHORT).show();

                                            }

                                        }

                                        @Override
                                        public void onFailure(Call<FCMResponse> call, Throwable t) {

                                            Log.e("ERROR", t.getMessage());

                                        }
                                    });

                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

    }

    private void findDrivers() {

        GeoFire drivers = new GeoFire(FirebaseDatabase.getInstance().getReference(Common.driverLoc));

        GeoQuery geoQuery = drivers.queryAtLocation(new GeoLocation(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()), radius);

        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                if (!isDriverFound) {

                    isDriverFound = true;
                    driverId = key;
                    btnBookNow.setText("Call Driver");
                    Toast.makeText(MainActivity.this, "Driver ID: " + key, Toast.LENGTH_SHORT).show();

                }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

                // if Driver not found in radius = 1, then increase radius
                if (radius <= 5) {

                    if (isDriverFound) {

                        radius++;
                        findDrivers();

                    }

                }

                Log.d("RADIUS", radius + "");

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    // Activity Life Cycle

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        if (locationManager != null) {
            removeLccationUpdateFromProviders();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    // Permission / runtime dialog activity handler


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                requestLocationUpdateFromProviders();

            } else {

                // finish this activity if permission isn't granted
                finish();

            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == LOCATION_ENABLE_RESOLUTION) {

            switch (resultCode) {
                case Activity.RESULT_OK:

                    requestLocationUpdateFromProviders();

                    break;

                case Activity.RESULT_CANCELED:

                    Toast.makeText(MainActivity.this, "Please enable Location Providers.", Toast.LENGTH_SHORT).show();

                    break;

            }
        }

    }
}