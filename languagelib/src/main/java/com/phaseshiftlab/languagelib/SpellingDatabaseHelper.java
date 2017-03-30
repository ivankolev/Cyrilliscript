package com.phaseshiftlab.languagelib;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

public class SpellingDatabaseHelper extends SQLiteAssetHelper {

    private static final String DATABASE_NAME = "bg.spelling.db";
    private static final int DATABASE_VERSION = 1;

    public SpellingDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public Cursor getWords(String inputWord) {

        SQLiteDatabase db = getReadableDatabase();

        String preparedStatement = "SELECT DISTINCT word_form AS suggestions FROM w_words JOIN w_word_forms ON w_words.`ID` = w_word_forms.`word_id` WHERE w_words.word like ? ORDER BY length(suggestions) LIMIT 5";
        String [] sqlSelect = {inputWord + "%"};

        Cursor c = db.rawQuery(preparedStatement, sqlSelect);

        c.moveToFirst();
        return c;

    }
}
