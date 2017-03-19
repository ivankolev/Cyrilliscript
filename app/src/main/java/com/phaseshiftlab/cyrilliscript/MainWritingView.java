package com.phaseshiftlab.cyrilliscript;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.phaseshiftlab.cyrilliscript.events.RxBus;
import com.phaseshiftlab.ocrlib.OcrService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * TODO: document your custom view class.
 */
public class MainWritingView extends View {

    private static final String TAG = "Cyrilliscript";
    public static String DATA_PATH = null;
    public static final String lang = "bul";

    private Path drawPath;

    //defines what to draw
    private Paint canvasPaint;

    //defines how to draw
    private Paint drawPaint;

    //initial color
    private int paintColor = 0xFF263238;

    //canvas - holding pen, holds your drawings
    //and transfers them to the view
    private Canvas drawCanvas;

    //canvas bitmap
    private Bitmap canvasBitmap;

    private OcrService ocrService;
    boolean isBound = false;

    private RxBus rxBus = RxBus.getInstance();

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            OcrService.MyBinder binder = (OcrService.MyBinder) service;
            ocrService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            ocrService = null;
            isBound = false;
        }
    };

    public MainWritingView(Context context, AttributeSet attrs) throws InterruptedException {
        super(context, attrs);
        if(!this.isInEditMode()) {
            DATA_PATH = Environment
                    .getExternalStorageDirectory().toString() + "/TesseractOCR/";
            bindToService(context);
        }

        rxBus.receive(String.class, s -> {
            if(s.equals("CLEAR")) {
                canvasBitmap.eraseColor(getResources().getColor(R.color.main_writing_view_bg));
            }
        });

        rxBus.receive(Map.class, s -> {
            String clearEvent = (String) s.get("CLEAR");
            Log.d("Cyrilliscript", "CLEAR received " + clearEvent);
            if(clearEvent != null && Objects.equals(clearEvent, "true")) {
                canvasBitmap.eraseColor(getResources().getColor(R.color.main_writing_view_bg));
            }
        });

        drawPath = new Path();
        drawPaint = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(6);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        drawPaint.setPathEffect(new CornerPathEffect(50) );

        canvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
//        parentView = (MainView) this.getParent();
//
//        final Button button = (Button) parentView.findViewById(R.id.clear);
//        button.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                invalidate();
//                // Perform action on click
//            }
//        });
    }

    private void bindToService(Context context) {
        Intent intent = new Intent(this.getContext(), OcrService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public MainWritingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MainWritingView(Context context) {
        super(context);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //create canvas of certain device size.
        super.onSizeChanged(w, h, oldw, oldh);

        //create Bitmap of certain w,h
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        //apply bitmap to graphic to start drawing.
        drawCanvas = new Canvas(canvasBitmap);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(canvasBitmap, 0 , 0, canvasPaint);
        canvas.drawPath(drawPath, drawPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();

        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drawPath.moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                drawPath.lineTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                drawPath.lineTo(touchX, touchY);
                drawCanvas.drawPath(drawPath, drawPaint);
                drawPath.reset();
                requestOcr();
                break;
            default:
                return false;
        }

        // Makes our view repaint and call onDraw
        invalidate();
        return true;
    }

    private void requestOcr() {
        Log.d("Cyrilliscript", "MotionEvent.ACTION_UP");
        if(!this.isInEditMode()) {
            new RequestOcrTask().execute();
        }
    }

    private class RequestOcrTask extends AsyncTask<String, Integer, Map> {

        @Override
        protected Map<String, String> doInBackground(String... params) {
            String recognized = ocrService.requestOCR(canvasBitmap);
            Log.d("Cyrilliscript", recognized);
            Map<String, String> recognizedMap = new HashMap<>();
            recognizedMap.put("RECOGNIZED", recognized);
            return recognizedMap;
        }

        @Override
        protected void onPostExecute(Map result) {
            rxBus.post(result);
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.d("Cyrilliscript", String.valueOf(values.length));
        }
    }
}
