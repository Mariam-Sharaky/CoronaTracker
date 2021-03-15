package com.example.coronatracker;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

import static com.example.coronatracker.LogActivity.lastLoc;
import static com.example.coronatracker.MainActivity.mAuth;

//https://stackoverflow.com/questions/16898675/how-does-it-work-requestlocationupdates-locationrequest-listener

public class GoogleMapsService extends Service implements ConnectionCallbacks, OnConnectionFailedListener,
        LocationListener {

    private GPSActivity gps;

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    public static final int UPDATE_INTERVAL = 1000 * 60 * 60;         // 10 seconds during testing, to be changed to 1 hour when everything is working
    public static final int FASTEST_UPDATE_INTERVAL = 1000 * 60 * 60; // 10 seconds during testing

    private String TAG = "LocationService";


    @Override
    public void onCreate() {
        //create gps object to call method
        gps = new GPSActivity();

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // get location request and set it up
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL);
        Log.d(TAG, "Service created");

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        googleApiClient.connect();
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "Service destroyed");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i("Message: ", "Location changed, " + location.getAccuracy() + " , " + location.getLatitude() + "," + location.getLongitude());
        HashMap<String,Object> newlocation = new HashMap<>();
        newlocation.put("latitude",location.getLatitude());
        newlocation.put("longitude",location.getLongitude());
        if(lastLoc.size() == 336) // 1 location collected per hour, incubation period of 2 weeks (14 days) ==> 24 x 14 = 336 relevant locations
            lastLoc.remove(0);//remove oldest location if we have a full 2 weeks of locations
        lastLoc.add(newlocation);//add new location
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        try {
            db.collection("users").document(mAuth.getCurrentUser().getEmail()).update("locations", lastLoc);
        }
        catch (Exception e)
        {
            onDestroy();
        }

    }

    public void onConnected(Bundle dataBundle) {
        // Check Permissions Now
        try {
            LocationServices.FusedLocationApi
                    .requestLocationUpdates(
                            googleApiClient, locationRequest, this);
        }
        catch (SecurityException s){
            Log.d(TAG,"Not able to run location services...");
        }
    }

    @Override
    public void onDestroy() {
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        super.onDestroy();
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.
                    removeLocationUpdates(googleApiClient, this);
        }
    }

    //**************************************************************
    // Implement OnConnectionFailedListener
    //****************************************************************
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection failed! " +
                        "Please check your settings and try again.",
                Toast.LENGTH_SHORT).show();
    }
}
