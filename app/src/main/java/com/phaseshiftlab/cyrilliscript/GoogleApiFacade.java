package com.phaseshiftlab.cyrilliscript;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.DateFormat;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.os.ResultReceiver;
import android.util.Log;
import android.util.LogPrinter;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.phaseshiftlab.cyrilliscript.eventslib.LocationEvent;
import com.phaseshiftlab.cyrilliscript.eventslib.PermissionEvent;
import com.phaseshiftlab.cyrilliscript.eventslib.PermissionRequestActivity;
import com.phaseshiftlab.cyrilliscript.FetchAddressIntentService;
import com.phaseshiftlab.cyrilliscript.FetchAddressIntentService.Constants;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Date;
import java.util.HashMap;


public class GoogleApiFacade implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private String TAG = "Cyrilliscript";
    private Tracker mTracker;
    private GoogleApiClient mGoogleApiClient;
    private SoftKeyboard mSoftKeyboard;
    private Location mLocation;
    private AddressResultReceiver mResultReceiver;
    private HashMap<String, String> mAddressOutput;

    private EventBus eventBus = EventBus.getDefault();


    GoogleApiFacade() {
    }

    GoogleApiFacade(SoftKeyboard keyboard) {
        this.mSoftKeyboard = keyboard;
        initGoogleAnalyticsAPI();
        initGoogleLocationAPI();
        if (!eventBus.isRegistered(this)) {
            eventBus.register(this);
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
            requestPermissions();
            return;
        }
        getLocation();
    }

    private void getLocation() {
        try {
            Log.d(TAG, "in getLocation");
            mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(mLocation == null) {
                Log.d(TAG, "getLastLocation returned null, calling requestLocationUpdates...");
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, LocationRequest.create(), this);
            } else {
                String latitude = String.valueOf(mLocation.getLatitude());
                String longitude = String.valueOf(mLocation.getLongitude());
                Log.d(TAG, latitude);
                Log.d(TAG, longitude);
                startIntentService();
            }

        } catch(SecurityException e) {
            requestPermissions();
        }

    }

    protected void startIntentService() {
        mResultReceiver = new AddressResultReceiver(new Handler());
        Intent intent = new Intent(mSoftKeyboard, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLocation);
        mSoftKeyboard.startService(intent);
    }

    @Subscribe
    void onPermissionEvent(PermissionEvent event) {
        if(event.getEventType() == PermissionEvent.GRANTED) {
            getLocation();
        }
    }

    private void requestPermissions() {
        Intent dialogIntent = new Intent(mSoftKeyboard, PermissionRequestActivity.class);
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

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Received location");
        location.dump(new LogPrinter(Log.DEBUG, TAG), "location");
        mLocation = location;
    }

    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string
            // or an error message sent from the intent service.
            mAddressOutput = (HashMap<String, String>) resultData.getSerializable(Constants.RESULT_DATA_KEY);

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                String fullAddress = mAddressOutput.get(Constants.FULL_ADDRESS);
                String countryCode = mAddressOutput.get(Constants.COUNTRY_CODE);
                Log.d(TAG, fullAddress);
                Log.d(TAG + " country", countryCode);
                eventBus.post(new LocationEvent(countryCode));
            }

        }
    }
}
