package io.pleos.mekabot.bridge

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import java.util.Locale

/**
 * Gleo AI Audio Bridge (TTS / STT & Driver Headrest Sound Zone)
 *
 * Delivers Gleo AI voice briefings upon Gear P transition and processes
 * driver voice commands ("스탯 파워에 찍어줘", "배틀 시작해줘") without disturbing passengers.
 */
class GleoAudioBridge(
    context: Context,
    private val webView: WebView
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "GleoAudioBridge"
    }

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isTtsReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.KOREAN)
            isTtsReady = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED)
            tts?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            Log.i(TAG, "Gleo TTS initialized (isReady=$isTtsReady)")
        }
    }

    @JavascriptInterface
    fun speakBriefing(message: String) {
        if (!isTtsReady) return
        try {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "gleo_briefing_utterance")
        } catch (e: Exception) {
            Log.w(TAG, "speakBriefing error: ${e.message}")
        }
    }

    @JavascriptInterface
    fun dispatchVoiceCommand(utterance: String) {
        webView.post {
            webView.evaluateJavascript(
                "window.PleosEventBus && window.PleosEventBus.dispatch('onVoiceCommand', {\"utterance\":\"$utterance\"});",
                null
            )
        }
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "release error: ${e.message}")
        }
    }
}
