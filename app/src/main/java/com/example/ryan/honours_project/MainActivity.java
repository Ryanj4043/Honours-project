package com.example.ryan.honours_project;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements SensorEventListener{
    private static final int REQUEST_CHECK_SETTINGS = 100;
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "upadtekey";
    MapView map = null;
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
    double mSpeed = 0.0; // km/h
    float mAzimuthAngleSpeed;
    RotationGestureOverlay mRotationGestureOverlay;
    double bearing;
    double distance;
    double azimuth;
    double lastBuzz = 0;

    ImageView left;
    ImageView middle;
    ImageView right;
    LinearLayout navBar;

    AlertDialog dialog;

    JSONObject route;
    ArrayList<double[]> waypoints = new ArrayList<>();

    Vibrator v;
    VibrationEffect vRight;

    public static SensorManager mSensorManager;
    public static Sensor accelerometer;
    public static Sensor magnetometer;

    public static float[] mAccelerometer = null;
    public static float[] mGeomagnetic = null;
    long lastBuzzTime = 0;
    long current = System.currentTimeMillis();


    @TargetApi(Build.VERSION_CODES.O)
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

        //set up vibrate
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] patternR = {0, 2000, 750};

        vRight = VibrationEffect.createWaveform(patternR,0);

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

        //set up visual cues
        left = findViewById(R.id.left);
        right = findViewById(R.id.right);
        middle = findViewById(R.id.middle);
        navBar = findViewById(R.id.barView);


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

        onCreateDialog();

        //continually centres map on user
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    GeoPoint user=  new GeoPoint(location.getLatitude(), location.getLongitude());
                    mCurrentLocation = location;
                    mSpeed = mCurrentLocation.getSpeed() * 3.6;
                    mapController.animateTo(user);
                    if (mSpeed >= 0.1) {
                        mAzimuthAngleSpeed = mCurrentLocation.getBearing();
                    }
                    if(target != null){
                        distance = distance(location.getLatitude(),target[0],location.getLongitude(), target[1]);
                        bearing = getBearing(mCurrentLocation.getLatitude(),target[0],mCurrentLocation.getLongitude(), target[1]);
                        map.setMapOrientation(-mAzimuthAngleSpeed);
                        if(!determineOnRoute(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude())){
                            //callToast("Off track!");
                            dialog.show();
                        }
                        if(distance <= 5.0){
                            callToast("Arrived at your destination!");
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

        //System.out.println(geoJSON);

        kmlDocument.parseGeoJSON(geoJSON);
        try {
            route = new JSONObject(geoJSON);
            JSONArray wayPoints = route.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates");
            for(int i = 0; i < wayPoints.length(); i++){
                waypoints.add(new double[]{wayPoints.getJSONArray(i).getDouble(0), wayPoints.getJSONArray(i).getDouble(1)});
                System.out.println(waypoints.get(i)[0]);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        KMLStyler styler = new KMLStyler();
        kmlOverlay = (FolderOverlay)kmlDocument.mKmlRoot.buildOverlay(map, null,styler, kmlDocument);

        map.getOverlays().add(kmlOverlay);
        IMapController mapController = map.getController();
        mapController.setZoom(20);

        searchView.setVisibility(View.GONE);
        map.invalidate();
    }

    /**
     *
     * @param lat1 lat of current position
     * @param lat2 lat of destination
     * @param lon1 long of current position
     * @param lon2 long of destination
     * @return distance
     */

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

    @RequiresApi(api = Build.VERSION_CODES.O)
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
                    bearing = getBearing(mCurrentLocation.getLatitude(), target[0], mCurrentLocation.getLongitude(), target[1]);
                    double diff = Math.abs(bearing - azimuth);
                    double dDiff = Math.abs(diff-lastBuzz);
                    long timeElapsed = current - lastBuzzTime;
                    current = System.currentTimeMillis();
                    if(dDiff >= 5 && timeElapsed >= 2000) {
                        navBar.setVisibility(View.GONE);
                        middle.setVisibility(View.GONE);
                        System.out.println("Diff: " + diff);
                        if ((diff <= 344 && diff >= 16)) {
                            v.cancel();
                            lastBuzzTime = System.currentTimeMillis();
                        } else if (diff <= 15 || (diff <= 360 && diff >= 345)) {
                            //v.cancel();
                            //System.out.println("Bingo");
                            v.vibrate(vRight);
                            lastBuzzTime = System.currentTimeMillis();
                            callToast("You're on the right track!");
                            navBar.setVisibility(View.VISIBLE);
                            middle.setVisibility(View.VISIBLE);
                        }
                        lastBuzz = diff;

                    }

                }

            }
        }
    }

    public void callToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    public void removedwaypoints(double userLat, double userLong){
        for(int i = 0; i <= waypoints.size(); i++){
            if(distance(userLat,waypoints.get(i)[1],userLong,waypoints.get(i)[0]) <= 4){
                waypoints.remove(i);
            }
        }
    }

    public boolean determineOnRoute(double userLat, double userLong){
        boolean tf = true;
        double maxD = 7.5;
        //latitudes
        double y1 = waypoints.get(0)[1];
        double y2 = waypoints.get(1)[1];
        //longitudes
        double x1 = waypoints.get(0)[0];
        double x2 = waypoints.get(1)[0];
        //gradient of line
        double m = (y2-y1)/(x2-x1);
        //constant
        double c =y1-m*x1;

        //perpendicular line
        double mp = -1/m;
        double cp = userLat-mp*userLong;

        //find common point
        double x = (cp - c) / (m - mp);
        double y = m * x + c;
        //System.out.println(distance(userLat,y,userLong,x));
        if(distance(userLat,y,userLong,x) >= maxD){
            System.out.println("False: ");
            System.out.println(distance(userLat,y,userLong,x));
            tf = false;
        }
        return tf;
    }

    public Dialog onCreateDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("You are off Track! Would you like to reroute to your destination?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dao.getRoute(getcoords(), target, MainActivity.this);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        dialog = builder.create();

        return dialog;
    }
}