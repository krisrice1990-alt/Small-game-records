package com.smallgamerecord.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.graphics.Color

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var updater: AppUpdater
    private var pendingGeoOrigin: String? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private val locationRequestCode = 7021
    private val fileChooserRequestCode = 7022

    @Suppress("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(33, 26, 19)
        window.navigationBarColor = Color.rgb(23, 18, 9)

        webView = WebView(this)
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        webView.setBackgroundColor(Color.rgb(33, 26, 19))
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.settings.setGeolocationEnabled(true)
        webView.addJavascriptInterface(HuntTrackingBridge(this), "AndroidHunt")
        updater = AppUpdater(this, webView)
        webView.addJavascriptInterface(updater.Bridge(), "AndroidUpdate")
        webView.addJavascriptInterface(BackupBridge(this), "AndroidBackup")
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (fine || coarse) {
                    callback.invoke(origin, true, false)
                } else {
                    pendingGeoOrigin = origin
                    pendingGeoCallback = callback
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        locationRequestCode
                    )
                }
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = filePathCallback
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                return try {
                    startActivityForResult(intent, fileChooserRequestCode)
                    true
                } catch (e: Exception) {
                    fileChooserCallback = null
                    false
                }
            }
        }
        // Edge-to-edge fix (SDK 35): the app used to draw under the status
        // bar. Pad the root by the system-bar insets so content sits between
        // the status bar and the gesture/nav area; the root's dark-brown
        // background shows behind the bars so they blend with the app.
        val root = FrameLayout(this)
        root.setBackgroundColor(Color.rgb(33, 26, 19))
        root.addView(webView)
        root.setOnApplyWindowInsetsListener { v, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        setContentView(root)
        root.requestApplyInsets()

        if (savedInstanceState == null) {
            webView.loadUrl("file:///android_asset/index.html")
        } else {
            webView.restoreState(savedInstanceState)
        }

        // Automatic update check (Settings can turn this off). Silent unless
        // a newer version is actually available.
        if (updater.autoEnabled()) {
            Handler(Looper.getMainLooper()).postDelayed({ updater.check(manual = false) }, 3000)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationRequestCode) {
            val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            pendingGeoCallback?.invoke(pendingGeoOrigin, granted, false)
            pendingGeoCallback = null
            pendingGeoOrigin = null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fileChooserRequestCode) {
            val result: Array<Uri>? = if (resultCode == RESULT_OK && data?.data != null) {
                arrayOf(data.data!!)
            } else {
                null
            }
            fileChooserCallback?.onReceiveValue(result)
            fileChooserCallback = null
        }
    }


class HuntTrackingBridge(private val activity: Activity) {
    @JavascriptInterface
    fun startTracking() {
        val intent = Intent(activity, HuntLocationService::class.java).setAction(HuntLocationService.ACTION_START)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) activity.startForegroundService(intent) else activity.startService(intent)
    }

    @JavascriptInterface
    fun stopTracking() {
        activity.stopService(Intent(activity, HuntLocationService::class.java))
    }

    @JavascriptInterface
    fun getSamplesJson(): String {
        return activity.getSharedPreferences(HuntLocationService.PREFS, Activity.MODE_PRIVATE)
            .getString(HuntLocationService.KEY_SAMPLES, "[]") ?: "[]"
    }
}

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    private fun defaultBack() {
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        // Ask the page first: Settings -> previous tab, Edit -> Records,
        // other tabs -> Totals. Only exit when the page says it is at the
        // top level (returns anything other than "true").
        webView.evaluateJavascript(
            "(window.sgrHandleBack ? window.sgrHandleBack() : false)"
        ) { result -> if (result != "true") defaultBack() }
    }
}
