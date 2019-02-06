package com.phaseshiftlab.languagelib

import android.app.Activity
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.dictionary_activity.*
import kotlinx.android.synthetic.main.dictionary_entry.view.*
import org.jetbrains.anko.*
import org.jetbrains.anko.db.*

class MyRowParser : RowParser<Word> {
    override fun parseRow(columns: Array<Any?>): Word {
        Log.d("MyRowParser", "parsing...")
        return Word(columns[0] as String)
    }
}

class UserDictionaryActivity : Activity() {
    var selectedWords: HashMap<String, Int> = HashMap()

    private lateinit var userDefinedWords: List<Word>
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("UserDictionaryActivity", "onCreate")
        super.onCreate(savedInstanceState)
        userDefinedWords = database.use {
            select(Word.TABLE_NAME).exec { parseList(MyRowParser()) }
        }
        setContentView(R.layout.dictionary_activity)
        dictionaryList.layoutManager = LinearLayoutManager(this)
        dictionaryList.setHasFixedSize(true)
        Log.d("UserDictionaryActivity", "bind to adapter")
        dictionaryList.adapter = DictionaryAdapter(userDefinedWords, selectedWords)

        deleteButton.setOnClickListener {
            Log.d("UserDictionaryActivity", "deleteSelected")
            for (word in this.selectedWords.keys) {
                database.use {
                    delete(Word.TABLE_NAME, "${Word.COLUMN_WORD} = {matchingWord}", "matchingWord" to word)
                }
                Log.d("UserDictionaryActivity", word + " deleted from Db")
                selectedWords.remove(word)
                userDefinedWords = database.use {
                    select(Word.TABLE_NAME).exec { parseList(MyRowParser()) }
                }
            }
            dictionaryList.adapter = DictionaryAdapter(userDefinedWords, selectedWords)
        }
    }
}

class DictionaryAdapter(private var userDefinedWordsList: List<Word>, private var selectedWords: HashMap<String, Int>) : RecyclerView.Adapter<DictionaryAdapter.ViewHolder>() {
    override fun getItemCount(): Int {
        return userDefinedWordsList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d("DictionaryAdapter", "onBindViewHolder")
        holder.dictionaryWordEntry.text = userDefinedWordsList[position].word
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.dictionary_entry, parent, false)

        view.dictionaryWord.setCheckMarkDrawable(0)
        view.dictionaryWord.setOnClickListener { selectWord(view.dictionaryWord) }
        Log.d("DictionaryAdapter", "onCreateViewHolder")
        return ViewHolder(view)
    }

    private fun selectWord(view: CheckedTextView) {
        view.isChecked = !view.isChecked
        if (view.isChecked) {
            view.context
            selectedWords.put(view.text.toString(), 1)
            view.setCheckMarkDrawable(R.drawable.ic_check_circle)
        } else {
            selectedWords.remove(view.text.toString())
            view.dictionaryWord.setCheckMarkDrawable(0)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dictionaryWordEntry: TextView = view.find(R.id.dictionaryWord)
        init { }
    }

}

class MySqlHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "user.dict.db") {

    companion object {
        private var instance: MySqlHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): MySqlHelper {
            Log.d("MySqlHelper", "companion object, getInstance")
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

        val COLUMN_WORD = "word"
    }
}

// Access property for Context
val Context.database: MySqlHelper
    get() = MySqlHelper.getInstance(applicationContext)