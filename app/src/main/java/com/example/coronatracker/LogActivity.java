package com.example.coronatracker;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.auth.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.coronatracker.MainActivity.mAuth;

public class LogActivity extends AppCompatActivity {
    private String TAG = "LogActivity";
    private FirebaseUser user;
    private FirebaseFirestore db;
    private String fname;
    private String lname;
    private String email;
    private String phone;
    private String password;
    private boolean corona;
    private boolean coronacheck;
    protected static List<HashMap<String,Object>> lastLoc = new ArrayList<>();
    private TextView welcome;
    private Button coronaButton;
    private Button viewEditInfoButton;
    private Button viewLocationsButton;
    private Button logOutButton;
    private Timer timer;
    private Intent serviceIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        Log.d(TAG, "started activity");

        // Initialize Firebase Auth
        user = mAuth.getCurrentUser();
        Log.d(TAG,"Signed in user email: "+user.getEmail());
        email = user.getEmail();


        db = FirebaseFirestore.getInstance();

        welcome = findViewById(R.id.welcomeTextView);
        coronaButton = findViewById(R.id.coronaButton);
        viewEditInfoButton = findViewById(R.id.viewEditInfoButton);
        viewLocationsButton = findViewById(R.id.locationButton);
        logOutButton = findViewById(R.id.logOutButton);

        coronaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                corona = !corona;
                db.collection("users").document(user.getEmail()).update("corona",corona);
                setCorona();
                if(corona) {
                    checkCoronaLocations();
                }
            }
        });

        viewEditInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //view info activity
                Intent intent = new Intent(getApplicationContext(),DisplayInfo.class);
                intent.putExtra("name",fname+" "+lname);
                intent.putExtra("email",email);
                intent.putExtra("password",password);
                intent.putExtra("phone",phone);
                intent.putExtra("corona",corona);
                startActivity(intent);

            }
        });

        viewLocationsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //gps activity
                Intent intent = new Intent(getApplicationContext(), GPSActivity.class);
                intent.putExtra("email",email);
                startActivity(intent);
            }
        });

        logOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut(); //sign out
                Intent intent = new Intent(getApplicationContext(),MainActivity.class); //return to main menu
                startActivity(intent);
            }
        });

        serviceIntent = new Intent(this, GoogleMapsService.class);
    }
    @Override
    public void onStart() {
        super.onStart();
        db.collection("users").document(email).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        //https://medium.com/firebase-tips-tricks/how-to-map-an-array-of-objects-from-cloud-firestore-to-a-list-of-objects-122e579eae10
                        fname =document.get("fname").toString();
                        lname = document.get("lname").toString();
                        phone = document.get("phone").toString();
                        corona = (boolean)document.get("corona");
                        password = document.get("password").toString();
                        lastLoc = (List<HashMap<String, Object>>) document.get("locations");
                        coronacheck = (boolean)document.get("coronacheck");

                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        welcome.setText("Welcome, "+fname+"!");
                        setCorona();
                        possibleCorona();
                        askforPermission();

                    } else {
                        Log.d(TAG, "No such document");
                        mAuth.signOut();
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                    mAuth.signOut();
                }
            }
        });
    }
    private void askforPermission()
    {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},123);
        else{ //--permission previously granted
            startService(serviceIntent);
        }
    }
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 123)
            if(grantResults.length == 1&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // start service
                Intent intent = new Intent(getApplicationContext(),GoogleMapsService.class);
                startService(intent);
            }
    }
    private void setCorona()
    {
        if(!corona)
        {
            coronaButton.setText("Help I just got corona");
            coronaButton.setBackgroundColor(getResources().getColor(R.color.HelpCorona));
        }
        else
        {
            coronaButton.setText("Hurray I just recovered from corona");
            coronaButton.setBackgroundColor(getResources().getColor(R.color.HurrayCorona));
        }
    }

    private class CoronaCheck extends AsyncTask<String,Void,Boolean>
    {
        private String email2;
        List<HashMap<String,Object>> newlocations;
        boolean alert = false;
        boolean gotuser = false;

        @Override
        protected Boolean doInBackground(String... strings) {
            email2 = strings[0]; //set email
            //get comparing user's locations
            while(!getUser()) {}; //wait till it gets user
            Iterator i = lastLoc.iterator();
            Iterator j = newlocations.iterator();
            HashMap<String, Object> infected_location;
            HashMap<String, Object> compare_location;
            while (i.hasNext() && !alert) {
                infected_location = (HashMap<String, Object>) i.next();
                Log.d(TAG, infected_location.toString());
                //0.001 latitude difference is approximately 0.11km
                //0.001 longtitude difference is approximately 0.11km
                //gathered by trial and error in here https://www.geodatasource.com/distance-calculator
                while (j.hasNext() && !alert) {
                    //check if it is in 100m distance latitude wise
                    compare_location = (HashMap<String, Object>) j.next();
                    Log.d(TAG,compare_location.toString());
                    if ((double) compare_location.get("latitude") >= (double) infected_location.get("latitude") - 0.11 &&
                            (double) compare_location.get("latitude") <= (double) infected_location.get("latitude") + 0.11) {
                        alert = true;
                    } else if ((double) compare_location.get("longitude") >= (double) infected_location.get("longitude") - 0.11 &&
                            (double) compare_location.get("longitude") <= (double) infected_location.get("longitude") + 0.11) {
                        alert = true;
                    }
                }
            }
            return alert;
        }
        private boolean getUser()
        {
            db.collection("users").document(email2).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            newlocations = (List<HashMap<String, Object>>) document.get("locations");
                            gotuser = true;

                        } else {
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });
            return gotuser;
        }

        @Override
        protected void onPostExecute(Boolean alert) {
            if(alert) //updates flag for other user so that users get notified of possible infection
                db.collection("users").document(email2).update("coronacheck",true);
        }
    }

    private void checkCoronaLocations()
    {
        //get all users except logged in user
        db.collection("users")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                String email2 = document.get("email").toString();
                                Log.d(TAG,email2);
                                if(!email.equals(email2))
                                    new CoronaCheck().execute(email2);
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private void possibleCorona()
    {
        // start new timer thread
        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                db.collection("users").document(email).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                coronacheck = (boolean) document.get("coronacheck");//download alert variable
                                if(coronacheck) //if it has been changed to true, send notification warning user of possible infection
                                    sendNotification();
                                coronacheck = false; //resets flag after notification has been sent
                                db.collection("users").document(user.getEmail()).update("coronacheck",coronacheck);//update db
                            } else {
                                Log.d(TAG, "No such document");
                            }
                        } else {
                            Log.d(TAG, "get failed with ", task.getException());
                        }
                    }
                });

            }
        };
        timer = new Timer(true);
        timer.scheduleAtFixedRate(task, 0, 1000 * 10); // every 10 seconds
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendNotification()
    {
        //Taken from chapter 11 lecture slides and edited
        // create the intent for the notification
        Intent notificationIntent = new Intent(this, LogActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // create the pending intent
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, flags);

        // create the variables for the notification
        CharSequence tickerText = "You may have been in contact with a corona infected person.";
        CharSequence contentTitle = getText(R.string.app_name);
        CharSequence contentText = "Please proceed to get yourself checked and update your profile if you are infected.";
        int icon = R.drawable.cast_ic_notification_small_icon;

        NotificationChannel notificationChannel =
                new NotificationChannel("Channel_ID", "My Notifications", NotificationManager.IMPORTANCE_DEFAULT);

        NotificationManager manager = (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(notificationChannel);


        // create the notification and set its data
        Notification notification = new NotificationCompat
                .Builder(this, "Channel_ID")
                .setTicker(tickerText)
                .setSmallIcon(icon)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setChannelId("Channel_ID")
                .build();

        final int NOTIFICATION_ID = 1;
        manager.notify(NOTIFICATION_ID, notification);

    }
}