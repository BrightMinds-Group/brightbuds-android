package com.example.brightbuds_app.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brightbuds_app.R;

public class MemoryMatchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_placeholder);

        TextView title = findViewById(R.id.title);
        String moduleTitle = getIntent().getStringExtra("moduleTitle");
        if (moduleTitle != null) {
            title.setText(moduleTitle + " - Game");
        } else {
            title.setText("Memory Match Game");
        }
    }
}