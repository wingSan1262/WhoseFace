package com.example.WhoseFace;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.WhoseFace.tflite.SimilarityClassifier;
import com.example.WhoseFace.tflite.TFLiteObjectDetectionAPIModel;
import com.example.WhoseFace.utils.WriteReadUtils;
import com.google.mlkit.vision.face.Face;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.example.WhoseFace.CameraActivity.EXTRACT_FACE_FROM_STORAGE;
import static com.example.WhoseFace.CameraActivity.TF_OD_API_INPUT_SIZE;
import static com.example.WhoseFace.utils.ImageUtils.createTransform;

public class FaceDetectActivity extends AppCompatActivity {



    protected int previewHeight;
    protected int previewWidth;

    protected int cropH;
    protected int cropW;


    Bitmap rgbFrameBitmap = null;
    Bitmap cropBitmap = null;
    Bitmap portraitBitmap = null;
    Bitmap faceBitmap = null;

    Bitmap trainBitmap = null;

    private static final String TF_OD_API_MODEL_FILE = "mobile_face_net.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";

    cameraListener mCameralistener;

    Matrix previewToCropMatric = null;
    Matrix cropToFrameTransform = null;

    protected boolean recognize = false;

    private SimilarityClassifier detector;
    protected boolean add = false;

    protected File counterPath = null;
    protected int trainCounter = 0;

    protected ArrayList<String> recognizedFaceName = null;
    protected ArrayList<Bitmap> recognizedFace = null;
    protected ArrayList<String> recognizedTimeStamp = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCameralistener = (cameraListener) this;
        recognizedFace = new ArrayList<>();
        recognizedFaceName = new ArrayList<>();
        recognizedTimeStamp = new ArrayList<>();
    }

    public void initiateDetector() {
        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            false);
            //cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            Logger.i("Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
    }

    public void registerDetectedImage(String name, SimilarityClassifier.Recognition rec){
        // with dialog
        // register detected face
        detector.register(name, rec);
    }

    public void processAddObtainFaceImage(List<Face> faces, Camera camera, String fileName, Handler trainingHandler){
        // create rotate matrix
        // Note this can be done only once
        if(faces.size() == 0 || cropW == 0 || cropH == 0){
            return;
        }
        Matrix transform = createTransform(
                cropW,
                cropH,
                cropW,
                cropH,
                0);

        // create a original bmp object -> then flip to portrait
        // get the face size
        RectF boundingBox = new RectF(faces.get(0).getBoundingBox());

        final Canvas cvFace = new Canvas(faceBitmap);
        float sx = ((float) TF_OD_API_INPUT_SIZE) / boundingBox.width();
        float sy = ((float) TF_OD_API_INPUT_SIZE) / boundingBox.height();
        transform.postTranslate(-boundingBox.left, -boundingBox.top);
        transform.postScale(sx, sy);

        if(fileName == null){
            cvFace.drawBitmap(cropBitmap, transform, null);
        } else {
            cvFace.drawBitmap(trainBitmap, transform, null);
        }

        final List<SimilarityClassifier.Recognition> resultsAux = detector.recognizeImage(faceBitmap, add);
        // create face recognition profiles for addition
        final SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
                "0", "", resultsAux.get(0).getDistance(), boundingBox);
        result.setColor(Color.BLUE);
        result.setLocation(boundingBox);
        result.setCrop(faceBitmap);
        if(resultsAux.get(0).getExtra() == null){
            Toast.makeText(this, resultsAux.get(0).getTitle() + "= name, distance = " + result.getDistance(), Toast.LENGTH_LONG).show();
            showDialogRecognizeResult(faceBitmap, camera, resultsAux.get(0));
            return;
        }
        Toast.makeText(this, result.getTitle() + "extra not null", Toast.LENGTH_LONG).show();
        result.setExtra(resultsAux.get(0).getExtra());

        if(recognize){
            // recognize images
            Toast.makeText(this, result.toString() + "= name, distance = " + result.getDistance(), Toast.LENGTH_LONG).show();
        } else {
            if (fileName == null){
                showDialogImageConfirming(faceBitmap, camera, result, this);
            }else {
                Date currentTime = Calendar.getInstance().getTime();
                recognizedFaceName.add(fileName);
                recognizedFace.add(Bitmap.createBitmap(faceBitmap));
                recognizedTimeStamp.add(currentTime.toString());
                registerDetectedImage(fileName, result);
                trainingHandler.sendEmptyMessage(EXTRACT_FACE_FROM_STORAGE);
            }
        }
    }

    public void showDialogImageConfirming (Bitmap image, final Camera camera, final SimilarityClassifier.Recognition result, final Context context){
        AlertDialog.Builder alertadd = new AlertDialog.Builder(this);
        LayoutInflater factory = LayoutInflater.from(this);
        final View view = factory.inflate(R.layout.dialog_face_confirm, null);
        alertadd.setView(view);
        ImageView imageView = view.findViewById(R.id.dialog_imageview);
        final EditText editText = view.findViewById(R.id.dlg_input);
        imageView.setImageBitmap(image);
        alertadd.setNeutralButton(getString(R.string.register_here_button), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int sumthin) {
                String name = editText.getText().toString();
                // write files
                WriteReadUtils.writeFiles(cropBitmap, context, name);
                // register
                registerDetectedImage(name, result);
                Date currentTime = Calendar.getInstance().getTime();
                recognizedFaceName.add(name);
                recognizedFace.add(Bitmap.createBitmap(faceBitmap));
                recognizedTimeStamp.add(currentTime.toString());
                mCameralistener.startPreviewCamera(camera);
            }
        });

        alertadd.show();
    }

    public void showDialogRecognizeResult (Bitmap image, final Camera camera, final SimilarityClassifier.Recognition result){
        AlertDialog.Builder alertadd = new AlertDialog.Builder(this);
        LayoutInflater factory = LayoutInflater.from(this);
        final View view = factory.inflate(R.layout.dialog_face_confirm, null);
        alertadd.setView(view);
        ImageView imageView = view.findViewById(R.id.dialog_imageview);
        final EditText editText = view.findViewById(R.id.dlg_input);
        editText.setVisibility(View.GONE);
        imageView.setImageBitmap(image);
        TextView mTv = view.findViewById(R.id.msg_name_tag);
        StringBuilder stringBuilder = new StringBuilder().append(getString(R.string.name_title)).append(result.getTitle())
                .append(getString(R.string.distance_tag)).append(result.getDistance());
        if(result.getDistance()>0.8f){
            stringBuilder = new StringBuilder().append("Cannot recognize this face boss ...");
            mTv.setText(stringBuilder.toString());
            mTv.setTextColor(getResources().getColor(R.color.colorAccent));

        } else {
            mTv.setTextColor(getResources().getColor(R.color.colorBlue));
            mTv.setText(stringBuilder.toString());
        }
        alertadd.setNeutralButton(getString(R.string.register_here_button) + "/continue", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int sumthin) {
                if(result.getDistance()>0.8f){
//                    showDialogImageConfirming(faceBitmap, camera, result, FaceDetectActivity.this);
                } else {
                    mCameralistener.startPreviewCamera(camera);
                }
            }
        });

        alertadd.show();
    }

    public interface cameraListener {
        // you can define any parameter as per your requirement
        public void startPreviewCamera(Camera camera);
    }

    public void onSingleDetectedface(){

    }


}
