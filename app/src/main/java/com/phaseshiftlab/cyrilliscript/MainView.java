package com.phaseshiftlab.cyrilliscript;

import android.content.Context;
import android.graphics.Canvas;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.ToggleButton;

import com.phaseshiftlab.cyrilliscript.eventslib.DownloadSuccessEvent;
import com.phaseshiftlab.cyrilliscript.eventslib.InputSelectChangedEvent;
import com.phaseshiftlab.cyrilliscript.eventslib.LanguageChangeEvent;
import com.phaseshiftlab.cyrilliscript.eventslib.UserDefinedDictionaryEvent;
import com.phaseshiftlab.ocrlib.PermissionRequestActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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
        if (!this.isInEditMode()) {
            EventBus.getDefault().register(this);
            initInputSelectSpinner();
            initLangguageSelectSpinner();
        }
    }

    private void initLangguageSelectSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.languageSelect);
        List<String> installedLanguages = PermissionRequestActivity.getInstalledLanguagesFromPrefs(context);
        if (installedLanguages.size() > 1) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, installedLanguages);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(this);
            spinner.setVisibility(VISIBLE);
        }
    }

    @Subscribe
    public void onUserDefinedDictionaryEvent(UserDefinedDictionaryEvent event) {
        ToggleButton saveToUserDict = (ToggleButton) findViewById(R.id.saveToUserDictionary);
        if (event.getEventType() == UserDefinedDictionaryEvent.SHOW) {
            saveToUserDict.setVisibility(VISIBLE);
        } else if (event.getEventType() == UserDefinedDictionaryEvent.HIDE) {
            saveToUserDict.setVisibility(GONE);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDownloadSuccessEvent(DownloadSuccessEvent event) {
        String message = event.getMessage();
        if (message != null && message.equals("SUCCESS")) {
            initLangguageSelectSpinner();
        }
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    private void initInputSelectSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.inputSelect);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.input_select, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.inputSelect:
                EventBus.getDefault().post(new InputSelectChangedEvent(position));
                break;
            case R.id.languageSelect:
                EventBus.getDefault().post(new LanguageChangeEvent(position));
                break;

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
