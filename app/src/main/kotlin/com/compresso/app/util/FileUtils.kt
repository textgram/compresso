package com.compresso.app.util

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    data class DocumentInfo(val displayName: String, val size: Long)

    fun queryDocumentInfo(resolver: ContentResolver, uri: Uri): DocumentInfo {
        var name = uri.lastPathSegment ?: "file"
        var size = -1L
        var cursor: Cursor? = null
        try {
            cursor = resolver.query(uri, null, null, null, null)
            cursor?.let {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) {
                        it.getString(nameIndex)?.let { n -> name = n }
                    }
                    if (sizeIndex >= 0 && !it.isNull(sizeIndex)) {
                        size = it.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            cursor?.close()
        }
        if (size < 0) {
            size = try {
                resolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
            } catch (_: Exception) {
                0L
            }
        }
        return DocumentInfo(name, size)
    }

    fun copyUriToFile(context: Context, uri: Uri, destination: File): Long {
        var bytesCopied = 0L
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destination).use { output ->
                bytesCopied = input.copyTo(output)
            }
        } ?: throw IllegalStateException("Unable to open input stream for $uri")
        return bytesCopied
    }

    fun workingDirectory(context: Context): File {
        val dir = File(context.cacheDir, "compression_work")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun outputDirectory(context: Context): File {
        val dir = File(context.filesDir, "compressed_output")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun nameWithoutExtension(name: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot > 0) name.substring(0, dot) else name
    }

    fun extensionOf(name: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot >= 0 && dot < name.length - 1) name.substring(dot + 1).lowercase() else ""
    }

    fun uniqueFile(directory: File, baseName: String, extension: String): File {
        var candidate = File(directory, "$baseName.$extension")
        var counter = 1
        while (candidate.exists()) {
            candidate = File(directory, "$baseName-$counter.$extension")
            counter++
        }
        return candidate
    }
}
