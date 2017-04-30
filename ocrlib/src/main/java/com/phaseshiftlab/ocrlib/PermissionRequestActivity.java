package com.phaseshiftlab.ocrlib;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.phaseshiftlab.cyrilliscript.eventslib.DownloadSuccessEvent;
import com.phaseshiftlab.cyrilliscript.eventslib.PermissionEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PermissionRequestActivity extends Activity {

    private static final String TAG = "Cyrilliscript";
    private ListView installedLanguagesList;
    ArrayList<LanguageItem> languageItems;
    private static LanguageItemAdapter languageItemAdapter;
    private static final int SOFTKEYBOARD_PERMISSIONS = 359;
    private EventBus eventBus;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.phaseshiftlab.ocrlib.R.layout.activity_soft_keyboard);
        this.preferences = this.getApplicationContext().getSharedPreferences(TAG, MODE_PRIVATE);
        this.preferences.edit().putBoolean("bul", false).apply();
        this.preferences.edit().putBoolean("eng", false).apply();

        eventBus = EventBus.getDefault();
        eventBus.register(this);

        requestAppPermissions();
        initInstalledLanguagesList();
    }

    private void initInstalledLanguagesList() {
        installedLanguagesList = (ListView) findViewById(com.phaseshiftlab.ocrlib.R.id.installedLanguages);

        languageItems = getLanguageListFromPrefs();

        languageItemAdapter = new LanguageItemAdapter(languageItems, this.getApplicationContext());

        installedLanguagesList.setAdapter(languageItemAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        eventBus.unregister(this);
    }

    private void requestAppPermissions() {
        this.requestPermissions(
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                SOFTKEYBOARD_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case SOFTKEYBOARD_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    eventBus.post(new PermissionEvent(PermissionEvent.GRANTED));
                } else {
                    eventBus.post(new PermissionEvent(PermissionEvent.DENIED));
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDownloadSuccessEvent(DownloadSuccessEvent event) {
        String message = event.getMessage();
        if(message != null && message.equals("SUCCESS")) {
            initInstalledLanguagesList();
        }
    }


    public void launchLanguageAndInputSettings(View view) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        ComponentName com = new ComponentName("com.android.settings", "com.android.settings.LanguageSettings");
        intent.setComponent(com); startActivity(intent);
    }

    public static List<String> getInstalledLanguagesFromPrefs(Context context) {
        Map<String, ?> prefs = context.getSharedPreferences(TAG, MODE_PRIVATE).getAll();

        List<String> installedLanguages = new ArrayList<>();

        for (String key : prefs.keySet()) {
            Object pref = prefs.get(key);
            if (pref instanceof Boolean) {
                String language = "";
                if(key.equals("bul")) {
                    language = "BG";
                } else if(key.equals("eng")) {
                    language = "EN";
                }

                if(!language.equals("")) {
                    Boolean fileExist = OcrFileUtils.tesseractTraineddataFileExist(key + ".traineddata");
                    if(!fileExist && (Boolean) pref ) {
                        context.getSharedPreferences(TAG, MODE_PRIVATE).edit().putBoolean(key, true).apply();
                    } else if(fileExist) {
                        installedLanguages.add(language);
                    }
                }
            }
        }

        return installedLanguages;
    }

    private ArrayList<LanguageItem> getLanguageListFromPrefs() {
        Map<String, ?> prefs = this.getSharedPreferences(TAG, MODE_PRIVATE).getAll();

        ArrayList<LanguageItem> languagesList = new ArrayList<>();

        for (String key : prefs.keySet()) {
            Object pref = prefs.get(key);
            String printVal = "";
            if (pref instanceof Boolean) {
                String language = "";
                if(key.equals("bul")) {
                    language = "Bulgarian";
                } else if(key.equals("eng")) {
                    language = "English";
                }

                if(!language.equals("")) {
                    String isItInstalled = (Boolean) pref ? "installed" : "not installed";
                    Boolean fileExist = OcrFileUtils.tesseractTraineddataFileExist(key + ".traineddata");
                    if(!fileExist && (Boolean) pref ) {
                        this.getSharedPreferences(TAG, MODE_PRIVATE).edit().putBoolean(key, false).apply();
                        isItInstalled = "not installed";

                    } else if(fileExist) {
                        this.getSharedPreferences(TAG, MODE_PRIVATE).edit().putBoolean(key, true).apply();
                        isItInstalled = "installed";
                    }
                    printVal = language + ": " + isItInstalled;
                    languagesList.add(new LanguageItem(key, printVal, fileExist));
                }
            }
            if (pref instanceof Float) {
                printVal =  key + " : " + pref;
            }
            if (pref instanceof Integer) {
                printVal =  key + " : " + pref;
            }
            if (pref instanceof Long) {
                printVal =  key + " : " + pref;
            }
            if (pref instanceof String) {
                printVal =  key + " : " + pref;
            }
            if (pref instanceof Set<?>) {
                printVal =  key + " : " + pref;
            }
            Log.d(TAG, "pref: " + printVal);

            // create a TextView with printVal as text and add to layout
        }

        return languagesList;
    }
}
