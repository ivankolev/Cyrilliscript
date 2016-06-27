package com.phaseshiftlab.cyrilliscript;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;

public class TransparentActivity extends Activity {

    Tag myTag;

    NfcAdapter adapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transparent);

        //For the very first time is Launched this activity
        handleIntent(getIntent());

        //For Next time... ForeGround Dispatch Methodology
        adapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[]{tagDetected};
    }

    @SuppressLint("NewApi") protected void handleIntent(Intent intent){
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            byte[] myTagBytes = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            String strTag = byteArrayToHexString(myTagBytes);

            Intent myInt = new Intent(this,SoftKeyboard.class);
            myInt.putExtra("UUID", strTag);
            startService(myInt);

            moveTaskToBack(true);       // Puts moves back this Activity on the process Task.
        }
    }

    //region ForegroundDispatch Methodology
    @SuppressLint("NewApi") protected void onNewIntent(Intent intent){
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            byte[] myTagBytes = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            String strTag = byteArrayToHexString(myTagBytes);

            Intent myInt = new Intent(this,SoftKeyboard.class);
            myInt.putExtra("UUID", strTag);
            startService(myInt);


            moveTaskToBack(true);
        }
    }

    @SuppressLint("NewApi") private void WriteModeOn(){
        adapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }

    @SuppressLint("NewApi") private void WriteModeOff(){
        adapter.disableForegroundDispatch(this);
    }
    //endregion

    //region ByteConversionString
    public static String byteArrayToHexString(byte[] array) {
        StringBuffer hexString = new StringBuffer();
        for (byte b : array) {
            int intVal = b & 0xff;
            if (intVal < 0x10)
                hexString.append("0");
            hexString.append(Integer.toHexString(intVal));
        }
        return hexString.toString();
    }
    //endregion

    public void onPause(){
        super.onPause();
        WriteModeOff();
    }
    public void onResume() {
        super.onResume();
        WriteModeOn();
    }

    //region Methods to Bring Back an Activity on the Task
//    private void moveToBack(View myCurrentView)
//    {
//        ViewGroup myViewGroup = ((ViewGroup) myCurrentView.getParent());
//        int index = myViewGroup.indexOfChild(myCurrentView);
//        for(int i = 0; i<index; i++)
//        {
//            myViewGroup.bringChildToFront(myViewGroup.getChildAt(i));
//        }
//    }
//
//    public static void sendToBack(final View child) {
//        final ViewGroup parent = (ViewGroup)child.getParent();
//        if (null != parent) {
//            parent.removeView(child);
//            parent.addView(child, 0);
//        }
//    }
    //endregion
}
