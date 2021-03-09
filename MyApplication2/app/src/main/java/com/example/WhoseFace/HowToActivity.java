package com.example.WhoseFace;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TextView;

import java.io.InputStream;
import java.util.Objects;

public class HowToActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_how_to);
        Objects.requireNonNull(this.getSupportActionBar()).setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().hide();

        TextView textView = findViewById(R.id.how_to_tv);

        try {
            Resources res = getResources();
            InputStream in_s = res.openRawResource(R.raw.how_to);

            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            textView.setText(new String(b));
        } catch (Exception e) {
            // e.printStackTrace();
            textView.setText(getResources().getString(R.string.error_raw_cannot_load));
        }

    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
