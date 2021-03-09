package com.example.WhoseFace;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public class WelcomeActivity extends AppCompatActivity {

    private boolean checkHasSavedFace = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                    Intent intent = new Intent(WelcomeActivity.this, CameraActivity.class);
                    startActivity(intent);
                    finish();

            }
        }, 1500);
    }
}
