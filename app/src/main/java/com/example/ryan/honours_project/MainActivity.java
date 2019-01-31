package com.example.ryan.honours_project;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

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
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import org.json.JSONException;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.net.URLEncoder;


public class MainActivity extends AppCompatActivity implements SensorEventListener{
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
    MaterialSearchView searchView;
    ListView lstView;
    KmlDocument kmlDocument;
    double[] target;
    FolderOverlay kmlOverlay;
    long mLastTime = 0; // milliseconds
    double mSpeed = 0.0; // km/h
    float mAzimuthAngleSpeed;
    RotationGestureOverlay mRotationGestureOverlay;
    double bearing;
    double distance;
    double azimuth;

    public static SensorManager mSensorManager;
    public static Sensor accelerometer;
    public static Sensor magnetometer;

    public static float[] mAccelerometer = null;
    public static float[] mGeomagnetic = null;



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

        //sets up sensors
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

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

        //searchbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Haptic Map Navigation");
        toolbar.setTitleTextColor(Color.parseColor("#FFFFFF"));

        //output handling
        lstView = findViewById(R.id.lstView);
        lstView.setVisibility(View.GONE);
        searchView = findViewById(R.id.search_view);
        searchView.setVisibility(View.GONE);
        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {
                searchView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onSearchViewClosed() {
                lstView.setVisibility(View.GONE);
            }
        });

        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                System.out.println(query);
                String updatedQuery = URLEncoder.encode(query);
                System.out.println(updatedQuery);


                try {
                    dao.getGeoCode(getcoords(),query, MainActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }

        });

        lstView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                lstView.setVisibility(view.GONE);
                String destination = ((TextView)view).getText().toString();

                String[] temp = destination.split(";");
                String[] tempCoords = temp[2].split(",");

                double[] dTempCoords = new double[2];
                dTempCoords[0] = Double.parseDouble(tempCoords[0]);
                dTempCoords[1] = Double.parseDouble(tempCoords[1]);

                target = dTempCoords;
                map.getOverlays().remove(kmlOverlay);
                map.invalidate();

                kmlDocument = new KmlDocument();
                dao.getRoute(getcoords(),dTempCoords, MainActivity.this);


            }
        });


        //creation and management of the maps openning view
        final IMapController mapController = map.getController();
        GeoPoint startpoint = new GeoPoint(55.864237, -4.251806);
        mapController.setZoom(15);
        mapController.setCenter(startpoint);

        //adds an overlay to the users location
        this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx), map);
        this.mLocationOverlay.enableMyLocation();
        map.getOverlays().add(this.mLocationOverlay);

        //rotation guesters
        mRotationGestureOverlay = new RotationGestureOverlay(this, map);
        mRotationGestureOverlay.setEnabled(true);
        map.setMultiTouchControls(true);
        map.getOverlays().add(this.mRotationGestureOverlay);


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
                    mSpeed = mCurrentLocation.getSpeed() * 3.6;
                    /*System.out.println("UPDATE");
                    System.out.println(location.getLatitude());
                    System.out.println(location.getLongitude());*/
                    mapController.animateTo(user);
                    if (mSpeed >= 0.1) {
                        mAzimuthAngleSpeed = mCurrentLocation.getBearing();
                    }


                    if(target != null){
                        distance = distance(location.getLatitude(),target[0],location.getLongitude(), target[1]);
                        bearing = getBearing(mCurrentLocation.getLatitude(),target[0],mCurrentLocation.getLongitude(), target[1]);
                        /*TextView textView  = findViewById(R.id.textOutput);
                        textView.bringToFront();
                        String output = "Distance: " + String.valueOf(temp) +"m" + "\r\n" + "Speed: " + mSpeed;

                        textView.setText(output);*/

                        //begin haptic compass


                        map.setMapOrientation(-mAzimuthAngleSpeed);
                        if(distance <= 5.0){
                            //write alert
                            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                            alertDialog.setMessage("You have reached your destination");
                            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            alertDialog.show();
                            target = null;
                            map.getOverlays().remove(kmlOverlay);
                            map.invalidate();
                        }

                    }

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
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        map.onResume();

    }

    public void onPause() {
        super.onPause();
        stopLocationUpdates();
        mSensorManager.unregisterListener(this, accelerometer);
        mSensorManager.unregisterListener(this, magnetometer);
        map.onPause();
    }

    protected void createLocationRequest() {
        requests = LocationRequest.create();
        requests.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        requests.setInterval(1500);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_item,menu);
        MenuItem item = menu.findItem(R.id.action_search);
        searchView.setMenuItem(item);
        return true;
    }

    public void setUpList(String[] input){
        lstView.setVisibility(View.VISIBLE);
        ArrayAdapter adapter = new ArrayAdapter (MainActivity.this,android.R.layout.simple_list_item_1,input);
        lstView.setAdapter(adapter);
    }

    public void setUpKML(String geoJSON){

        kmlDocument.parseGeoJSON(geoJSON);
        KMLStyler styler = new KMLStyler();
        kmlOverlay = (FolderOverlay)kmlDocument.mKmlRoot.buildOverlay(map, null,styler, kmlDocument);

        map.getOverlays().add(kmlOverlay);
        IMapController mapController = map.getController();
        mapController.setZoom(19);

        searchView.setVisibility(View.GONE);
        map.invalidate();
    }

    public double distance(double lat1, double lat2, double lon1, double lon2){
        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        return distance;

    }

    /**
     *
     * @param lat1 lat of current position
     * @param lat2 lat of destination
     * @param lon1 long of current position
     * @param lon2 long of destination
     * @return The number of degrees between the user and their destination
     */

    public double getBearing(double lat1, double lat2, double lon1, double lon2){
        double bearing = Math.atan2(lat2-lat1, lon2-lon1);

        return 180+Math.toDegrees(bearing);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // onSensorChanged gets called for each sensor so we have to remember the values
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mAccelerometer = event.values;
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = event.values;
        }

        if (mAccelerometer != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mAccelerometer, mGeomagnetic);

            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                // at this point, orientation contains the azimuth(direction), pitch and roll values.
                azimuth = 180 * orientation[0] / Math.PI;
                if(target != null) {
                    double modifer = 5;

                    bearing = getBearing(mCurrentLocation.getLatitude(), target[0], mCurrentLocation.getLongitude(), target[1]);
                    double diff = Math.abs(bearing - azimuth);
                    double tolerance = (bearing / 100) * modifer;
                    if (diff < tolerance) {
                        System.out.println(diff);
                    }
                }

            }
        }
    }

}