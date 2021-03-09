package com.example.WhoseFace.tflite;

import android.util.Log;

class Logger {


    private static final String TAG = "Logger";
    //todo create setter for is Debug
    public static boolean isDebug = true;

    public static void w(String line) {
        if(isDebug){
            Log.i (TAG, line);
        }
    }

    public static void i(String s) {
        if(isDebug){
            Log.i (TAG, s);
        }
    }
}
