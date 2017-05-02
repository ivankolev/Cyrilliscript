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

public class LanguageItemAdapter extends ArrayAdapter<LanguageItem> {

    private String TAG = "Cyrilliscript";
    private ArrayList<LanguageItem> dataSet;
    Context mContext;

    private static class ViewHolder {
        TextView languageDescription;
        Button downloadButton;
        Button deleteButton;
    }


    public LanguageItemAdapter(ArrayList<LanguageItem> dataSet, Context mContext) {
        super(mContext, R.layout.language_item_row, dataSet);
        this.dataSet = dataSet;
        this.mContext = mContext;
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
            viewHolder.languageDescription = (TextView) convertView.findViewById(R.id.languageDescription);
            viewHolder.downloadButton = (Button) convertView.findViewById(R.id.downloadLanguageButton);
            viewHolder.deleteButton = (Button) convertView.findViewById(R.id.deleteLanguageButton);
            result = convertView;

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            result = convertView;
        }


        viewHolder.languageDescription.setText(languageItem.getLanguageDescription());
        if(!languageItem.getInstalled()) {
            createDownloadClickListener(position, viewHolder);
        } else {
            createDeleteClickListener(position, viewHolder);
        }

        // Return the completed view to render on screen
        return convertView;
    }

    private void createDownloadClickListener(int position, ViewHolder viewHolder) {
        viewHolder.deleteButton.setVisibility(View.GONE);
        viewHolder.downloadButton.setTag(position);
        viewHolder.downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = (Integer) v.getTag();
                Object object = getItem(position);
                LanguageItem languageItem = (LanguageItem) object;

                int i = v.getId();
                if (i == R.id.downloadLanguageButton) {
                    String languageName = languageItem.getLanguageName();
                    Log.d(TAG, "attempt to download language data file for: " + languageName);
                    OcrLanguageSupport.downloadTesseractData(mContext, languageName);
                }
            }
        });
    }

    private void createDeleteClickListener(int position, ViewHolder viewHolder) {
        viewHolder.downloadButton.setVisibility(View.GONE);
        viewHolder.deleteButton.setTag(position);
        viewHolder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = (Integer) v.getTag();
                Object object = getItem(position);
                LanguageItem languageItem = (LanguageItem) object;

                int i = v.getId();
                if (i == R.id.deleteLanguageButton) {
                    String languageName = languageItem.getLanguageName();
                    Log.d(TAG, "attempt to delete language data file for: " + languageName);
                    //actually delete it here
                }
            }
        });
    }
}
