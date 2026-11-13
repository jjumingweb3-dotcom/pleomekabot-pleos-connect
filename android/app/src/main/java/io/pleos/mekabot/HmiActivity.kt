package io.pleos.mekabot

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import io.pleos.mekabot.bridge.FusedLocationBridge
import io.pleos.mekabot.bridge.GleoAudioBridge
import io.pleos.mekabot.bridge.PleosVehicleBridge

/**
 * Pleos Connect AAOS HMI Activity
 *
 * Hosts the In-Car 3D / 20s Battle Web Engine and enforces Zero-Distraction compliance
 * by managing lifecycle and Gear P/D state transitions via PleosVehicleBridge.
 */
class HmiActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var vehicleBridge: PleosVehicleBridge? = null
    private var locationBridge: FusedLocationBridge? = null
    private var audioBridge: GleoAudioBridge? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.allowFileAccess = true
            webChromeClient = WebChromeClient()
        }

        val vBridge = PleosVehicleBridge(this, webView) { gearStr ->
            runOnUiThread {
                handleZeroDistractionLifecycle(gearStr)
            }
        }
        val lBridge = FusedLocationBridge(this, webView)
        val aBridge = GleoAudioBridge(this, webView)

        vehicleBridge = vBridge
        locationBridge = lBridge
        audioBridge = aBridge

        webView.addJavascriptInterface(vBridge, "PleosNativeVehicle")
        webView.addJavascriptInterface(lBridge, "PleosNativeLocation")
        webView.addJavascriptInterface(aBridge, "PleosNativeAudio")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                vBridge.startListening()
                lBridge.startLocationUpdates()
            }
        }

        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")
    }

    /**
     * Zero-Distraction Compliance:
     * When gear is 'D', 'R', or 'N', the battle UI immediately blinds interactive controls.
     */
    private fun handleZeroDistractionLifecycle(gearStr: String) {
        if (gearStr == "P") {
            webView.resumeTimers()
        }
    }

    override fun onDestroy() {
        locationBridge?.stopLocationUpdates()
        vehicleBridge?.release()
        audioBridge?.release()
        webView.destroy()
        super.onDestroy()
    }
}
