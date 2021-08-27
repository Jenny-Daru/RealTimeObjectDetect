package com.android.jenny.realtimeobjectdetect

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var cameraButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraButton = findViewById(R.id.cameraButton)
        cameraButton.setOnClickListener {
            Log.e(TAG, "CameraButton click")
            startActivity(
                Intent(
                    this@MainActivity,
                    CameraActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
    }
}