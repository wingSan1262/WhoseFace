package com.example.WhoseFace.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class WriteReadUtils {

    public static void writeFiles(Bitmap rgbFrameBitmap, Context context, String fileName){
        File getPath = getPathContent(context);
        int size = Objects.requireNonNull(getPath.list()).length;

        try{
            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "name:" + fileName + "number:" + size);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Objects.requireNonNull(context.getExternalFilesDir(null)).toString());
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));
            } else {
                String imagesDir = Objects.requireNonNull(context.getExternalFilesDir(null)).toString();
                File image = new File(imagesDir, "name:" + fileName + "number:" + size);
                fos = new FileOutputStream(image);
            }
            rgbFrameBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            Objects.requireNonNull(fos).close();
        }catch (IOException e) {
            // Log Message
        }
    }

    public static boolean isFaceDirectoryEmpty(Context context){
        File directory = context.getExternalFilesDir(null);
        assert directory != null;
        File[] contents = directory.listFiles();
        // the directory file is not really a directory..
        // Folder contains empty
        if (contents == null || contents.length == 0) {
            return true;
        }
        // Folder not empty
        return false;
    }

    public static File getPathContent(Context context){
        File path = context.getExternalFilesDir(null);
        return path;
    }
}
