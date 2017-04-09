package com.phaseshiftlab.languagelib;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

public class StatisticsDatabaseHelper extends SQLiteAssetHelper {
    private static final String DATABASE_NAME = "statistics.db";
    private static final int DATABASE_VERSION = 1;
    private SQLiteDatabase statisticsDb;
    public StatisticsDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        statisticsDb = getWritableDatabase();
    }


    public long insertMeanConfidenceDataPoint(Integer meanConfidence) {
        String tableName = "mean_confidence_events";
        ContentValues contentValues = new ContentValues();
        contentValues.put("mean_confidence", meanConfidence);
        return statisticsDb.insert(tableName, null, contentValues);
    }

    public Cursor getTotals() {
        Cursor c = statisticsDb.query("mean_confidence_events_totals", new String[]{"m_c_events_rows_count", "m_c_events_rows_avg"}, null, null, null,null,null);
        c.moveToFirst();
        return c;
    }
}
