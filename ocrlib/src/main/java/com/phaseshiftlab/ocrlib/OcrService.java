package com.phaseshiftlab.ocrlib;

import android.Manifest;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Debug;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.WriteFile;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.phaseshiftlab.cyrilliscript.eventslib.LanguageChangeEvent;
import com.phaseshiftlab.cyrilliscript.eventslib.PermissionEvent;
import com.phaseshiftlab.languagelib.StatisticsProvider;

import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OcrService extends Service {

    private final int mId = 1001;

    private enum Alphabets {
        BG_CYRILLIC("АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЬЮЯабвгдежзийклмнопрстуфхцчшщъьюя"),
        EN_LATIN("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"),
        DIGITS("1234567890"),
        SYMBOLS("`~!@#$%^&*()_+-={}[]|\\:;\"'<>/?,.");
        private final String alphabet;

        Alphabets(String alphabet) {
            this.alphabet = alphabet;
        }

        public String getAlphabet() {
            return alphabet;
        }
    }

    //TODO move this to OcrFileUtils.java
    private enum TesseractFiles {
        BG("bul"),
        EN("eng");

        private final String language;

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

    private static final Map<String, String> countryCodes;

    static {
        countryCodes = new HashMap<>();
        countryCodes.put("BG", "bul");
        countryCodes.put("US", "eng");
    }

    private final Context context;
    private TessBaseAPI baseAPI;
    private final IBinder myBinder = new MyBinder();
    private static final String TAG = "Cyrilliscript";
    private static final String DATA_PATH = OcrFileUtils.getDataPath();

    private static String getLang() {
        return lang;
    }

    private static void setLang(String lang) {
        OcrService.lang = lang;
    }

    private static String getLangFile() {
        return langFile;
    }

    private static void setLangFile(String langFile) {
        OcrService.langFile = langFile;
    }

    private static String getLetters() {
        return letters;
    }

    private static void setLetters(String letters) {
        OcrService.letters = letters;
    }

    private static String lang;
    private static String langFile;

    private static String letters;
    private static final String digits = Alphabets.DIGITS.getAlphabet();
    private static final String symbols = Alphabets.SYMBOLS.getAlphabet();

    private String tesseractFilePrefix;
    private SharedPreferences preferences;


    public OcrService() {
        context = this;
    }

    public OcrService(Context context) {
        this.context = context;
    }

    public class MyBinder extends Binder {
        public OcrService getService() {
            return OcrService.this;
        }
    }


    @Override
    public void onCreate() {
        Log.d(TAG, "Ocr Service onCreate");
        this.preferences = context.getSharedPreferences(TAG, MODE_PRIVATE);
        setLang(TesseractFiles.BG.getLanguage());
        setLangFile(TesseractFiles.BG.getFileName());
        setLetters(Alphabets.BG_CYRILLIC.getAlphabet());
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Ocr Service onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        initRequiredFiles();
        return myBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    public String requestOCR(Bitmap bitmap) {
        baseAPI.setImage(bitmap);
        String recognized = baseAPI.getUTF8Text();
        int meanConfidence = baseAPI.meanConfidence();
        Log.d(TAG, "mean confidence: " + String.valueOf(meanConfidence));
        //statisticsDb.insertMeanConfidenceDataPoint(meanConfidence);
        ContentValues cv = new ContentValues();
        cv.put("mean_confidence", meanConfidence);
        getContentResolver().insert(StatisticsProvider.CONTENT_URI, cv);
        //dumps the thresholdImage when debugger is attached
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            saveThresholdImage();
        }
        return recognized;
    }

    private void saveThresholdImage() {
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
    public void onPermissionEvent(PermissionEvent event) {
        if (event.getEventType() == PermissionEvent.GRANTED) {
            initialize();
        }
    }

    @Subscribe
    public void onLanguageChangeEvent(LanguageChangeEvent event) {
        if(event.getEventType() == LanguageChangeEvent.BG) {
            setLang(TesseractFiles.BG.getLanguage());
            setLangFile(TesseractFiles.BG.getFileName());
            setLetters(Alphabets.BG_CYRILLIC.getAlphabet());
        } else if(event.getEventType() == LanguageChangeEvent.EN) {
            setLang(TesseractFiles.EN.getLanguage());
            setLangFile(TesseractFiles.EN.getFileName());
            setLetters(Alphabets.EN_LATIN.getAlphabet());
        }
        baseAPI.end();
        initTesseractAPI();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "Intent received");
        OcrLanguageSupport.downloadTesseractData(context, tesseractFilePrefix);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(mId);
        return START_STICKY;
    }

    private void initialize() {
        OcrFileUtils.prepareTrainedDataFiles(context, getLangFile(), getLang());
        initTesseractAPI();
    }

    private void requestPermissions() {
        Intent dialogIntent = new Intent(this, PermissionRequestActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(dialogIntent);
    }

    private void initRequiredFiles() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            initialize();
        } else {
            requestPermissions();
        }
    }

    public void setLettersWhitelist() {
        baseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, getLetters());
        baseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, digits + symbols);
    }

    public void setDigitsWhitelist() {
        baseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, digits);
        baseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, getLetters() + symbols);
    }

    public void setSymbolsWhitelist() {
        baseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, symbols);
        baseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, getLetters() + digits);
    }


    private void initTesseractAPI() {
        baseAPI = new TessBaseAPI();
        baseAPI.setDebug(true);
        baseAPI.init(DATA_PATH, getLang());
        baseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_WORD);
        setLettersWhitelist();
    }
}
