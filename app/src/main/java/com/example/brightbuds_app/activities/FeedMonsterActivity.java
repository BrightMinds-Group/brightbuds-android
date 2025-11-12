package com.example.brightbuds_app.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.brightbuds_app.R;

/*
 Hosts the FeedTheMonsterFragment inside a FragmentContainerView.
 Activity contains no game logic to keep responsibilities separate.
*/
public class FeedMonsterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_monster);
        setTitle("Feed the Monster");
    }
}
