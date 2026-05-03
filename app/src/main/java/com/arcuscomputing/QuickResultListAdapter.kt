package com.arcuscomputing

import android.content.Context
import android.content.Intent
import android.database.DataSetObserver
import android.text.Spannable
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.ImageView
import android.widget.ListAdapter
import android.widget.TextView
import com.arcuscomputing.dictionary.ArcusSearchActivity
import com.arcuscomputing.dictionary.DictionaryConstants.ADJECTIVE_LABEL
import com.arcuscomputing.dictionary.DictionaryConstants.ADJECTIVE_LABEL_ABV
import com.arcuscomputing.dictionary.DictionaryConstants.ADVERB_LABEL
import com.arcuscomputing.dictionary.DictionaryConstants.ADVERB_LABEL_ABV
import com.arcuscomputing.dictionary.DictionaryConstants.CLEAN_PATTERN
import com.arcuscomputing.dictionary.DictionaryConstants.NOUN_LABEL
import com.arcuscomputing.dictionary.DictionaryConstants.NOUN_LABEL_ABV
import com.arcuscomputing.dictionary.DictionaryConstants.SIMPLE_WORDS
import com.arcuscomputing.dictionary.DictionaryConstants.VERB_LABEL
import com.arcuscomputing.dictionary.DictionaryConstants.VERB_LABEL_ABV
import com.arcuscomputing.dictionarypro.ads.R

class QuickResultListAdapter(
    private val results: List<WordModel>,
    private val activity: ArcusSearchActivity
) : ListAdapter {

    private val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    fun getResults(): List<WordModel> = results

    override fun areAllItemsEnabled() = false
    override fun isEnabled(position: Int) = false
    override fun getCount() = results.size
    override fun getItem(position: Int): Any = results[position]
    override fun getItemId(position: Int) = position.toLong()
    override fun getItemViewType(position: Int) = Adapter.IGNORE_ITEM_VIEW_TYPE
    override fun getViewTypeCount() = 1
    override fun hasStableIds() = true
    override fun isEmpty() = results.isEmpty()
    override fun registerDataSetObserver(observer: DataSetObserver) {}
    override fun unregisterDataSetObserver(observer: DataSetObserver) {}

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.definition_table, parent, false)
        val word = results[position]

        view.findViewById<TextView>(R.id.definition_tv_headline).text = capitalize(word.word)

        var def = word.definition
        if (activity.inFavouritesMode()) {
            when {
                def.startsWith(NOUN_LABEL_ABV) -> { word.type = NOUN_LABEL; def = def.substringAfter(" ") }
                def.startsWith(VERB_LABEL_ABV) -> { word.type = VERB_LABEL; def = def.substringAfter(" ") }
                def.startsWith(ADVERB_LABEL_ABV) -> { word.type = ADVERB_LABEL; def = def.substringAfter(" ") }
                def.startsWith(ADJECTIVE_LABEL_ABV) -> { word.type = ADJECTIVE_LABEL; def = def.substringAfter(" ") }
            }
        }

        view.findViewById<TextView>(R.id.definition_tv_definition).apply {
            movementMethod = LinkMovementMethod.getInstance()
            setText(capitalize(def), TextView.BufferType.SPANNABLE)
            linkifyDefinition(this, def)
        }

        view.findViewById<TextView>(R.id.definition_tv_type).text =
            if (word.tagCount == -1) "Web" else capitalize(word.type)

        val synonymsView = view.findViewById<TextView>(R.id.definition_tv_synonyms)
        if (word.synonyms.isNotEmpty()) {
            synonymsView.movementMethod = LinkMovementMethod.getInstance()
            synonymsView.setText("Synonyms: ${word.synonyms}", TextView.BufferType.SPANNABLE)
            synonymsView.visibility = View.VISIBLE
            linkifySynonyms(synonymsView, synonymsView.text.toString())
        } else {
            synonymsView.visibility = View.GONE
        }

        val favIcon = view.findViewById<ImageView>(R.id.FavIcon)
        val ttsIcon = view.findViewById<ImageView>(R.id.TtsIcon)
        val shareIcon = view.findViewById<ImageView>(R.id.ShareIcon)

        if (word.tagCount == -1) {
            favIcon.visibility = View.GONE
            ttsIcon.visibility = View.GONE
            shareIcon.visibility = View.GONE
        } else {
            val d = getDefinition(word)
            favIcon.setImageResource(
                if (isFavourited(word.word, d)) R.drawable.ic_star
                else R.drawable.ic_star_border
            )
            favIcon.setOnClickListener {
                val def2 = getDefinition(word)
                if (isFavourited(word.word, def2)) {
                    activity.dbHelper.deleteFromFavourites(word.word, def2)
                    favIcon.setImageResource(R.drawable.ic_star_border)
                    if (activity.inFavouritesMode()) activity.refreshFavourites()
                } else {
                    favIcon.setImageResource(R.drawable.ic_star)
                    activity.dbHelper.insertfavourite(word.word, def2)
                }
            }
            ttsIcon.setOnClickListener { activity.speak(word.word) }
            shareIcon.setOnClickListener {
                activity.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Shared by Arcus Dictionary\n${word.word}: ${word.definition}")
                        },
                        "Share word"
                    )
                )
            }

            //linkifyDefinition()
            //linkifySynonyms()
        }

        return view
    }

    private fun getDefinition(word: WordModel): String {
        if (activity.inFavouritesMode()) return word.definition
        return when (word.type.lowercase()) {
            NOUN_LABEL.lowercase() -> "$NOUN_LABEL_ABV ${word.definition}"
            ADVERB_LABEL.lowercase() -> "$ADVERB_LABEL_ABV ${word.definition}"
            ADJECTIVE_LABEL.lowercase() -> "$ADJECTIVE_LABEL_ABV ${word.definition}"
            VERB_LABEL.lowercase() -> "$VERB_LABEL_ABV ${word.definition}"
            else -> word.definition
        }
    }

    private fun linkifyDefinition(tv: TextView, definition: String) {
        val span = tv.text as Spannable
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, tv.textSize + 1.5f)
        var currentStart = 0
        while (true) {
            var spacePos = definition.indexOf(" ", currentStart)
            val done = spacePos == -1
            if (done) spacePos = definition.length
            val currentWord = definition.substring(currentStart, spacePos).replace(Regex(CLEAN_PATTERN), " ")
            if (currentWord.length > 3 && !SIMPLE_WORDS.contains(currentWord)) {
                span.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) { activity.setQuery(currentWord) }
                    override fun updateDrawState(ds: TextPaint) { super.updateDrawState(ds) }
                }, currentStart, spacePos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (done) break
            currentStart = spacePos + 1
        }
    }

    private fun linkifySynonyms(tv: TextView, synString: String) {
        val span = tv.text as Spannable
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, tv.textSize + 1.5f)
        var currentStart = synString.indexOf(" ") + 1
        while (true) {
            var commaPos = synString.indexOf(",", currentStart)
            val done = commaPos == -1
            if (done) commaPos = synString.length
            val currentWord = synString.substring(currentStart, commaPos)
            span.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) { activity.setQuery(currentWord) }
                override fun updateDrawState(ds: TextPaint) { super.updateDrawState(ds) }
            }, currentStart, commaPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (done) break
            currentStart = commaPos + 2
        }
    }

    private fun isFavourited(word: String, definition: String) =
        activity.dbHelper.isFavourite(word, definition)

    companion object {
        private fun capitalize(str: String?): String {
            if (str.isNullOrEmpty()) return str ?: ""
            return str[0].titlecase() + str.substring(1)
        }
    }
}
