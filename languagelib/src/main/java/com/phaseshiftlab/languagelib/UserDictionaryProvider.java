package com.phaseshiftlab.languagelib;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;

public class UserDictionaryProvider extends ContentProvider {
    public static final Uri CONTENT_URI =
            Uri.parse("content://com.phaseshiftlab.languagelib.userdictionaryprovider/words");
    private static final int WORDS = 1;
    private static final int WORDS_COUNT = 2;
    private static final int WORD_ID = 3;
    private static final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        matcher.addURI(CONTENT_URI.getAuthority(), "words", WORDS);
        matcher.addURI(CONTENT_URI.getAuthority(), "words/count", WORDS_COUNT);//Add before wildcard, or it won't be matched!
        matcher.addURI(CONTENT_URI.getAuthority(), "words/*", WORD_ID);
    }

    SQLiteDatabase db;
    UserDictDatabaseHelper userDictDatabaseHelper;

    public UserDictionaryProvider() {
    }

    @Override
    public boolean onCreate() {
        userDictDatabaseHelper = new UserDictDatabaseHelper(getContext());
        db = userDictDatabaseHelper.getWritableDatabase();
        return true;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        long result = db.insertWithOnConflict("user_defined_words", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if(result != -1) {
            return Uri.withAppendedPath(uri, values.getAsString("word"));
        } else {
            return null;
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        int result = matcher.match(uri);
        switch(result) {
            case WORDS:
                return userDictDatabaseHelper.queryUserDictionary(selection);
            case WORDS_COUNT:
                return db.rawQuery("SELECT count(word) FROM user_defined_words", null);
            default:
                return null;
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        return 0;
    }
}
