package com.example.brightbuds

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val matchLetterIcon: ImageView = findViewById(R.id.matchLetterIcon)
        matchLetterIcon.setOnClickListener {
            // In Kotlin, you create a new object without the 'new' keyword.
            val intent = Intent(this, MatchLetterActivity::class.java)
            startActivity(intent)
        }
    }
}
