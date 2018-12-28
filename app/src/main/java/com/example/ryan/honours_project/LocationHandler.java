package com.example.ryan.honours_project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class LocationHandler {

    private FusedLocationProviderClient mFusedLocationClient;
    private static final int  MY_PERMISSIONS_ACCESS_FINE_LOCATION = 218;
    private Activity activity;

    public LocationHandler(Activity act){
        this.activity = act;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(act, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(act,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSIONS_ACCESS_FINE_LOCATION);
            }
        }
    }

    @SuppressLint("MissingPermission")
    public double[] getLocation(){
        final double[] loc;
        loc = new double[2];
        mFusedLocationClient.getLastLocation().addOnSuccessListener(activity, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    // Logic to handle location object
                    loc[0] = location.getLongitude();
                    loc[1] = location.getLatitude();
                }
            }
        });
        return loc;
    }

}
