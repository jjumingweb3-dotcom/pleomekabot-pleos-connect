package io.pleos.mekabot.bridge

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView

/**
 * FusedLocation & NaviHelper Bridge
 *
 * Provides 1~5km sector matching coordinates and triggers Pleos Navigation routing
 * to nearby Bluehands service docks or E-pit charging stations.
 */
@SuppressLint("MissingPermission")
class FusedLocationBridge(
    private val context: Context,
    private val webView: WebView
) {
    companion object {
        private const val TAG = "FusedLocationBridge"
        // Default Seongsu Sector Coordinates (Hyundai Pleos Hub)
        private const val DEFAULT_LAT = 37.5445
        private const val DEFAULT_LNG = 127.0560
    }

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private var currentLat: Double = DEFAULT_LAT
    private var currentLng: Double = DEFAULT_LNG

    private val locationListener = LocationListener { location: Location ->
        currentLat = location.latitude
        currentLng = location.longitude
        emitToWeb(
            "onLocationSynced",
            """{"lat":$currentLat,"lng":$currentLng,"sector":"Seongsu-B2","radiusKm":3.0}"""
        )
    }

    fun startLocationUpdates() {
        try {
            val provider = when {
                locationManager?.isProviderEnabled(LocationManager.FUSED_PROVIDER) == true ->
                    LocationManager.FUSED_PROVIDER
                locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ->
                    LocationManager.GPS_PROVIDER
                else -> null
            }
            if (provider != null) {
                locationManager?.requestLocationUpdates(provider, 5000L, 10f, locationListener)
            }
            emitToWeb(
                "onLocationSynced",
                """{"lat":$currentLat,"lng":$currentLng,"sector":"Seongsu-B2","radiusKm":3.0}"""
            )
        } catch (e: Exception) {
            Log.w(TAG, "Location fallback to Seongsu Hub: ${e.message}")
        }
    }

    @JavascriptInterface
    fun navigateToBluehands(branchName: String) {
        try {
            val uri = Uri.parse("geo:$currentLat,$currentLng?q=${Uri.encode(branchName)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "NaviHelper intent fallback: ${e.message}")
        }
    }

    fun stopLocationUpdates() {
        try {
            locationManager?.removeUpdates(locationListener)
        } catch (e: Exception) {
            Log.w(TAG, "stopLocationUpdates error: ${e.message}")
        }
    }

    private fun emitToWeb(event: String, jsonPayload: String) {
        webView.post {
            webView.evaluateJavascript(
                "window.PleosEventBus && window.PleosEventBus.dispatch('$event', $jsonPayload);",
                null
            )
        }
    }
}
