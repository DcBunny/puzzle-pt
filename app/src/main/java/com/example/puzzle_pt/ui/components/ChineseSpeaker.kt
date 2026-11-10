package com.example.puzzle_pt.ui.components

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

class ChineseSpeaker(context: Context) {
    private var engine: TextToSpeech? = null
    private var ready = false

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val languageResult = engine?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                ready = languageResult != TextToSpeech.LANG_MISSING_DATA &&
                    languageResult != TextToSpeech.LANG_NOT_SUPPORTED
                engine?.setSpeechRate(0.85f)
            }
        }
    }

    fun speak(text: String): Boolean {
        val currentEngine = engine ?: return false
        if (!ready) return false
        currentEngine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lesson")
        return true
    }

    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
    }
}

@Composable
fun rememberChineseSpeaker(): ChineseSpeaker {
    val context = LocalContext.current
    val speaker = remember { ChineseSpeaker(context) }
    DisposableEffect(speaker) {
        onDispose { speaker.shutdown() }
    }
    return speaker
}
