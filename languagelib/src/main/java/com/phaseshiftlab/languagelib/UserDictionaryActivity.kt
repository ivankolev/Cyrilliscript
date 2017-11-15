package com.phaseshiftlab.languagelib

import android.app.Activity
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import android.widget.TextView
import kotlinx.android.synthetic.main.dictionary_activity.*
import kotlinx.android.synthetic.main.dictionary_entry.view.*
import org.jetbrains.anko.*
import org.jetbrains.anko.db.ManagedSQLiteOpenHelper
import org.jetbrains.anko.db.classParser
import org.jetbrains.anko.db.parseList
import org.jetbrains.anko.db.select


class UserDictionaryActivity: Activity() {
    lateinit var userDefinedWords: List<Word>;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userDefinedWords = database.use {
            select(Word.TABLE_NAME).exec { parseList(classParser()) }
        }
        setContentView(R.layout.dictionary_activity)
        dictionaryList.layoutManager = LinearLayoutManager(this)
        dictionaryList.setHasFixedSize(true)
        dictionaryList.adapter = DictionaryAdapter(userDefinedWords)
    }
}

class DictionaryAdapter(var userDefinedWordsList: List<Word>) : RecyclerView.Adapter<DictionaryAdapter.ViewHolder>() {
    override fun getItemCount(): Int {
        return userDefinedWordsList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.dictionaryWordEntry.text = userDefinedWordsList[position].word
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.dictionary_entry, parent, false)

        view.dictionaryWord.setCheckMarkDrawable(0)
        view.dictionaryWord.setOnClickListener { doSomething(view.dictionaryWord) }
        return ViewHolder(view)
    }

    private fun doSomething(view: CheckedTextView) {
        view.isChecked = !view.isChecked
        if(view.isChecked) {
            view.setCheckMarkDrawable(R.drawable.ic_check_circle)
        } else {
            view.dictionaryWord.setCheckMarkDrawable(0)
        }
    }

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val dictionaryWordEntry: TextView = view.find(R.id.dictionaryWord)
        init {

        }
    }

}

class MySqlHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "user.dict.db") {

    companion object {
        private var instance: MySqlHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): MySqlHelper {
            if (instance == null) {
                instance = MySqlHelper(ctx.applicationContext)
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }

}

data class Word(val word: String) {
    companion object {
        val TABLE_NAME = "user_defined_words"
        val COLUMN_WORDID = "wordid"
        val COLUMN_WORD = "word"
    }
}

// Access property for Context
val Context.database: MySqlHelper
    get() = MySqlHelper.getInstance(applicationContext)