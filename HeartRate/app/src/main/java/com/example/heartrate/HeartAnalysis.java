package com.example.heartrate;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Locale;

public class HeartAnalysis extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_analysis);

        TextView tvHeartRate = findViewById(R.id.tvHeartRate);

        // Retrieve the heart rate passed from MainActivity
        float heartRate = getIntent().getFloatExtra("HeartRate", 0);
        tvHeartRate.setText(String.format(Locale.getDefault(), "Heart Rate: %.2f bpm", heartRate));
    }
}