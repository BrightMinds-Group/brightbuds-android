package com.example.brightbuds_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brightbuds_app.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LandingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Skip landing if logged in already
            startActivity(new Intent(this, RoleSelectionActivity.class));
            finish();
            return;
        }

        MaterialButton btnSignIn = findViewById(R.id.btnSignIn);
        MaterialButton btnRegister = findViewById(R.id.btnRegister);
        ImageView imgAbcSong = findViewById(R.id.imgAbcSong);
        ImageView img123Song = findViewById(R.id.img123Song);
        TextView txtFreeModules = findViewById(R.id.txtFreeModules);

        // Launch demo modules
        imgAbcSong.setOnClickListener(v -> {
            Intent intent = new Intent(this, VideoModuleActivity.class);
            intent.putExtra("moduleId", "module_abc_song");
            intent.putExtra("moduleTitle", "ABC Song");
            intent.putExtra("storagePath", "module_abc_song.mp4"); // Firebase Storage path
            intent.putExtra("moduleType", "video");
            intent.putExtra("isDemo", true);
            startActivity(intent);
        });

        img123Song.setOnClickListener(v -> {
            Intent intent = new Intent(this, VideoModuleActivity.class);
            intent.putExtra("moduleId", "module_123_song");
            intent.putExtra("moduleTitle", "123 Song");
            intent.putExtra("storagePath", "module_123_song.mp4"); // Firebase Storage path
            intent.putExtra("moduleType", "video");
            intent.putExtra("isDemo", true);
            startActivity(intent);
        });

        // Parent sign-in and registration buttons
        btnSignIn.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveTaskToBack(true); // Prevents back navigation to splash
    }
}
