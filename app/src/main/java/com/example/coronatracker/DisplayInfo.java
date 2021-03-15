package com.example.coronatracker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DisplayInfo extends AppCompatActivity {

    private TextView nameTextView;
    private TextView emailTextView;
    private TextView phoneTextView;
    private TextView passwordTextView;
    private TextView coronaTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_info);

        Intent intent =getIntent();

        nameTextView = findViewById(R.id.nameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        passwordTextView = findViewById(R.id.passwordTextView);
        phoneTextView = findViewById(R.id.phoneTextView);
        coronaTextView = findViewById(R.id.coronaTextView);


        nameTextView.setText(intent.getStringExtra("name"));
        emailTextView.setText(intent.getStringExtra("email"));
        passwordTextView.setText(intent.getStringExtra("password"));
        phoneTextView.setText(intent.getStringExtra("phone"));
        boolean corona = intent.getBooleanExtra("corona",false);
        if(corona)
            coronaTextView.setText("Infected");
        else
            coronaTextView.setText("Not Infected");

    }
}