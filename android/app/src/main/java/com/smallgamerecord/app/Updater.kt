package com.smallgamerecord.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.concurrent.thread

/**
 * In-app updater. Checks a small public update feed (update.json), downloads a
 * newer APK, verifies its size + SHA-256, and hands it to the Android system
 * installer. Same package + same signing key means the install lands in place
 * and the user's on-device records are untouched.
 *
 * The feed and APK live in a separate PUBLIC releases repository so the phone
 * needs no login and no credential of any kind is stored in the app.
 */
class AppUpdater(private val activity: Activity, private val webView: WebView) {

    companion object {
        const val FEED_URL =
            "https://raw.githubusercontent.com/krisrice1990-alt/Small-game-records-releases/main/update.json"
        private const val PREFS = "sgr_updater"
        private const val KEY_AUTO = "auto_update"
    }

    private val prefs by lazy { activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    fun autoEnabled(): Boolean = prefs.getBoolean(KEY_AUTO, true)

    fun currentVersionCode(): Long = try {
        val info = activity.packageManager.getPackageInfo(activity.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
        else @Suppress("DEPRECATION") info.versionCode.toLong()
    } catch (e: Exception) {
        0L
    }

    fun currentVersionName(): String = try {
        activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: ""
    } catch (e: Exception) {
        ""
    }

    private fun status(message: String, toast: Boolean = false) {
        activity.runOnUiThread {
            if (toast) Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
            val escaped = message.replace("\\", "\\\\").replace("'", "\\'")
            webView.evaluateJavascript(
                "window.sgrUpdateStatus&&sgrUpdateStatus('$escaped')", null
            )
        }
    }

    /** manual = true when the user tapped "Check for updates". */
    fun check(manual: Boolean) {
        thread {
            try {
                val feedText = fetchText(FEED_URL)
                val feed = JSONObject(feedText)
                val latestCode = feed.optLong("version_code", 0L)
                val latestName = feed.optString("version_name", "")
                val apkUrl = feed.optString("apk_url", "")
                val sha = feed.optString("sha256", "")
                val size = feed.optLong("size_bytes", 0L)
                val mine = currentVersionCode()
                if (latestCode <= mine || apkUrl.isEmpty()) {
                    if (manual) status("You're on the latest version (v${currentVersionName()}).", true)
                    return@thread
                }
                status("Update found: v$latestName — downloading…", manual)
                // Android requires a one-time per-app permission before the
                // system installer will accept a downloaded APK.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    !activity.packageManager.canRequestPackageInstalls()
                ) {
                    status("Allow installs from this app once, then check for updates again.", true)
                    val intent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${activity.packageName}")
                    )
                    activity.startActivity(intent)
                    return@thread
                }
                val apk = ApkProvider.apkFile(activity)
                downloadTo(apkUrl, apk)
                if (size > 0 && apk.length() != size) {
                    status("Update download did not match the expected size — not installed.", true)
                    return@thread
                }
                if (sha.isNotEmpty() && !sha256(apk).equals(sha, ignoreCase = true)) {
                    status("Update download failed its safety check — not installed.", true)
                    return@thread
                }
                status("Update v$latestName downloaded — opening the installer. Your records stay as they are.", true)
                val uri = ApkProvider.uriFor(activity)
                activity.grantUriPermission(
                    "com.android.packageinstaller", uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val install = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activity.startActivity(install)
            } catch (e: Exception) {
                if (manual) status("Update check failed — check your connection and try again.", true)
            }
        }
    }

    private fun fetchText(urlText: String): String {
        val conn = (URL(urlText).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "SmallGameRecordsNL-Updater")
        }
        conn.inputStream.bufferedReader().use { return it.readText() }
    }

    private fun downloadTo(urlText: String, dest: File) {
        val conn = (URL(urlText).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 60000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "SmallGameRecordsNL-Updater")
        }
        conn.inputStream.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Methods exposed to the Settings screen in the web UI. */
    inner class Bridge {
        @JavascriptInterface
        fun checkForUpdates() = check(manual = true)

        @JavascriptInterface
        fun getAutoUpdate(): Boolean = autoEnabled()

        @JavascriptInterface
        fun setAutoUpdate(enabled: Boolean) {
            prefs.edit().putBoolean(KEY_AUTO, enabled).apply()
        }

        @JavascriptInterface
        fun getVersionName(): String = currentVersionName()

        @JavascriptInterface
        fun getVersionCode(): Long = currentVersionCode()
    }
}
