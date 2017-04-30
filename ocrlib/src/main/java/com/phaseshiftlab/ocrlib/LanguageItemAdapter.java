package com.phaseshiftlab.ocrlib;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class LanguageItemAdapter extends ArrayAdapter<LanguageItem> implements View.OnClickListener {

    private String TAG = "Cyrilliscript";
    private ArrayList<LanguageItem> dataSet;
    Context mContext;

    private static class ViewHolder {
        TextView languageName;
        Button downloadButton;
    }


    public LanguageItemAdapter(ArrayList<LanguageItem> dataSet, Context mContext) {
        super(mContext, R.layout.language_item_row, dataSet);
        this.dataSet = dataSet;
        this.mContext = mContext;
    }

    @Override
    public void onClick(View v) {
        int position = (Integer) v.getTag();
        Object object = getItem(position);
        LanguageItem languageItem = (LanguageItem) object;

        int i = v.getId();
        if (i == R.id.downloadLanguageButton) {
            Log.d(TAG, "download language " + languageItem.getLanguageName());
        }
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        LanguageItem languageItem = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag

        final View result;

        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.language_item_row, parent, false);
            viewHolder.languageName = (TextView) convertView.findViewById(R.id.languageName);
            viewHolder.downloadButton = (Button) convertView.findViewById(R.id.downloadLanguageButton);
            result = convertView;

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            result = convertView;
        }


        viewHolder.languageName.setText(languageItem.getLanguageName());
        if(!languageItem.getInstalled()) {
            viewHolder.downloadButton.setTag(position);
            viewHolder.downloadButton.setOnClickListener(this);
        } else {
            viewHolder.downloadButton.setVisibility(View.GONE);
        }

        // Return the completed view to render on screen
        return convertView;
    }
}
