package com.phaseshiftlab.ocrlib;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.phaseshiftlab.cyrilliscript.eventslib.LocationEvent;
import com.phaseshiftlab.cyrilliscript.eventslib.PermissionEvent;
import com.phaseshiftlab.cyrilliscript.eventslib.PermissionRequestActivity;

import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OcrService extends Service {

    private enum Alphabets {
        BG_CYRILLIC("АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЬЮЯабвгдежзийклмнопрстуфхцчшщъьюя"),
        EN_LATIN("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"),
        DIGITS("1234567890"),
        SYMBOLS("`~!@#$%^&*()_+-={}[]|\\:;\"'<>/?,.");
        private String alphabet;

        Alphabets(String alphabet) {
            this.alphabet = alphabet;
        }

        public String getAlphabet() {
            return alphabet;
        }
    }

    private enum TesseractFiles {
        BG("bul"),
        EN("eng");

        private String language;
        TesseractFiles(String language) {
            this.language = language;
        }

        public String getFileName() {
            return language + ".traineddata";
        }

        public String getLanguage() {
            return language;
        }
    }

    private static Map<String, String> countryCodes;

    static {
        countryCodes = new HashMap<>();
        countryCodes.put("BG", "bul");
        countryCodes.put("US", "eng");
    }

    private final Context context;
    private TessBaseAPI baseAPI;
    private IBinder myBinder = new MyBinder();
    private static final String TAG = "Cyrilliscript";
    private static final String DATA_PATH = Environment
            .getExternalStorageDirectory().toString() + "/TesseractOCR/";

    private static String lang = TesseractFiles.BG.getLanguage();
    private static String langFile = TesseractFiles.BG.getFileName();
    private static String letters = Alphabets.BG_CYRILLIC.getAlphabet();
    private static String digits = Alphabets.DIGITS.getAlphabet();
    private static String symbols = Alphabets.SYMBOLS.getAlphabet();

    private SharedPreferences preferences;

    private AssetManager assetManager;

    public OcrService() {
        context = this;
    }

    public OcrService(Context context) {
        Log.i(TAG, DATA_PATH);
        this.context = context;
        this.assetManager = context.getAssets();
        this.preferences = context.getSharedPreferences(TAG, MODE_PRIVATE);
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

        if (!(new File(DATA_PATH + "tessdata/" + langFile)).exists()) {
            try {
                Log.v(TAG, "Opening .traineddata asset");
                copyTrainedDataFile();
                preferences.edit().putBoolean(lang, true).apply();
                Log.v(TAG, "Copied " + langFile);
            } catch (IOException e) {
                Log.e(TAG, "Was unable to copy " + langFile + " " + e.toString());
            }
        }
    }

    private void copyTrainedDataFile() throws IOException {
        InputStream in = context.getAssets().open("tessdata/" + langFile);
        OutputStream out = new FileOutputStream(new File(DATA_PATH + "tessdata/", langFile));

        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
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
        Log.d(TAG, "Will try to save thresholdImage to file...");
        try {
            Pix rawImage = baseAPI.getThresholdedImage();
            Bitmap thresholdImage = WriteFile.writeBitmap(rawImage);
            String path = Environment.getExternalStorageDirectory().toString();
            OutputStream fOut;
            File file = new File(path, "thresholdImage." + UUID.randomUUID().toString() + ".jpg");
            fOut = new FileOutputStream(file);

            thresholdImage.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
            fOut.close();
            rawImage.recycle();
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    void onPermissionEvent(PermissionEvent event) {
        if (event.getEventType() == PermissionEvent.GRANTED) {
            initialize();
        }
    }

    @Subscribe
    public void onLocationEvent(LocationEvent event) {
        String countryCode = event.getMessage();
        if(countryCode != null) {
            String tesseractFile = countryCodes.get(countryCode);
            boolean supportedCountry = countryCodes.containsKey(countryCode);
            boolean notAlreadyDownloaded = preferences.getBoolean(tesseractFile, false);
            if(supportedCountry && notAlreadyDownloaded) {
                OcrLanguageSupport.downloadTesseractData(tesseractFile);
            }
        }
    }

    private void initialize() {
        try {
            prepareTrainedDataFiles();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        initTesseractAPI();
    }

    private void requestPermissions() {
        Intent dialogIntent = new Intent(this, PermissionRequestActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(dialogIntent);
    }

    public void initRequiredFiles() throws InterruptedException {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            initialize();
        } else {
            requestPermissions();
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
