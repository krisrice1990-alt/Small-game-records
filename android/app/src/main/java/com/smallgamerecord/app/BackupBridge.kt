package com.smallgamerecord.app

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import java.io.File

/**
 * Native side of record safety for the web UI:
 * - mirror(): every successful record save is mirrored into SharedPreferences,
 *   so if WebView storage is ever reset the records can be restored on launch
 *   (the web side only restores when its own store reads empty).
 * - saveBackupFile(): writes the Settings "Back up records" JSON file to the
 *   device's Downloads folder (blob downloads don't work in a plain WebView).
 */
class BackupBridge(private val activity: Activity) {

    companion object {
        private const val PREFS = "sgr_records_mirror"
        private const val KEY = "records_json"
        private const val BACKUP_NAME = "small-game-records-backup.json"
    }

    private val prefs by lazy { activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    @JavascriptInterface
    fun mirror(recordsJson: String) {
        prefs.edit().putString(KEY, recordsJson).apply()
    }

    @JavascriptInterface
    fun getMirror(): String = prefs.getString(KEY, "") ?: ""

    @JavascriptInterface
    fun saveBackupFile(recordsJson: String): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = activity.contentResolver
                // Remove our own earlier backup so there is one current file.
                try {
                    resolver.delete(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        "${MediaStore.Downloads.DISPLAY_NAME} = ?",
                        arrayOf(BACKUP_NAME)
                    )
                } catch (e: Exception) { /* best effort */ }
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, BACKUP_NAME)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return "Backup could not be saved: storage was unavailable."
                resolver.openOutputStream(uri)?.use { it.write(recordsJson.toByteArray()) }
                    ?: return "Backup could not be saved: storage was unavailable."
                "Backup saved to Downloads/$BACKUP_NAME"
            } else {
                val dir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: return "Backup could not be saved: storage was unavailable."
                File(dir, BACKUP_NAME).writeText(recordsJson)
                "Backup saved to ${dir.absolutePath}/$BACKUP_NAME"
            }
        } catch (e: Exception) {
            "Backup could not be saved. You can still use Export CSV in Records."
        }
    }
}
