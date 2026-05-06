package com.arcuscomputing.dictionary

import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arcuscomputing.dictionarypro.ads.databinding.DefinitionTableBinding
import com.arcuscomputing.dictionarypro.ads.R

class QuickResultListAdapter(
    private val callbacks: Callbacks
) : ListAdapter<WordModel, QuickResultListAdapter.ViewHolder>(DIFF_CALLBACK) {

    interface Callbacks {
        fun onWordClick(word: String)
        fun onFavouriteToggled(word: String, definition: String, added: Boolean)
        fun onSpeak(word: String)
        fun onShare(word: String, definition: String)
    }

    class ViewHolder(val binding: DefinitionTableBinding) : RecyclerView.ViewHolder(binding.root)

    private val favourites = mutableSetOf<Pair<String, String>>()
    private var favouritesMode = false

    fun submit(
        results: List<WordModel>,
        favourites: Set<Pair<String, String>>,
        favouritesMode: Boolean
    ) {
        this.favourites.clear()
        this.favourites.addAll(favourites)
        this.favouritesMode = favouritesMode
        submitList(results)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DefinitionTableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val word = getItem(position)
        val b = holder.binding

        b.definitionTvHeadline.text = WordTextUtils.capitalize(word.word)

        val pos = if (favouritesMode) PartOfSpeech.fromAbbreviation(word.definition) else null
        val def = if (pos != null) word.definition.substringAfter(" ") else word.definition
        val displayType = pos?.label ?: word.type

        b.definitionTvDefinition.apply {
            movementMethod = LinkMovementMethod.getInstance()
            setText(WordTextUtils.capitalize(def), TextView.BufferType.SPANNABLE)
            linkifyDefinition(this, def)
        }

        b.definitionTvType.text = WordTextUtils.capitalize(displayType)

        if (word.synonyms.isNotEmpty()) {
            val synonymText = b.root.context.getString(R.string.synonyms_prefix, word.synonyms)
            b.definitionTvSynonyms.movementMethod = LinkMovementMethod.getInstance()
            b.definitionTvSynonyms.setText(synonymText, TextView.BufferType.SPANNABLE)
            b.definitionTvSynonyms.visibility = View.VISIBLE
            linkifySynonyms(b.definitionTvSynonyms, synonymText)
        } else {
            b.definitionTvSynonyms.visibility = View.GONE
        }

        val storedDef = WordTextUtils.getStoredDefinition(word, favouritesMode)
        b.favIcon.setImageResource(starIcon(word.word, storedDef))
        b.favIcon.setOnClickListener {
            val key = word.word to storedDef
            val adding = key !in favourites
            if (adding) favourites += key else favourites -= key
            b.favIcon.setImageResource(if (adding) R.drawable.ic_star else R.drawable.ic_star_border)
            callbacks.onFavouriteToggled(word.word, storedDef, adding)
        }
        b.ttsIcon.setOnClickListener { callbacks.onSpeak(word.word) }
        b.shareIcon.setOnClickListener { callbacks.onShare(word.word, word.definition) }
    }

    private fun starIcon(word: String, definition: String) =
        if ((word to definition) in favourites) R.drawable.ic_star else R.drawable.ic_star_border

    private fun linkifyDefinition(tv: TextView, definition: String) {
        val span = tv.text as Spannable
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, tv.textSize + 1.5f)
        var currentStart = 0
        while (true) {
            var spacePos = definition.indexOf(" ", currentStart)
            val done = spacePos == -1
            if (done) spacePos = definition.length
            val cleaned = definition.substring(currentStart, spacePos)
                .replace(Regex(WordTextUtils.CLEAN_PATTERN), " ")
                .trim()
            if (WordTextUtils.isLinkableWord(cleaned)) {
                span.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) { callbacks.onWordClick(cleaned) }
                }, currentStart, spacePos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (done) break
            currentStart = spacePos + 1
        }
    }

    private fun linkifySynonyms(tv: TextView, synString: String) {
        val span = tv.text as Spannable
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, tv.textSize + 1.5f)
        val firstSpace = synString.indexOf(" ")
        var currentStart = if (firstSpace >= 0) firstSpace + 1 else return
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
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WordModel>() {
            override fun areItemsTheSame(oldItem: WordModel, newItem: WordModel): Boolean =
                oldItem.word == newItem.word && oldItem.definition == newItem.definition

            override fun areContentsTheSame(oldItem: WordModel, newItem: WordModel): Boolean =
                oldItem == newItem
        }
    }
}
