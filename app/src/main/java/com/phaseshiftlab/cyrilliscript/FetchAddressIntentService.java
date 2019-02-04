package com.phaseshiftlab.cyrilliscript;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FetchAddressIntentService extends IntentService {
    private final String TAG = "Cyrilliscript";
    private ResultReceiver mReceiver;

    public FetchAddressIntentService() {
        super("FetchAddressIntentService");
    }
    public FetchAddressIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Geocoder geocoder = new Geocoder(this);
        String errorMessage = "";

        // Get the location passed to this service through an extra.
        Location location = null;
        if (intent != null) {
            location = intent.getParcelableExtra(
                    Constants.LOCATION_DATA_EXTRA);
            mReceiver = intent.getParcelableExtra(Constants.RECEIVER);
        }

        List<Address> addresses = null;

        try {
            if (location != null) {
                addresses = geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        1);
            }
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            errorMessage = getString(R.string.service_not_available);
            Log.e(TAG, errorMessage, ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = getString(R.string.invalid_lat_long_used);
            Log.e(TAG, errorMessage + ". " +
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " +
                    location.getLongitude(), illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_address_found);
                Log.e(TAG, errorMessage);
            }
            HashMap<String, String> errorMessageMap = new HashMap<>();
            errorMessageMap.put(Constants.FAILURE, errorMessage);
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessageMap);
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<>();

            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for(int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }

            String fullAddress = TextUtils.join(System.getProperty("line.separator"),
                    addressFragments);
            String countryCode = address.getCountryCode();
            HashMap<String, String> addressResult = new HashMap<>();
            addressResult.put(Constants.FULL_ADDRESS, fullAddress);
            addressResult.put(Constants.COUNTRY_CODE, countryCode);
            Log.i(TAG, getString(R.string.address_found));
            deliverResultToReceiver(Constants.SUCCESS_RESULT, addressResult);
        }
    }

    @SuppressLint("RestrictedApi")
    private void deliverResultToReceiver(int resultCode, HashMap<String, String> message) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constants.RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }

    final class Constants {
        static final int SUCCESS_RESULT = 0;
        static final int FAILURE_RESULT = 1;
        static final String PACKAGE_NAME =
                "com.phaseshiftlab.cyrilliscript.location.locationaddress";
        static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
        static final String RESULT_DATA_KEY = PACKAGE_NAME +
                ".RESULT_DATA_KEY";
        static final String LOCATION_DATA_EXTRA = PACKAGE_NAME +
                ".LOCATION_DATA_EXTRA";
        static final String FULL_ADDRESS = "Full.Address";
        public static final String COUNTRY_CODE = "Country.Code";
        public static final String FAILURE = "Failure";
    }
}
