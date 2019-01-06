package com.example.ryan.honours_project;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;


public class LocationHandler {

    private static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 100;
    LocationRequest requests = new LocationRequest();

    private Activity activity;
    private double[] loc = new double[2];
    private FusedLocationProviderClient mFusedLocationClient;



    public LocationHandler(Activity act) {
        requests.setPriority(100);
        requests.setInterval(2000);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(act);

        if (ActivityCompat.checkSelfPermission(act, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(act, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(act,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(act, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_ACCESS_FINE_LOCATION);
            }
        }
        mFusedLocationClient.getLastLocation().addOnSuccessListener(act, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    System.out.println("test");
                    // Logic to handle location object
                    setLoc(location.getLongitude(),location.getLatitude());

                }
            }
        });
    }


    public void setLoc(double loc1, double loc2){
        System.out.println("loc1= "+ loc1);
        System.out.println("loc2= "+ loc2);
        this.loc[0] = loc1;
        this.loc[1] = loc2;
    }

    public double[] getLoc() {
        return loc;
    }
}
