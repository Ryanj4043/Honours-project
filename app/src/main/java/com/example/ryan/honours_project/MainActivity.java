package com.example.ryan.honours_project;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;



public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CHECK_SETTINGS = 100;
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "upadtekey";
    MapView map = null;
    private static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 218;
    private static final int MY_PERMISSIONS_ACCESS_EXTERNAL_STORAGE = 318;
    private MyLocationNewOverlay mLocationOverlay;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location mCurrentLocation;
    private LocationCallback mLocationCallback;
    private LocationRequest requests = LocationRequest.create();
    boolean mRequestingLocationUpdates;
    private DAO dao ;


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //get location
        createLocationRequest();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                mRequestingLocationUpdates = true;
                startLocationUpdates();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS );
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });



        //create instance
        super.onCreate(savedInstanceState);

        //sets up dao
        dao = new DAO(this);

        //sets the content view
        setContentView(R.layout.activity_main);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        //sets the map up for usage
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        //creation and management of the maps openning view
        final IMapController mapController = map.getController();
        GeoPoint startpoint = new GeoPoint(55.864237, -4.251806);
        mapController.setZoom(15);
        mapController.setCenter(startpoint);

        //adds an overlay to the users location
        this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx), map);
        this.mLocationOverlay.enableMyLocation();
        map.getOverlays().add(this.mLocationOverlay);


        //continually centres map on user
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    GeoPoint user= new GeoPoint(location.getLatitude(), location.getLongitude());
                    mCurrentLocation = location;
                    System.out.println("UPDATE");
                    System.out.println(location.getLatitude());
                    System.out.println(location.getLongitude());
                    mapController.setCenter(user);
                }
            }
        };

        updateValuesFromBundle(savedInstanceState);
    }

    public void onResume() {
        super.onResume();
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up

    }

    public void onPause() {
        super.onPause();
        stopLocationUpdates();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    protected void createLocationRequest() {
        requests = LocationRequest.create();
        requests.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        requests.setInterval(1000);
        requests.setFastestInterval(500);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(requests);

    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        mFusedLocationClient.requestLocationUpdates(requests, mLocationCallback, null /* Looper */);
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        // ...
        super.onSaveInstanceState(outState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        // Update the value of mRequestingLocationUpdates from the Bundle.
        if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
            mRequestingLocationUpdates = savedInstanceState.getBoolean(
                    REQUESTING_LOCATION_UPDATES_KEY);
        }

        // ...

        // Update UI to match restored state
        //updateUI();
    }

    private Location getmCurrentLocation(){
        return this.mCurrentLocation;
    }

    private double[] getcoords(){
        double[] output = new double[2];
        output[0] = mCurrentLocation.getLongitude();
        output[1] = mCurrentLocation.getLatitude();
        return output;
    }

}