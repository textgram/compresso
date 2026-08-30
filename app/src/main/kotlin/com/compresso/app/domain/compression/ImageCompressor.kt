package com.compresso.app.domain.compression

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Movie
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import androidx.core.content.FileProvider
import com.compresso.app.data.model.CompressionMode
import com.compresso.app.data.model.CompressionResult
import com.compresso.app.domain.metadata.ExifPreserver
import com.compresso.app.util.FileUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

sealed class ImageCompressionOutcome {
    data class Success(val result: CompressionResult) : ImageCompressionOutcome()
    data class Skipped(val reason: String) : ImageCompressionOutcome()
    data class Failed(val reason: String) : ImageCompressionOutcome()
}

class ImageCompressor {

    private data class Candidate(
        val bytes: ByteArray,
        val label: String,
        val container: String
    )

    fun compress(
        context: Context,
        sourceFile: File,
        mode: CompressionMode,
        preserveMetadata: Boolean,
        outputDir: File,
        baseName: String
    ): ImageCompressionOutcome {
        val startedAt = System.currentTimeMillis()

        if (isAnimated(sourceFile)) {
            return ImageCompressionOutcome.Skipped("Animated images aren't compressed yet to avoid losing frames")
        }

        val bitmap = decodeBitmap(sourceFile)
            ?: return ImageCompressionOutcome.Failed("Couldn't decode this image")

        try {
            val candidates = buildCandidates(bitmap, mode)
            if (candidates.isEmpty()) {
                return ImageCompressionOutcome.Skipped("No lossless encoder available on this device for this format")
            }

            val verified = candidates.filter { verifyLossless(it.bytes, bitmap) }
            if (verified.isEmpty()) {
                return ImageCompressionOutcome.Failed("Every candidate failed bit-perfect verification")
            }

            val best = verified.minByOrNull { it.bytes.size }!!

            if (best.bytes.size.toLong() >= sourceFile.length()) {
                return ImageCompressionOutcome.Skipped("Already optimally compressed")
            }

            val extension = if (best.container == "WebP") "webp" else "png"
            val outFile = FileUtils.uniqueFile(outputDir, baseName, extension)
            FileOutputStream(outFile).use { it.write(best.bytes) }

            var metadataPreserved = false
            if (preserveMetadata) {
                metadataPreserved = ExifPreserver.copy(sourceFile, outFile)
            }

            val outputUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )

            val methodLabel = if (mode == CompressionMode.EXTREME) {
                "Extreme, ${candidates.size} candidates tried"
            } else {
                "Standard"
            }

            return ImageCompressionOutcome.Success(
                CompressionResult(
                    outputUri = outputUri,
                    outputDisplayName = outFile.name,
                    originalSize = sourceFile.length(),
                    compressedSize = outFile.length(),
                    codec = best.label,
                    container = best.container,
                    method = methodLabel,
                    durationMs = System.currentTimeMillis() - startedAt,
                    verified = true,
                    metadataPreserved = metadataPreserved,
                    resolution = "${bitmap.width}x${bitmap.height}",
                    colorDepth = colorDepthLabel(bitmap)
                )
            )
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun decodeBitmap(sourceFile: File): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(sourceFile)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = false
                }
            } else {
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeFile(sourceFile.absolutePath, options)
            }
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun isAnimated(sourceFile: File): Boolean {
        val viaMovie = try {
            val movie = Movie.decodeFile(sourceFile.absolutePath)
            movie != null && movie.duration() > 0
        } catch (e: Exception) {
            false
        }
        if (viaMovie) return true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return try {
                val source = ImageDecoder.createSource(sourceFile)
                val drawable = ImageDecoder.decodeDrawable(source)
                drawable is AnimatedImageDrawable
            } catch (e: Exception) {
                false
            }
        }
        return false
    }

    private fun buildCandidates(bitmap: Bitmap, mode: CompressionMode): List<Candidate> {
        val results = mutableListOf<Candidate>()

        try {
            val pngOut = ByteArrayOutputStream()
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, pngOut)) {
                results.add(Candidate(pngOut.toByteArray(), "PNG (Deflate, filtered)", "PNG"))
            }
        } catch (e: Exception) {
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val effort = if (mode == CompressionMode.EXTREME) 100 else 87
                val webpOut = ByteArrayOutputStream()
                if (bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, effort, webpOut)) {
                    results.add(Candidate(webpOut.toByteArray(), "WebP Lossless (effort $effort)", "WebP"))
                }
            } catch (e: Exception) {
            }
        }

        return results
    }

    private fun verifyLossless(bytes: ByteArray, original: Bitmap): Boolean {
        val decoded = try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        } ?: return false

        try {
            if (decoded.width != original.width || decoded.height != original.height) return false
            return bitmapContentEquals(original, decoded)
        } finally {
            if (!decoded.isRecycled) decoded.recycle()
        }
    }

    private fun bitmapContentEquals(a: Bitmap, b: Bitmap): Boolean {
        val width = a.width
        val height = a.height
        val rowA = IntArray(width)
        val rowB = IntArray(width)
        for (y in 0 until height) {
            a.getPixels(rowA, 0, width, 0, y, width, 1)
            b.getPixels(rowB, 0, width, 0, y, width, 1)
            if (!rowA.contentEquals(rowB)) return false
        }
        return true
    }

    private fun colorDepthLabel(bitmap: Bitmap): String {
        return when (bitmap.config) {
            Bitmap.Config.ARGB_8888 -> "8-bit per channel + alpha"
            Bitmap.Config.RGBA_F16 -> "16-bit float HDR"
            Bitmap.Config.RGB_565 -> "16-bit (565)"
            Bitmap.Config.ALPHA_8 -> "8-bit alpha"
            else -> "Unknown"
        }
    }
}
