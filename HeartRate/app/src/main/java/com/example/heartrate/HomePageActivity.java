package com.example.heartrate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class HomePageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        Button forwardButton = findViewById(R.id.forwardbutton);
        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomePageActivity.this, MainActivity.class));
               /* onBackPressed(); // This will simulate the back button press and return to the previous activity*/
            }
        });

        // Add more code here to set up your home page UI and functionality
    }
}

