package com.compresso.app.util

import android.content.ContentResolver
import android.net.Uri
import com.compresso.app.data.model.MediaKind

object MediaTypeDetector {

    val supportedImageMimeTypes = setOf(
        "image/png",
        "image/jpeg",
        "image/webp",
        "image/bmp",
        "image/gif",
        "image/heic",
        "image/heif",
        "image/x-ms-bmp"
    )

    val supportedVideoMimeTypes = setOf(
        "video/mp4",
        "video/webm",
        "video/x-matroska",
        "video/3gpp",
        "video/quicktime",
        "video/avi",
        "video/x-msvideo",
        "video/mpeg"
    )

    fun resolveMimeType(resolver: ContentResolver, uri: Uri, fallback: String?): String {
        return resolver.getType(uri) ?: fallback ?: "application/octet-stream"
    }

    fun kindOf(mimeType: String): MediaKind {
        return when {
            mimeType.startsWith("image/") -> MediaKind.IMAGE
            mimeType.startsWith("video/") -> MediaKind.VIDEO
            else -> MediaKind.UNKNOWN
        }
    }

    fun isSupported(mimeType: String): Boolean {
        return mimeType in supportedImageMimeTypes || mimeType in supportedVideoMimeTypes
    }

    fun pickerMimeTypes(): Array<String> {
        return (supportedImageMimeTypes + supportedVideoMimeTypes).toTypedArray()
    }
}
