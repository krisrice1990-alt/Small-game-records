package com.smallgamerecord.app

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

/**
 * Minimal framework-only ContentProvider that serves the single downloaded
 * update APK from the app's cache directory, so the system installer can read
 * it through a content:// URI (file:// URIs are blocked on modern Android).
 * Not exported; the installer gets a temporary read grant on the URI.
 */
class ApkProvider : ContentProvider() {

    companion object {
        fun uriFor(context: android.content.Context): Uri =
            Uri.parse("content://${context.packageName}.apk/update.apk")

        fun apkFile(context: android.content.Context): File =
            File(context.cacheDir, "sgr-update.apk")
    }

    override fun onCreate(): Boolean = true

    private fun fileFor(uri: Uri): File {
        val ctx = context ?: throw FileNotFoundException("no context")
        val f = apkFile(ctx)
        if (!f.exists()) throw FileNotFoundException(uri.toString())
        return f
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor =
        ParcelFileDescriptor.open(fileFor(uri), ParcelFileDescriptor.MODE_READ_ONLY)

    override fun getType(uri: Uri): String = "application/vnd.android.package-archive"

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor {
        val f = fileFor(uri)
        val cols = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val cursor = MatrixCursor(cols)
        val row = cols.map { col ->
            when (col) {
                OpenableColumns.DISPLAY_NAME -> "SmallGameRecordsNL-update.apk"
                OpenableColumns.SIZE -> f.length()
                else -> null
            }
        }
        cursor.addRow(row)
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
