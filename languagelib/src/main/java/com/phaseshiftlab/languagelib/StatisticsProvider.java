package com.phaseshiftlab.languagelib;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;


public class StatisticsProvider extends ContentProvider {
    public static final Uri CONTENT_URI =
            Uri.parse("content://com.phaseshiftlab.languagelib.statisticsprovider/stats");
    private static final int STATS = 1;
    private static final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        matcher.addURI(CONTENT_URI.getAuthority(), "stats", STATS);
    }

    SQLiteDatabase db;
    StatisticsDatabaseHelper statisticsDatabaseHelper;

    @Override
    public boolean onCreate() {
        statisticsDatabaseHelper = new StatisticsDatabaseHelper(getContext());
        db = statisticsDatabaseHelper.getWritableDatabase();
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        int result = matcher.match(uri);
        switch (result) {
            case STATS:
                return statisticsDatabaseHelper.getTotals();
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        long result = db.insert("mean_confidence_events", null, values);
        if(result != -1) {
            return Uri.withAppendedPath(uri, String.valueOf(new Date().getTime()));
        } else {
            return null;
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
