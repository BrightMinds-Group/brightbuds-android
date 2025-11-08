package com.example.brightbuds_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brightbuds_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.logoImage);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        logo.startAnimation(fadeIn);

        new Handler().postDelayed(() -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            Intent intent;

            if (user != null) {
                // User already logged in → Go straight to role selection
                intent = new Intent(SplashActivity.this, RoleSelectionActivity.class);
            } else {
                // No login yet → Show landing screen with demo modules
                intent = new Intent(SplashActivity.this, LandingActivity.class);
            }

            startActivity(intent);
            finish();

        }, 2500); // 2.5s delay
    }
}
