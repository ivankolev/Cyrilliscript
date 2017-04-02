package com.phaseshiftlab.cyrilliscript;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.RuntimeExecutionException;
import com.phaseshiftlab.cyrilliscript.events.PermissionEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import static android.support.v4.content.ContextCompat.startActivity;


class GoogleApiFacade implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private Tracker mTracker;
    private GoogleApiClient mGoogleApiClient;
    private SoftKeyboard mSoftKeyboard;
    private SettingsActivity mSettingsActivity;

    GoogleApiFacade(SoftKeyboard keyboard) {
        this.mSoftKeyboard = keyboard;
        initGoogleAnalyticsAPI();
        initGoogleLocationAPI();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    void sendAnalyticsEvent(String event) {
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Event")
                .setAction(event)
                .build());
    }

    void connect() {
        mGoogleApiClient.connect();
    }

    void disconnect() {
        mGoogleApiClient.disconnect();
    }

    private synchronized Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(mSoftKeyboard);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(R.xml.global_tracker);
        }
        return mTracker;
    }

    private void initGoogleLocationAPI() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(mSoftKeyboard)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    private void initGoogleAnalyticsAPI() {
        this.getDefaultTracker();
        mTracker.setAnonymizeIp(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(mSoftKeyboard, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mSoftKeyboard, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermisions();
            return;
        }
        getLocation();
    }

    private void getLocation() {
        try {
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            String latitude = String.valueOf(mLastLocation.getLatitude());
            String longitude = String.valueOf(mLastLocation.getLongitude());
        } catch(SecurityException e) {
            requestPermisions();
        }

    }

    @Subscribe
    void onPermissionEvent(PermissionEvent event) {
        if(event.getEventType() == PermissionEvent.GRANTED) {
            getLocation();
        }
    }

    private void requestPermisions() {
        Intent dialogIntent = new Intent(mSoftKeyboard, SettingsActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mSoftKeyboard.startActivity(dialogIntent);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    protected void finalize() throws Throwable {
        super.finalize();
        EventBus.getDefault().unregister(this);
    }
}
