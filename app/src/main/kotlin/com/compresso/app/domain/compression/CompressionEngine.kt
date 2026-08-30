package com.compresso.app.domain.compression

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.compresso.app.data.model.CompressionMode
import com.compresso.app.data.model.CompressionResult
import com.compresso.app.data.model.MediaKind
import com.compresso.app.util.FileUtils
import java.io.File

sealed class EngineOutcome {
    data class Success(val result: CompressionResult) : EngineOutcome()
    data class Skipped(val reason: String) : EngineOutcome()
    data class Failed(val reason: String) : EngineOutcome()
}

class CompressionEngine(private val context: Context) {

    private val imageCompressor = ImageCompressor()
    private val videoCompressor = VideoCompressor()

    fun process(
        sourceUri: Uri,
        displayName: String,
        kind: MediaKind,
        mode: CompressionMode,
        preserveMetadata: Boolean,
        deleteOriginalWhenDone: Boolean
    ): EngineOutcome {
        val workingDir = FileUtils.workingDirectory(context)
        val outputDir = FileUtils.outputDirectory(context)
        val baseName = FileUtils.nameWithoutExtension(displayName).ifBlank { "file" }
        val localCopy = File(workingDir, "${System.currentTimeMillis()}_$baseName")

        return try {
            FileUtils.copyUriToFile(context, sourceUri, localCopy)

            val outcome: EngineOutcome = when (kind) {
                MediaKind.IMAGE -> when (
                    val r = imageCompressor.compress(
                        context,
                        localCopy,
                        mode,
                        preserveMetadata,
                        outputDir,
                        baseName
                    )
                ) {
                    is ImageCompressionOutcome.Success -> EngineOutcome.Success(r.result)
                    is ImageCompressionOutcome.Skipped -> EngineOutcome.Skipped(r.reason)
                    is ImageCompressionOutcome.Failed -> EngineOutcome.Failed(r.reason)
                }

                MediaKind.VIDEO -> when (
                    val r = videoCompressor.compress(context, localCopy, outputDir, baseName)
                ) {
                    is VideoCompressionOutcome.Success -> EngineOutcome.Success(r.result)
                    is VideoCompressionOutcome.Skipped -> EngineOutcome.Skipped(r.reason)
                    is VideoCompressionOutcome.Failed -> EngineOutcome.Failed(r.reason)
                }

                MediaKind.UNKNOWN -> EngineOutcome.Failed("Unsupported file type")
            }

            if (outcome is EngineOutcome.Success && deleteOriginalWhenDone) {
                tryDeleteOriginal(sourceUri)
            }

            outcome
        } catch (e: OutOfMemoryError) {
            EngineOutcome.Failed("This file is too large to process on this device")
        } catch (e: Exception) {
            EngineOutcome.Failed(e.message ?: "Unexpected error while processing this file")
        } finally {
            localCopy.delete()
        }
    }

    private fun tryDeleteOriginal(uri: Uri) {
        try {
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        } catch (e: Exception) {
        }
    }

    fun resultCanBeSavedToGallery(result: CompressionResult): Boolean {
        return result.container == "PNG" || result.container == "WebP" ||
            result.container == "MP4" || result.container == "WebM"
    }
}
