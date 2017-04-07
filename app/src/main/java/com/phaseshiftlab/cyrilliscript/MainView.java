package com.phaseshiftlab.cyrilliscript;

import android.content.Context;
import android.graphics.Canvas;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.phaseshiftlab.cyrilliscript.eventslib.InputSelectChangedEvent;
import com.phaseshiftlab.ocrlib.PermissionRequestActivity;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * TODO: document your custom view class.
 */
public class MainView extends ConstraintLayout implements AdapterView.OnItemSelectedListener {
    private final Context context;

    public MainView(Context context) {
        super(context);
        this.context = context;
    }

    public MainView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public MainView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(!this.isInEditMode()){
            initInputSelectSpinner();
            initLangguageSelectSpinner();
        }
    }

    private void initLangguageSelectSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.languageSelect);
        List<String> installedLanguages = PermissionRequestActivity.getInstalledLanguagesFromPrefs(context);
        if(installedLanguages.size() > 1) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, installedLanguages);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(this);
            spinner.setVisibility(VISIBLE);
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
        EventBus.getDefault().post(new InputSelectChangedEvent(position));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
