package com.iqrasmart.app

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pending: Triple<String, Float, Float>? = null
    private var pendingId = 0

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(Bridge(), "AndroidTTS")
        setContentView(webView)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("ar")
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) = done(utteranceId)
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) = done(utteranceId)
                    override fun onStop(utteranceId: String?, interrupted: Boolean) = done(utteranceId)
                })
                ttsReady = true
                pending?.let { doSpeak(it.first, it.second, it.third, pendingId) }
                pending = null
            }
        }

        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun done(id: String?) {
        if (id == null) return
        runOnUiThread {
            webView.evaluateJavascript("window.__ttsDone&&window.__ttsDone($id)", null)
        }
    }

    private fun doSpeak(text: String, rate: Float, pitch: Float, id: Int) {
        val t = tts ?: return
        t.setSpeechRate(rate)
        t.setPitch(pitch)
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, id.toString())
    }

    inner class Bridge {
        @JavascriptInterface
        fun speak(text: String, rate: Double, pitch: Double, id: Int) {
            if (ttsReady) {
                doSpeak(text, rate.toFloat(), pitch.toFloat(), id)
            } else {
                pending = Triple(text, rate.toFloat(), pitch.toFloat())
                pendingId = id
            }
        }

        @JavascriptInterface
        fun stop() {
            tts?.stop()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        webView.destroy()
        super.onDestroy()
    }
}
