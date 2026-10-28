package com.smallgamerecord.app

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

/**
 * Serves record photos stored under filesDir/record-photos/<recordId>/<photoId>.jpg
 * as content://<applicationId>.photos/<recordId>/<photoId> so the WebView can
 * display them and the camera app (with a per-intent grant) can write a capture.
 * Not exported; access is same-app or via explicit URI grants only.
 */
class PhotoProvider : ContentProvider() {

    private fun baseDir(): File = File(context!!.filesDir, "record-photos")

    private fun fileFor(uri: Uri): File? {
        val segs = uri.pathSegments
        if (segs.size != 2) return null
        val safe = Regex("[A-Za-z0-9_\\-]+")
        if (!segs[0].matches(safe) || !segs[1].matches(safe)) return null
        val base = baseDir()
        val f = File(File(base, segs[0]), segs[1] + ".jpg")
        return if (f.canonicalPath.startsWith(base.canonicalPath)) f else null
    }

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val f = fileFor(uri) ?: throw FileNotFoundException(uri.toString())
        if (mode.contains("w")) {
            f.parentFile?.mkdirs()
            return ParcelFileDescriptor.open(
                f,
                ParcelFileDescriptor.MODE_WRITE_ONLY or
                    ParcelFileDescriptor.MODE_CREATE or
                    ParcelFileDescriptor.MODE_TRUNCATE
            )
        }
        if (!f.exists()) throw FileNotFoundException(uri.toString())
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String = "image/jpeg"
    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?): Int = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?): Int = 0
}
