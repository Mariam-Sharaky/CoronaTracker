package com.example.coronatracker;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.coronatracker.LogActivity.lastLoc;

public class GPSActivity extends FragmentActivity implements OnMapReadyCallback {


    private GoogleMap map;
    private double x = 0f;
    private double y = 0f;
    private String TAG = "GPSActivity";
    Timer timer;
    private final int INTERVAL_REFRESH = 1000 * 60 * 60; //10 seconds during testing, to be changed to 1 hour when everything is working same with location collection

    //taken from ch18 running app and edited
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_coordinates);

        // start service
        Context context = getApplicationContext();
        Intent service = new Intent(context, GoogleMapsService.class);
        context.startService(service);


        // if GPS is not enabled, start GPS settings activity
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please enable GPS!", Toast.LENGTH_LONG).show();
            Intent intent2 = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent2);
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        if (map != null) {
            map.getUiSettings().setZoomControlsEnabled(true);
            UpdateMap();
            setMapToRefresh();
        }
    }
    private void setMapToRefresh(){
        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                GPSActivity.this.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run() {
                        UpdateMap();
                    }
                });
            }
        };
        timer.schedule(task, INTERVAL_REFRESH, INTERVAL_REFRESH);
    }

    private void UpdateMap(){
        //will put a marker on the map for every location in the list
        if (!lastLoc.isEmpty()){
            LatLng latlng = new LatLng(x,y);
            Iterator i = lastLoc.iterator();
            while (i.hasNext()) {
                HashMap<String, Object> hash_map = (HashMap<String, Object>) i.next();
                Log.d(TAG, hash_map.toString());
                x = (Double)hash_map.get("latitude");
                y = (Double)hash_map.get("longitude");
                latlng = new LatLng(x,y);
                map.addMarker(    // add new marker on the map
                        new MarkerOptions()
                                .position(latlng)
                                .title("Latitude: " + x + "\nLongitude: " + y));
                Log.d(TAG,"Latitude: " + x + "\nLongitude: " + y + " Marker added");
            }
            map.animateCamera(//shift the camera to the last location
                    CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(latlng)
                                    .zoom(16.5f)
                                    .bearing(0)
                                    .tilt(25)
                                    .build()));
        }
    }

}


