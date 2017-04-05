package com.phaseshiftlab.cyrilliscript.eventslib;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.view.View;

import org.greenrobot.eventbus.EventBus;

public class PermissionRequestActivity extends Activity {

    private static final int SOFTKEYBOARD_PERMISSIONS = 359;
    private EventBus eventBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soft_keyboard);
        eventBus = EventBus.getDefault();
        ActivityCompat.requestPermissions(this,
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
                    eventBus.postSticky(new PermissionEvent(PermissionEvent.GRANTED));
                } else {
                    eventBus.postSticky(new PermissionEvent(PermissionEvent.DENIED));
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void launchLanguageAndInputSettings(View view) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        ComponentName com = new ComponentName("com.android.settings", "com.android.settings.LanguageSettings");
        intent.setComponent(com); startActivity(intent);
    }
}
