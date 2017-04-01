package com.phaseshiftlab.ocrlib;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Debug;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.WriteFile;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class OcrService extends Service implements ActivityCompat.OnRequestPermissionsResultCallback {
    private final Context context;
    private TessBaseAPI baseAPI;
    private IBinder myBinder = new MyBinder();
    private static final int PERM_REQUEST_LOCATION_NOTIFICATION = 99;
    private static final String TAG = "Cyrilliscript";
    private static final String DATA_PATH = Environment
            .getExternalStorageDirectory().toString() + "/TesseractOCR/";

    private static final String lang = "bul";
    private static final String letters = "АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЬЮЯабвгдежзийклмнопрстуфхцчшщъьюя";
    private static final String digits = "1234567890";
    private static final String symbols = "`~!@#$%^&*()_+-={}[]|\\:;\"'<>/?,.";
    private AssetManager assetManager;

    public OcrService() {
        context = this;
    }

    public OcrService(Context context) {
        Log.i(TAG, DATA_PATH);
        this.context = context;
        this.assetManager = context.getAssets();
    }

    public class MyBinder extends Binder {
        public OcrService getService() {
            return OcrService.this;
        }
    }

    private void prepareTrainedDataFiles() throws InterruptedException {
        String[] paths = new String[]{DATA_PATH, DATA_PATH + "/tessdata"};

        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
                    return;
                } else {
                    Log.v(TAG, "Created directory " + path + " on sdcard");
                }
            }
        }

        if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
            try {
                Log.v(TAG, "Opening .traineddata asset");
                InputStream in = context.getAssets().open("tessdata/" + lang + ".traineddata");
                OutputStream out = new FileOutputStream(new File(DATA_PATH + "tessdata/", lang + ".traineddata"));

                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();

                Log.v(TAG, "Copied " + lang + " traineddata");
            } catch (IOException e) {
                Log.e(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "OcrService onCreate");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "OcrService onBind");
        try {
            initRequiredFiles();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return myBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    public String requestOCR(Bitmap bitmap) {
        baseAPI.setImage(bitmap);
        String recognized = baseAPI.getUTF8Text();

        //dumps the thresholdImage when debugger is attached
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            saveThresholdImage();
        }
        return recognized;
    }

    public void saveThresholdImage() {
        try {
            Pix rawImage = baseAPI.getThresholdedImage();
            Bitmap thresholdImage = WriteFile.writeBitmap(rawImage);
            String path = Environment.getExternalStorageDirectory().toString();
            OutputStream fOut;
            File file = new File(path, "thresholdImage."+ UUID.randomUUID().toString() +".jpg");
            fOut = new FileOutputStream(file);

            thresholdImage.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
            fOut.close();
            rawImage.recycle();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean isGranted = false;
        for (int i = 0; i < grantResults.length; i++)
            if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && (grantResults[i] == PackageManager.PERMISSION_GRANTED))
                isGranted = true;
        if (isGranted) {
            try {
                prepareTrainedDataFiles();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            initTesseractAPI();
        } else {
            Log.d(TAG, "WRITE_EXTERNAL_STORAGE permission not granted.");
        }
    }


    public void initRequiredFiles() throws InterruptedException {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            prepareTrainedDataFiles();
            initTesseractAPI();
        } else {
            Log.d(TAG, "Requesting permission to store trained data...");
            PermissionRequester.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERM_REQUEST_LOCATION_NOTIFICATION,
                    context.getString(R.string.notify_perm_title),
                    context.getString(R.string.notify_perm_body),
                    android.R.drawable.ic_secure);
        }
    }

    public void setLettersWhitelist() {
        baseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, letters);
        baseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, digits + symbols);
    }

    public void setDigitsWhitelist() {
        baseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, digits);
        baseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, letters + symbols);
    }

    public void setSymbolsWhitelist() {
        baseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, symbols);
        baseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, letters + digits);
    }


    private void initTesseractAPI() {
        baseAPI = new TessBaseAPI();
        baseAPI.setDebug(true);
        baseAPI.init(DATA_PATH, lang);
        baseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_WORD);
        setLettersWhitelist();
    }
}
