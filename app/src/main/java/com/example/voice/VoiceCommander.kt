package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Spoken advice only; never touches the game or executes attacks. */
class VoiceCommander(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ready = tts.setLanguage(Locale.forLanguageTag("id-ID")) >= TextToSpeech.LANG_AVAILABLE
        }
    }
    fun speak(instruction: String) {
        if (ready && instruction.isNotBlank())
            tts.speak(instruction.take(240), TextToSpeech.QUEUE_FLUSH, null, "zayns-war-command")
    }
    fun shutdown() { tts.stop(); tts.shutdown() }
}
