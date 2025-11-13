package com.example.brightbuds_app.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.brightbuds_app.R;
import com.example.brightbuds_app.ui.games.MemoryMatchFragment;

public class MemoryMatchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the activity layout that contains the fragment container
        setContentView(R.layout.activity_memory_match);

        // Load MemoryMatchFragment when activity starts
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.memoryMatchFragmentContainer, new MemoryMatchFragment())
                    .commit();
        }
    }
}
