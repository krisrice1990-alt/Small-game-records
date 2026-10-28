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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.content.ClipData
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var updater: AppUpdater
    private var pendingGeoOrigin: String? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private val locationRequestCode = 7021
    private val fileChooserRequestCode = 7022
    private val photoCameraRequestCode = 7031
    private val photoGalleryRequestCode = 7032
    private var pendingPhotoRecord: String? = null
    private var pendingPhotoId: String? = null

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
        webView.addJavascriptInterface(PhotosBridge(), "AndroidPhotos")
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
        if (requestCode == photoCameraRequestCode) {
            val rec = pendingPhotoRecord; val pid = pendingPhotoId
            pendingPhotoRecord = null; pendingPhotoId = null
            if (rec != null && pid != null) {
                val f = File(File(filesDir, "record-photos"), "$rec/$pid.jpg")
                if (resultCode == RESULT_OK && f.exists() && f.length() > 0L) {
                    if (downscaleInPlace(f)) notifyPhotoSaved(rec, pid)
                    else notifyPhotoFailed(rec, "That photo could not be saved.")
                } else if (resultCode == RESULT_OK) {
                    notifyPhotoFailed(rec, "That photo could not be saved.")
                }
            }
            return
        }
        if (requestCode == photoGalleryRequestCode) {
            val rec = pendingPhotoRecord
            pendingPhotoRecord = null; pendingPhotoId = null
            val uri = data?.data
            if (rec != null && resultCode == RESULT_OK && uri != null) {
                val pid = "p" + System.currentTimeMillis()
                val dest = File(File(filesDir, "record-photos"), "$rec/$pid.jpg")
                if (copyDownscaled(uri, dest)) notifyPhotoSaved(rec, pid)
                else notifyPhotoFailed(rec, "That photo could not be saved.")
            } else if (rec != null && resultCode == RESULT_OK) {
                notifyPhotoFailed(rec, "That photo could not be saved.")
            }
            return
        }
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


    private fun validId(v: String): Boolean = v.matches(Regex("[A-Za-z0-9_\\-]+"))

    inner class PhotosBridge {
        @JavascriptInterface
        fun pickCamera(recordId: String) {
            if (!validId(recordId)) return
            runOnUiThread { launchPhotoPick(recordId, camera = true) }
        }

        @JavascriptInterface
        fun pickGallery(recordId: String) {
            if (!validId(recordId)) return
            runOnUiThread { launchPhotoPick(recordId, camera = false) }
        }

        @JavascriptInterface
        fun deletePhoto(recordId: String, photoId: String) {
            if (!validId(recordId) || !validId(photoId)) return
            File(File(filesDir, "record-photos"), "$recordId/$photoId.jpg").delete()
        }

        @JavascriptInterface
        fun deleteRecordPhotos(recordId: String) {
            if (!validId(recordId)) return
            File(filesDir, "record-photos/$recordId").deleteRecursively()
        }
    }

    private fun launchPhotoPick(recordId: String, camera: Boolean) {
        pendingPhotoRecord = recordId
        if (camera) {
            val pid = "p" + System.currentTimeMillis()
            pendingPhotoId = pid
            File(filesDir, "record-photos/$recordId").mkdirs()
            val uri = Uri.parse("content://$packageName.photos/$recordId/$pid")
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                clipData = ClipData.newUri(contentResolver, "record photo", uri)
            }
            try {
                val handlers = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                for (h in handlers) {
                    grantUriPermission(h.activityInfo.packageName, uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                startActivityForResult(intent, photoCameraRequestCode)
            } catch (e: Exception) {
                pendingPhotoRecord = null; pendingPhotoId = null
                notifyPhotoFailed(recordId, "No camera app was found.")
            }
        } else {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            try {
                startActivityForResult(intent, photoGalleryRequestCode)
            } catch (e: Exception) {
                pendingPhotoRecord = null
                notifyPhotoFailed(recordId, "No photo picker was found.")
            }
        }
    }

    private fun notifyPhotoSaved(recordId: String, photoId: String) {
        webView.post {
            webView.evaluateJavascript(
                "window.sgrPhotoSaved && sgrPhotoSaved('$recordId','$photoId')", null)
        }
    }

    private fun notifyPhotoFailed(recordId: String, message: String) {
        val safe = message.replace("'", "")
        webView.post {
            webView.evaluateJavascript(
                "window.sgrPhotoFailed && sgrPhotoFailed('$recordId','$safe')", null)
        }
    }

    /** Rewrites [file] as JPEG with longest edge <= 1600 px. False on failure. */
    private fun downscaleInPlace(file: File): Boolean {
        return try {
            val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: return false
            val out = scaleDown(bmp)
            FileOutputStream(file).use { out.compress(Bitmap.CompressFormat.JPEG, 82, it) }
            true
        } catch (e: Exception) { false }
    }

    /** Decodes [uri] downscaled (longest edge <= 1600 px) and writes JPEG to [dest]. */
    private fun copyDownscaled(uri: Uri, dest: File): Boolean {
        return try {
            dest.parentFile?.mkdirs()
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= 800 && bounds.outHeight / (sample * 2) >= 800) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return false
            val out = scaleDown(bmp)
            FileOutputStream(dest).use { out.compress(Bitmap.CompressFormat.JPEG, 82, it) }
            true
        } catch (e: Exception) { false }
    }

    private fun scaleDown(bmp: Bitmap): Bitmap {
        val maxEdge = 1600
        val w = bmp.width; val h = bmp.height
        if (w <= maxEdge && h <= maxEdge) return bmp
        val ratio = maxEdge.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bmp, (w * ratio).toInt(), (h * ratio).toInt(), true)
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
