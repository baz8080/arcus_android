package com.arcuscomputing.dictionary

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

class TtsHelper(context: Context, private val scope: CoroutineScope, preferences: ArcusPreferences) {

    private var tts: TextToSpeech? = null
    private val ready = CompletableDeferred<TextToSpeech>()

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = if (preferences.useUSEnglish) Locale.US else Locale.UK
                tts?.let { ready.complete(it) }
            } else {
                ready.cancel(CancellationException("TTS unavailable"))
            }
        }
    }

    fun speak(word: String) {
        scope.launch {
            try {
                ready.await().speak(word, TextToSpeech.QUEUE_FLUSH, null, null)
            } catch (_: CancellationException) {}
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
