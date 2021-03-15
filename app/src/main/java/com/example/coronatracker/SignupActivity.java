package com.example.coronatracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class SignupActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private String TAG = "Signup";
    private EditText fnameEditText;
    private EditText lnameEditText;
    private EditText emailEditText;
    private EditText phoneEditText;
    private EditText passwordEditText;
    private Button signupButton;
    private String email;
    private String password;
    FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        Log.d(TAG,"started activity");

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        //set references
        fnameEditText = findViewById(R.id.editTextFName);
        lnameEditText = findViewById(R.id.editTextLName);
        emailEditText = findViewById(R.id.editTextEmail);
        phoneEditText = findViewById(R.id.editTextPhone);
        passwordEditText = findViewById(R.id.editTextPassword);
        signupButton = findViewById(R.id.signupBtn);


        //firebase code gotten from https://firebase.google.com/docs/auth/android/password-auth

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { //set listener for signup button

                //get all the signup details
                String fname = fnameEditText.getText().toString();
                String lname = lnameEditText.getText().toString();
                String phone = phoneEditText.getText().toString();
                email = emailEditText.getText().toString();
                password = passwordEditText.getText().toString();
                ArrayList<HashMap<String,Double>> locations = new ArrayList<>();

                //display toast if missing name, phone or email
                if(fname.equals("")||lname.equals("")||phone.equals("")||email.equals(""))
                {
                    Toast.makeText(getApplicationContext(),"Please fill all data in before signing up!",Toast.LENGTH_SHORT).show();
                }
                else if(password.length() < 6)
                    Toast.makeText(getApplicationContext(),"Password must be at least 6 characters!",Toast.LENGTH_SHORT).show();
                else {

                    // Create a new user map
                    Map<String, Object> user = new HashMap<>();//signup details
                    user.put("fname", fname);
                    user.put("lname", lname);
                    user.put("phone", phone);
                    user.put("email", email);
                    user.put("password", password);
                    user.put("corona",false);
                    user.put("locations",locations);
                    user.put("coronacheck",false);//flag to know whether to alert user of possible infection

                    // Add a new document with email as ID
                    db.collection("users")
                            .document(email)//use email for document name
                            .set(user) //add the details into the document
                            .addOnSuccessListener(new OnSuccessListener() {
                                @Override
                                public void onSuccess(Object o) {
                                    Log.d(TAG, "Document added");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w(TAG, "Error adding document", e);
                                }
                            });

                    authUser();//authorize the new user to be allowed to log in
                }

            }
        });
    }
    private void authUser() {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success
                            mAuth = FirebaseAuth.getInstance();
                            Log.d(TAG, "createUserWithEmail:success");
                            Intent intent = new Intent(getApplicationContext(),LogActivity.class);
                            startActivity(intent); //log in
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(SignupActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}