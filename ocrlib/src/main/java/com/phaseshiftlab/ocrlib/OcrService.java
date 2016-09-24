package com.phaseshiftlab.ocrlib;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;

public class OcrService extends Service {
    public OcrService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public String requestOCR(Bitmap bitmap) {
        return "Width:" + bitmap.getWidth() + " Height:" + bitmap.getHeight();
    }
}
