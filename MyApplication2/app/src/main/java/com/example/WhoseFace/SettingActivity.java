package com.example.WhoseFace;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener{

    View taskBarView = null;

    protected ArrayList<String> recognizedFaceName = null;
    protected ArrayList<String> recognizedTimeStamp = null;
    protected ArrayList<Bitmap> recognizedFace = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_activity);
        recognizedFace = (ArrayList<Bitmap>) getIntent().getSerializableExtra(CameraActivity.RECOGNIZED_FACE);
        recognizedFaceName = (ArrayList<String>) getIntent().getSerializableExtra(CameraActivity.RECOGNIZED_FACE_NAME);
        recognizedTimeStamp = (ArrayList<String>) getIntent().getSerializableExtra(CameraActivity.RECOGNIZED_TIME_STAMP);

        Objects.requireNonNull(this.getSupportActionBar()).setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.navigation_layout);
        getSupportActionBar().setElevation(0);
        taskBarView = getSupportActionBar().getCustomView();
        taskBarView.findViewById(R.id.setting_button).setVisibility(View.GONE);

        findViewById(R.id.face_list).setOnClickListener(this);
        findViewById(R.id.flush_face).setOnClickListener(this);
        findViewById(R.id.about).setOnClickListener(this);
        findViewById(R.id.how_to).setOnClickListener(this);
    }

    @Override
    public void onClick(View v){
        switch (v.getId()) {
            case R.id.face_list:
                //
                Intent intent = new Intent(SettingActivity.this, FaceListActivity.class);
                intent.putExtra(CameraActivity.RECOGNIZED_FACE_NAME, recognizedFaceName);
                intent.putExtra(CameraActivity.RECOGNIZED_FACE, recognizedFace);
                intent.putExtra(CameraActivity.RECOGNIZED_TIME_STAMP, recognizedTimeStamp);
                startActivity(intent);
                break;
            case R.id.flush_face:
                // recognize image
                deleteSavedFaceDialog(this);
                break;
            case R.id.about:
                // show raw test about with BadCode. logo
                startActivity(new Intent(SettingActivity.this, AboutActivity.class));
                break;
            case R.id.how_to:
                // show guidance
                startActivity(new Intent(SettingActivity.this, HowToActivity.class));
                break;
            default:
                Logger.i("Weird Button is clicked");
                break;
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public void deleteSavedFace(){
        File dir = new File(Objects.requireNonNull(this.getExternalFilesDir(null)).toString());
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            assert children != null;
            for (int i = 0; i < children.length; i++)
            {
                new File(dir, children[i]).delete();
            }
        }
    }

    private void deleteSavedFaceDialog(final Context context) {
        final AlertDialog.Builder exitDialog = new AlertDialog.Builder(context, 0);
        exitDialog.setTitle(getResources().getString(R.string.delete_face))
                .setMessage(getResources().getString(R.string.delete_msg1))
                .setPositiveButton(R.string.str_nope, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // delete
                        deleteSavedFace();
                    }
                }).setCancelable(false)
                .setNegativeButton(R.string.aigth, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // finish him !!!
                    }
                });
        exitDialog.create();
        exitDialog.show();
    }
}
