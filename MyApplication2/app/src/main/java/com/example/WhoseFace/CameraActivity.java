package com.example.WhoseFace;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.example.WhoseFace.utils.ImageUtils;
import com.example.WhoseFace.utils.WriteReadUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class CameraActivity extends FaceDetectActivity implements Camera.PreviewCallback, View.OnClickListener, FaceDetectActivity.cameraListener {

    private static final String TAG = "CameraActivity";
    public static final String RECOGNIZED_FACE_NAME = "face-list-name";
    public static final String RECOGNIZED_FACE = "face-list";
    public static final String RECOGNIZED_TIME_STAMP = "face-time-stamp";


    public static int CAMERA_PERMISSION_REQUEST_CODE = 100;



    private Camera mCamera;
    private CameraPreview mPreview;
    public static final int TF_OD_API_INPUT_SIZE = 112;
    private Matrix originaFrameToCrop;
    private boolean isProcessingImage = false;
    private Runnable imageConverter = null;
    private int[] mRGBBytes;
    private FaceDetector faceDetector;
//    private Runnable postInferenceCallback;

    private boolean firstTimeRunning = true;


    private Handler handler;

    private Button captureButton;
    private Button recognizeButton;
    private boolean hasFaceSaved = false;

    View taskBarView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);

        Objects.requireNonNull(this.getSupportActionBar()).setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.navigation_layout);
        getSupportActionBar().setElevation(0);
        taskBarView = getSupportActionBar().getCustomView();

        handler = new Handler(Looper.getMainLooper());
        captureButton = findViewById(R.id.capture);
        recognizeButton = findViewById(R.id.recognize);
        captureButton.setOnClickListener(this);
        recognizeButton.setOnClickListener(this);
        taskBarView.findViewById(R.id.setting_button).setOnClickListener(this);

        // Real-time contour detection of multiple faces
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();

        FaceDetector detector = FaceDetection.getClient(options);

        faceDetector = detector;

        // initiate essential for training, man now the code is a mess
        faceBitmap = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.ARGB_8888);
        initiateDetector();

        // ask permission and initiate whole in this code, i'm to lazy dude
        if (!checkAndAskCameraPermission(this, this)){
            // do nothing already covered in the if logic method
        } else {
            initializeCamera();
        }
    }

    @Override
    public void onClick(View v){
        switch (v.getId()) {
            case R.id.capture:
                recognize = false;
                add = true;
                captureCamera(mCamera);
                break;
            case R.id.recognize:
                // recognize image
                recognize = true;
                add = false;
                captureCamera(mCamera);
                break;
            case R.id.setting_button:
                // open setting button activity
                Intent intent = new Intent(CameraActivity.this, SettingActivity.class);
                intent.putExtra(RECOGNIZED_FACE_NAME, recognizedFaceName);
                intent.putExtra(RECOGNIZED_FACE, recognizedFace);
                intent.putExtra(RECOGNIZED_TIME_STAMP, recognizedTimeStamp);
                startActivity(intent);
            default:
                recognize = false;
                add = false;
                Log.e(TAG, "weird button clicked");
                break;
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        //TODO release camera timing please fix
        releaseCamera();
    }

    /** Callback for android.hardware.Camera API */
    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {
        if(isProcessingImage){
            return;
        }

        int rotation = 90;

        if (rgbFrameBitmap == null) {
            Camera.Size previewSize = camera.getParameters().getPreviewSize();
            previewHeight = previewSize.height;
            previewWidth = previewSize.width;
            cropH = previewWidth/2;
            cropW = previewHeight/2;
            initBitmaps(new Size(previewWidth, previewHeight), rotation);
            previewToCropMatric =
                    ImageUtils.getTransformationMatrix(
                            previewWidth, previewHeight,
                            previewHeight/2, previewWidth/2,
                            90, false);

            cropToFrameTransform = new Matrix();
            previewToCropMatric.invert(cropToFrameTransform);
        }

        processImage(bytes, previewWidth, previewHeight);
    }

    private void processImage(final byte[] previewBytes, final int previewWidth, final int previewHeight) {

        handler = new Handler();

        final Runnable r = new Runnable() {
            public void run() {
                // processing the image
                isProcessingImage = true;
                int [] convertedImage = new int[previewWidth * previewHeight];
                ImageUtils.convertYUV420SPToARGB8888(previewBytes, previewWidth, previewHeight, convertedImage);
                rgbFrameBitmap.setPixels(convertedImage, 0, previewWidth, 0, 0, previewWidth, previewHeight);

                final Canvas canvas = new Canvas(cropBitmap);
                canvas.drawBitmap(rgbFrameBitmap, previewToCropMatric, null);
                findFace(cropBitmap, null, null);

            }
        };

        handler.postDelayed(r, 200);
    }

    private void findFace (Bitmap croppedFrame, final String filename, final Handler trainingHandler) {
        InputImage inputImage = InputImage.fromBitmap(croppedFrame,0);

        faceDetector.process(inputImage).addOnSuccessListener(new OnSuccessListener<List<Face>>() {
            @Override
            public void onSuccess(List<Face> faces) {
//                        Toast.makeText(CameraActivity.this, "Aye!!! a face boss!!" + faces.size(), Toast.LENGTH_LONG).show();
                processAddObtainFaceImage(faces, mCamera, filename, trainingHandler);
                isProcessingImage = false;
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(CameraActivity.this, "found no face boss!!", Toast.LENGTH_LONG).show();
                isProcessingImage = false;
            }
        });
    }

    public void getTrainCounter() {
        counterPath = WriteReadUtils.getPathContent(this);
        trainCounter = 0;
        trainingHandler.sendEmptyMessage(EXTRACT_FACE_FROM_STORAGE);
    }

    public void trainModel (int pos) {
        String[] fileNames = Objects.requireNonNull(counterPath.list());
        if(pos >= fileNames.length){
            // finish training
            trainingHandler.sendEmptyMessage(FINISH_TRAINING);
            return;
        }
        trainBitmap = BitmapFactory.decodeFile(counterPath.getPath()+"/"+ fileNames[pos]);
        // train model using cropped images
        if (cropW == 0 || cropH == 0){
            cropW = trainBitmap.getWidth();
            cropH = trainBitmap.getHeight();
        }
        String getFaceName = fileNames[pos].split("name:")[1];
        getFaceName = getFaceName.split("number")[0];
        findFace(trainBitmap, getFaceName, trainingHandler);
        trainCounter++;
    }

    public static int EXTRACT_FACE_FROM_STORAGE = 99;
    public static int GET_COUNTER = 98;
    public static int FINISH_TRAINING = 101;
    private Handler trainingHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == GET_COUNTER) {
                //I thought the sendMessage invoked inside the Thread
                //would go here, where I can process the bundle and
                //set some data to a View element and not only Thread change
                getTrainCounter();
                add = true;
            }
            if (msg.what == EXTRACT_FACE_FROM_STORAGE) {
                //I thought the sendMessage invoked inside the Thread
                //would go here, where I can process the bundle and
                //set some data to a View element and not only Thread change
                trainModel(trainCounter);
            }
            if (msg.what == FINISH_TRAINING) {
                //I thought the sendMessage invoked inside the Thread
                //would go here, where I can process the bundle and
                //set some data to a View element and not only Thread change
                add = false;
                findViewById(R.id.loading_ui).setVisibility(View.GONE);
                findViewById(R.id.camera_layout).setVisibility(View.VISIBLE);
                Objects.requireNonNull(getSupportActionBar()).show();
            }
        }
    };


    /**
     *
     * @param size size is original source
     * @param rotation rotation is 90 degree, pretty much useless now
     */

    private void initBitmaps(Size size, int rotation) {

        mRGBBytes = new int[size.getWidth() * size.getHeight()];

        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        cropBitmap = Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888);
        portraitBitmap = Bitmap.createBitmap(size.getHeight(), size.getWidth(), Bitmap.Config.ARGB_8888);

        originaFrameToCrop = ImageUtils.getTransformationMatrix(size.getWidth(), size.getHeight(),
                size.getWidth()/2, size.getHeight()/2, rotation, true);

    }


    private boolean checkAndAskCameraPermission(Context context, Activity activity) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(activity,
                    new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Show dialog permission denied
     * @param context, for getting dialog instance
     */
    private void dialogExit(final Context context, final Activity activity) {
        AlertDialog.Builder exitDialog = new AlertDialog.Builder(context, 0);
        exitDialog.setTitle(R.string.permission_denied_title)
                .setMessage(R.string.permission_denied_message1)
                .setPositiveButton(R.string.str_permission_retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        checkAndAskCameraPermission(context, activity);
                    }
                }).setCancelable(false)
                .setNegativeButton(R.string.str_nope, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // finish him !!!
                        finish();
                    }
                });
        exitDialog.create();
        exitDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!WriteReadUtils.isFaceDirectoryEmpty(this) && firstTimeRunning){
            // train
            findViewById(R.id.loading_ui).setVisibility(View.VISIBLE);
            findViewById(R.id.camera_layout).setVisibility(View.GONE);
            Objects.requireNonNull(getSupportActionBar()).hide();
            trainingHandler.sendEmptyMessage(GET_COUNTER);
        }else {
            //start Camera
            findViewById(R.id.loading_ui).setVisibility(View.GONE);
            findViewById(R.id.camera_layout).setVisibility(View.VISIBLE);
            Objects.requireNonNull(getSupportActionBar()).show();
        }
        firstTimeRunning = false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];
                if (permission.equals(Manifest.permission.CAMERA)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        // continue the app
                        initializeCamera();
                    } else {
                        // show warning
                        dialogExit(this, this);
                    }
                }
            }
        }
    }

    // initialize camera
    private void initializeCamera() {
        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
    }

    //checking camera permission
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    /** A basic Camera preview class */
    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.d("Camera Fail", "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                settingCameraParameter(mCamera, parameters, mHolder);
//                processCameraFrame(mCamera, CameraActivity.this);
                startPreviewCamera(mCamera);

            } catch (Exception e){
                Log.d("Camera Fail", "Error starting camera preview: " + e.getMessage());
            }
        }
    }

    private void captureCamera (Camera camera) {
//        camera.takePicture(null, null, mPicture);
        camera.setOneShotPreviewCallback(this);
    }

    public void startPreviewCamera(Camera mCamera){
        mCamera.startPreview();
    }

    private void settingCameraParameter (Camera camera, Camera.Parameters parameters, SurfaceHolder mHolder) {
        Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

        if(display.getRotation() == Surface.ROTATION_0) {
//                    parameters.setPreviewSize(mPreview.getHeight(), mPreview.getWidth());
            mCamera.setDisplayOrientation(90);
        }

        if(display.getRotation() == Surface.ROTATION_90) {
//                    parameters.setPreviewSize(mPreview.getHeight(), mPreview.getWidth());
        }

        if(display.getRotation() == Surface.ROTATION_180) {
//                    parameters.setPreviewSize(mPreview.getHeight(), mPreview.getWidth());
        }

        if(display.getRotation() == Surface.ROTATION_270) {
//                    parameters.setPreviewSize(mPreview.getHeight(), mPreview.getWidth());
            mCamera.setDisplayOrientation(180);
        }

        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            parameters.setPreviewFormat(ImageFormat.RGB_565);
        }

        mCamera.setParameters(parameters);
        try {
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            if (rgbFrameBitmap == null) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                initBitmaps(new Size(previewWidth, previewHeight), 90);
            }

            processImage(data, previewWidth, previewHeight);
        }
    };




}
