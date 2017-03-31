package com.phaseshiftlab.cyrilliscript;

import android.content.Context;
import android.graphics.Canvas;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.phaseshiftlab.cyrilliscript.events.InputSelectChangedEvent;

import org.greenrobot.eventbus.EventBus;

/**
 * TODO: document your custom view class.
 */
public class MainView extends ConstraintLayout implements AdapterView.OnItemSelectedListener {
    public MainView(Context context) {
        super(context);
    }

    public MainView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MainView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(!this.isInEditMode()){
            initInputSelectSpinner();
        }
    }
//
//    @Override
//    protected void onDetachedFromWindow() {
//        super.onDetachedFromWindow();
//        EventBus.getDefault().unregister(this);
//    }

    private void initInputSelectSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.inputSelect);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.input_select, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        EventBus.getDefault().post(new InputSelectChangedEvent((String )parent.getItemAtPosition(position)));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
