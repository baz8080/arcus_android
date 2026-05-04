package com.arcuscomputing

import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arcuscomputing.dictionary.PartOfSpeech
import com.arcuscomputing.dictionarypro.ads.R

class QuickResultListAdapter(
    private val results: List<WordModel>,
    private val callbacks: Callbacks
) : RecyclerView.Adapter<QuickResultListAdapter.ViewHolder>() {

    interface Callbacks {
        fun inFavouritesMode(): Boolean
        fun onWordClick(word: String)
        fun isFavourited(word: String, definition: String): Boolean
        fun onFavouriteToggled(word: String, definition: String, added: Boolean)
        fun onSpeak(word: String)
        fun onShare(word: String, definition: String)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val headline: TextView = view.findViewById(R.id.definition_tv_headline)
        val definition: TextView = view.findViewById(R.id.definition_tv_definition)
        val type: TextView = view.findViewById(R.id.definition_tv_type)
        val synonyms: TextView = view.findViewById(R.id.definition_tv_synonyms)
        val favIcon: ImageView = view.findViewById(R.id.FavIcon)
        val ttsIcon: ImageView = view.findViewById(R.id.TtsIcon)
        val shareIcon: ImageView = view.findViewById(R.id.ShareIcon)
    }

    fun getResults(): List<WordModel> = results

    override fun getItemCount() = results.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.definition_table, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val word = results[position]

        holder.headline.text = capitalize(word.word)

        val pos = if (callbacks.inFavouritesMode()) PartOfSpeech.fromAbbreviation(word.definition) else null
        val def = if (pos != null) word.definition.substringAfter(" ") else word.definition
        val displayType = pos?.label ?: word.type

        holder.definition.apply {
            movementMethod = LinkMovementMethod.getInstance()
            setText(capitalize(def), TextView.BufferType.SPANNABLE)
            linkifyDefinition(this, def)
        }

        holder.type.text = capitalize(displayType)

        if (word.synonyms.isNotEmpty()) {
            holder.synonyms.movementMethod = LinkMovementMethod.getInstance()
            holder.synonyms.setText("Synonyms: ${word.synonyms}", TextView.BufferType.SPANNABLE)
            holder.synonyms.visibility = View.VISIBLE
            linkifySynonyms(holder.synonyms, holder.synonyms.text.toString())
        } else {
            holder.synonyms.visibility = View.GONE
        }

        val storedDef = getStoredDefinition(word)
        holder.favIcon.setImageResource(
            if (callbacks.isFavourited(word.word, storedDef)) R.drawable.ic_star
            else R.drawable.ic_star_border
        )
        holder.favIcon.setOnClickListener {
            val d = getStoredDefinition(word)
            val adding = !callbacks.isFavourited(word.word, d)
            callbacks.onFavouriteToggled(word.word, d, adding)
            holder.favIcon.setImageResource(if (adding) R.drawable.ic_star else R.drawable.ic_star_border)
        }
        holder.ttsIcon.setOnClickListener { callbacks.onSpeak(word.word) }
        holder.shareIcon.setOnClickListener { callbacks.onShare(word.word, word.definition) }
    }

    private fun getStoredDefinition(word: WordModel): String {
        if (callbacks.inFavouritesMode()) return word.definition
        val pos = PartOfSpeech.fromLabel(word.type) ?: return word.definition
        return "${pos.abbreviation} ${word.definition}"
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
                    override fun onClick(widget: View) { callbacks.onWordClick(currentWord) }
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
                override fun onClick(widget: View) { callbacks.onWordClick(currentWord) }
            }, currentStart, commaPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (done) break
            currentStart = commaPos + 2
        }
    }

    companion object {
        private const val CLEAN_PATTERN = "\\(|\\)|;|\\.|'|`|,|\""
        private val SIMPLE_WORDS = setOf(
            "than", "that", "with", "which",
            "goes", "whose", "what", "where",
            "when", "they", "from", "your", "into"
        )

        private fun capitalize(str: String?): String {
            if (str.isNullOrEmpty()) return str ?: ""
            return str[0].titlecase() + str.substring(1)
        }
    }
}
