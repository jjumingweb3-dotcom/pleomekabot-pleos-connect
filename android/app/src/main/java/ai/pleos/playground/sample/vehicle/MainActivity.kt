package ai.pleos.playground.sample.vehicle

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
 * MainActivity entry point for Pleos Connect Launcher
 * (Matches ai.pleos.playground.sample.vehicle/.MainActivity whitelist)
 */
class MainActivity : ComponentActivity() {

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
                if (gearStr == "P") {
                    webView.resumeTimers()
                }
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

    override fun onDestroy() {
        locationBridge?.stopLocationUpdates()
        vehicleBridge?.release()
        audioBridge?.release()
        webView.destroy()
        super.onDestroy()
    }
}
