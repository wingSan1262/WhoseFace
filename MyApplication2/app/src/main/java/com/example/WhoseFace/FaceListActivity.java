package com.example.WhoseFace;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.os.Bundle;

import java.util.ArrayList;

public class FaceListActivity extends AppCompatActivity {

    protected ArrayList<String> recognizedFaceName = null;
    protected ArrayList<Bitmap> recognizedFace = null;
    protected ArrayList<String> recognizedTimeStamp = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_list);
        recognizedFace = (ArrayList<Bitmap>) getIntent().getSerializableExtra(CameraActivity.RECOGNIZED_FACE);
        recognizedFaceName = (ArrayList<String>) getIntent().getSerializableExtra(CameraActivity.RECOGNIZED_FACE_NAME);
        recognizedTimeStamp = (ArrayList<String>) getIntent().getSerializableExtra(CameraActivity.RECOGNIZED_TIME_STAMP);

        RecyclerView recyclerView = findViewById(R.id.my_face_view_list);
        FaceListAdapter listAdapter = new FaceListAdapter(recognizedFaceName, recognizedFace, recognizedTimeStamp);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        recyclerView.setAdapter(listAdapter);
    }
}
