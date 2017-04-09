package com.phaseshiftlab.ocrlib;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Debug;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat.Builder;
import android.util.Log;

import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.WriteFile;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.phaseshiftlab.cyrilliscript.eventslib.LanguageChangeEvent;
import com.phaseshiftlab.cyrilliscript.eventslib.LocationEvent;
import com.phaseshiftlab.cyrilliscript.eventslib.PermissionEvent;
import com.phaseshiftlab.languagelib.StatisticsDatabaseHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OcrService extends Service {

    private int mId = 1001;

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

    //TODO move this to OcrFileUtils.java
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
    private StatisticsDatabaseHelper statisticsDb;
    private IBinder myBinder = new MyBinder();
    private static final String TAG = "Cyrilliscript";
    private static final String DATA_PATH = OcrFileUtils.getDataPath();

    public static String getLang() {
        return lang;
    }

    public static void setLang(String lang) {
        OcrService.lang = lang;
    }

    public static String getLangFile() {
        return langFile;
    }

    public static void setLangFile(String langFile) {
        OcrService.langFile = langFile;
    }

    public static String getLetters() {
        return letters;
    }

    public static void setLetters(String letters) {
        OcrService.letters = letters;
    }

    private static String lang;
    private static String langFile;

    private static String letters;
    private static String digits = Alphabets.DIGITS.getAlphabet();
    private static String symbols = Alphabets.SYMBOLS.getAlphabet();

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
        statisticsDb = new StatisticsDatabaseHelper(this);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Ocr Service onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
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
        int meanConfidence = baseAPI.meanConfidence();
        Log.d(TAG, "mean confidence: " + String.valueOf(meanConfidence));
        statisticsDb.insertMeanConfidenceDataPoint(meanConfidence);
        Cursor stats = statisticsDb.getTotals();

        Integer i = 0;
        while (!stats.isAfterLast()) {
            Integer totalEventsCount = stats.getInt(0);
            Integer totalAverageConfidence = stats.getInt(1);
            Log.d(TAG, "totalEventsCount: " + totalEventsCount);
            Log.d(TAG, "totalAverageConfidence: " + totalAverageConfidence);
            i++;
            stats.moveToNext();
        }

        stats.close();

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

    @Subscribe
    public void onLocationEvent(LocationEvent event) {
        String countryCode = event.getMessage();
        Log.d(TAG, "received onLocationEvent " + countryCode);
        if (countryCode != null) {
            tesseractFilePrefix = countryCodes.get(countryCode);
            Log.d(TAG, "tesseractFilePrefix is " + tesseractFilePrefix);
            boolean supportedCountry = countryCodes.containsKey(countryCode);
            Log.d(TAG, "supportedCountry is " + supportedCountry);
            boolean notAlreadyDownloaded = !preferences.getBoolean(tesseractFilePrefix, false);
            Log.d(TAG, "notAlreadyDownloaded is " + notAlreadyDownloaded);
            if (supportedCountry && notAlreadyDownloaded) {
                android.support.v4.app.NotificationCompat.Builder mBuilder =
                        new Builder(this)
                                .setSmallIcon(R.drawable.ic_archive_black_24dp)
                                .setContentTitle(getString(R.string.download_file))
                                .setContentText(getString(R.string.suggest_download));
                Intent resultIntent = new Intent(this, OcrService.class);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                stackBuilder.addParentStack(PermissionRequestActivity.class);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent = PendingIntent.getService(context, 0, resultIntent, PendingIntent.FLAG_ONE_SHOT);
                mBuilder.setContentIntent(resultPendingIntent);
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(mId, mBuilder.build());
            }
        }
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
        try {
            OcrFileUtils.prepareTrainedDataFiles(context, getLangFile(), getLang());
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
