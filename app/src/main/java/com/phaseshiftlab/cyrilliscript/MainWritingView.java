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
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.phaseshiftlab.cyrilliscript.eventslib.InputSelectChangedEvent;
import com.phaseshiftlab.cyrilliscript.eventslib.SoftKeyboardEvent;
import com.phaseshiftlab.cyrilliscript.eventslib.WritingViewEvent;
import com.phaseshiftlab.ocrlib.OcrService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayDeque;
import java.util.Objects;

/**
 * TODO: document your custom view class.
 */
public class MainWritingView extends View {

    private static final String TAG = "Cyrilliscript";
    public static String DATA_PATH = null;
    public static final String lang = "bul";

    private Path drawPath;

    //defines how to draw
    private Paint drawPaint;

    //initial color
    private int paintColor = 0xFF263238;

    //canvas - holding pen, holds your drawings
    //and transfers them to the view
    private Canvas drawCanvas;

    //canvas bitmap
    private Bitmap canvasBitmap;

    private ArrayDeque<Path> pathStack = new ArrayDeque<>();

    private OcrService ocrService;
    boolean isBound = false;

    private EventBus eventBus;


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            OcrService.MyBinder binder = (OcrService.MyBinder) service;
            ocrService = binder.getService();
            if(ocrService != null) {
                eventBus.register(ocrService);
                isBound = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if(ocrService != null && eventBus != null) {
                eventBus.unregister(ocrService);
            }
            ocrService = null;
            isBound = false;
        }
    };

    public MainWritingView(Context context, AttributeSet attrs) throws InterruptedException {
        super(context, attrs);
        if (!this.isInEditMode()) {
            DATA_PATH = Environment
                    .getExternalStorageDirectory().toString() + "/TesseractOCR/";
            bindToService(context);
        }

        drawPath = new Path();
        drawPaint = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(6);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        drawPaint.setPathEffect(new CornerPathEffect(50));

    }

    @Subscribe
    public void onSoftKeyboardEvent(SoftKeyboardEvent event) {
        int eventType = event.getEventType();
        if (Objects.equals(eventType, SoftKeyboardEvent.CLEAR)) {
            pathStack.clear();
            invalidate();
        } else if (Objects.equals(eventType, SoftKeyboardEvent.UNDO)) {
            if (pathStack.size() > 0) {
                pathStack.pop();
                invalidate();
                requestOcr();
            }
        }
    }

    @Subscribe
    public void onInputSelectChangedEvent(InputSelectChangedEvent event) {
        int inputSelected = event.getEventType();
        if(ocrService != null) {
            if (inputSelected == InputSelectChangedEvent.LETTERS) {
                ocrService.setLettersWhitelist();

            } else if (inputSelected == InputSelectChangedEvent.DIGITS) {
                ocrService.setDigitsWhitelist();

            } else if (inputSelected == InputSelectChangedEvent.SYMBOLS) {
                ocrService.setSymbolsWhitelist();
            }
        }
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
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
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        eventBus = EventBus.getDefault();
        eventBus.register(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        eventBus.unregister(this);
        this.getContext().unbindService(serviceConnection);
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
        drawFromPathStack(canvas);
    }

    private void drawFromPathStack(Canvas canvas) {
        canvas.drawPath(drawPath, drawPaint);
        for (Path aPathStack : pathStack) {
            canvas.drawPath(aPathStack, drawPaint);
        }
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
                requestOcr();
                pathStack.push(drawPath);
                drawPath = new Path();
                break;
            default:
                return false;
        }

        // Makes our view repaint and call onDraw
        invalidate();
        return true;
    }

    public Bitmap getBitmap() {
        this.setDrawingCacheEnabled(true);
        this.buildDrawingCache();
        Bitmap bmp = Bitmap.createBitmap(this.getDrawingCache());
        this.setDrawingCacheEnabled(false);
        return bmp;
    }

    private void requestOcr() {
        Log.d(TAG, "executing ocr task...");
        if (!this.isInEditMode()) {
            new RequestOcrTask().execute(this.getBitmap());
        }
    }

    private class RequestOcrTask extends AsyncTask<Bitmap, Integer, String> {

        @Override
        protected String doInBackground(Bitmap... params) {
            String recognized = ocrService.requestOCR(params[0]);
            Log.d(TAG, recognized);
            return recognized;
        }

        @Override
        protected void onPostExecute(String result) {
            eventBus.post(new WritingViewEvent(result));
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.d(TAG, String.valueOf(values.length));
        }
    }
}
