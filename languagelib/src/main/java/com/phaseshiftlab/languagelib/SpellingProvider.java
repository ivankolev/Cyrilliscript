package com.phaseshiftlab.languagelib;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class SpellingProvider extends ContentProvider {
    public static final Uri CONTENT_URI =
            Uri.parse("content://com.phaseshiftlab.languagelib.spellingprovider/words");

    private static final int WORDS = 1;
    private static final int WORDS_COUNT = 2;
    private static final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        matcher.addURI(CONTENT_URI.getAuthority(), "words", WORDS);
        matcher.addURI(CONTENT_URI.getAuthority(), "words/count", WORDS_COUNT);
    }

    SQLiteDatabase db;
    SpellingDatabaseHelper spellingDatabaseHelper;

    public SpellingProvider() {
    }

    @Override
    public boolean onCreate() {
        spellingDatabaseHelper = new SpellingDatabaseHelper(getContext());
        db = spellingDatabaseHelper.getReadableDatabase();
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        int result = matcher.match(uri);
        switch(result) {
            case WORDS:
                return spellingDatabaseHelper.getWords(selection);
            case WORDS_COUNT:
                return db.rawQuery("SELECT count(w_words.word) FROM w_words", null);
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
        return null;
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
