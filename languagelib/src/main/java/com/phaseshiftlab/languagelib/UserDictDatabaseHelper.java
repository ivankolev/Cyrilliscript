package com.phaseshiftlab.languagelib;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;


class UserDictDatabaseHelper extends SQLiteAssetHelper {
    private static final String DATABASE_NAME = "user.dict.db";
    private static final int DATABASE_VERSION = 1;
    private final SQLiteDatabase db;
    private final String pattern = "\\p{L}*";

    UserDictDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = getWritableDatabase();
    }

    @Deprecated
    public int insertWord(String word) {
        if (word.matches(pattern)) {
            String insertStatement = "INSERT OR IGNORE INTO user_defined_words VALUES( ? )";
            String[] sqlInsert = {word};
            Cursor c = db.rawQuery(insertStatement, sqlInsert);
            Integer result = 0;
            if(c.moveToFirst()) {
                 result = c.getInt(0);
            }
            c.close();
            return result;
        } else {
            return -1;
        }
    }

    @Deprecated
    public int getWordCount() {
        String selectStatement = "SELECT count(word) FROM user_defined_words";
        Cursor c = db.rawQuery(selectStatement, null);
        int result;
        if (c.moveToFirst()) {
            result = c.getInt(0);
        } else {
            result = -1;
        }

        c.close();
        return result;
    }

    @Deprecated
    public Cursor getWords() {
        String selectStatement = "SELECT word FROM user_defined_words";
        Cursor c = db.rawQuery(selectStatement, null);
        if(c.moveToFirst()) {
            return c;
        } else {
            return null;
        }
    }

    Cursor queryUserDictionary(String word) {
        if (word.matches(pattern)) {
            String preparedStatement = "SELECT word FROM user_defined_words WHERE word like ? ORDER BY length(word) LIMIT 5";
            String[] sqlSelect = {word + "%"};

            Cursor c = db.rawQuery(preparedStatement, sqlSelect);

            c.moveToFirst();
            return c;
        } else {
            return null;
        }
    }
}
