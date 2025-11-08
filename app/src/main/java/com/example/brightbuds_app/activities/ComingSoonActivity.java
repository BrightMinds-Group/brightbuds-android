package com.example.brightbuds_app.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brightbuds_app.R;
import com.google.android.material.textview.MaterialTextView;

public class ComingSoonActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coming_soon);

        String title = getIntent().getStringExtra("moduleTitle");
        MaterialTextView tv = findViewById(R.id.txtComingSoon);
        tv.setText(title != null ? (title + " is coming soon!") : "Module coming soon!");
    }
}
