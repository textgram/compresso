package com.compresso.app.domain.compression

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.core.content.FileProvider
import com.compresso.app.data.model.CompressionResult
import com.compresso.app.util.FileUtils
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest

sealed class VideoCompressionOutcome {
    data class Success(val result: CompressionResult) : VideoCompressionOutcome()
    data class Skipped(val reason: String) : VideoCompressionOutcome()
    data class Failed(val reason: String) : VideoCompressionOutcome()
}

class VideoCompressor {

    private data class TrackInfo(val index: Int, val mime: String, val width: Int, val height: Int)
    private data class ContainerCandidate(val muxerFormat: Int, val name: String, val extension: String)

    fun compress(
        context: Context,
        sourceFile: File,
        outputDir: File,
        baseName: String
    ): VideoCompressionOutcome {
        val startedAt = System.currentTimeMillis()

        val tracks = try {
            inspectTracks(sourceFile)
        } catch (e: Exception) {
            return VideoCompressionOutcome.Failed("Couldn't read this video's tracks")
        }

        if (tracks.isEmpty()) {
            return VideoCompressionOutcome.Failed("No readable audio or video tracks found")
        }

        val candidates = candidateContainers(tracks)
        if (candidates.isEmpty()) {
            return VideoCompressionOutcome.Skipped("No compatible lossless container for these codecs")
        }

        var bestFile: File? = null
        var bestContainerName = ""

        for (candidate in candidates) {
            val candidateFile = FileUtils.uniqueFile(
                outputDir,
                baseName + "_" + candidate.name.lowercase(),
                candidate.extension
            )

            val remuxed = try {
                remux(sourceFile, candidateFile, candidate.muxerFormat)
            } catch (e: Exception) {
                false
            }

            if (!remuxed) {
                candidateFile.delete()
                continue
            }

            val verified = try {
                verifyStreamsMatch(sourceFile, candidateFile)
            } catch (e: Exception) {
                false
            }

            if (!verified) {
                candidateFile.delete()
                continue
            }

            val current = bestFile
            if (current == null || candidateFile.length() < current.length()) {
                current?.delete()
                bestFile = candidateFile
                bestContainerName = candidate.name
            } else {
                candidateFile.delete()
            }
        }

        val finalFile = bestFile
            ?: return VideoCompressionOutcome.Failed("No verified lossless remux could be produced")

        if (finalFile.length() >= sourceFile.length()) {
            finalFile.delete()
            return VideoCompressionOutcome.Skipped("Already optimally packaged")
        }

        val outputUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            finalFile
        )

        val videoTrack = tracks.firstOrNull { it.mime.startsWith("video/") }

        return VideoCompressionOutcome.Success(
            CompressionResult(
                outputUri = outputUri,
                outputDisplayName = finalFile.name,
                originalSize = sourceFile.length(),
                compressedSize = finalFile.length(),
                codec = tracks.joinToString(", ") { it.mime.substringAfter("/") },
                container = bestContainerName,
                method = "Verified stream-copy remux",
                durationMs = System.currentTimeMillis() - startedAt,
                verified = true,
                metadataPreserved = true,
                resolution = videoTrack?.let { "${it.width}x${it.height}" },
                colorDepth = null
            )
        )
    }

    private fun inspectTracks(file: File): List<TrackInfo> {
        val extractor = MediaExtractor()
        val infos = mutableListOf<TrackInfo>()
        try {
            extractor.setDataSource(file.absolutePath)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (!mime.startsWith("video/") && !mime.startsWith("audio/")) continue
                val width = if (format.containsKey(MediaFormat.KEY_WIDTH)) format.getInteger(MediaFormat.KEY_WIDTH) else 0
                val height = if (format.containsKey(MediaFormat.KEY_HEIGHT)) format.getInteger(MediaFormat.KEY_HEIGHT) else 0
                infos.add(TrackInfo(i, mime, width, height))
            }
        } finally {
            extractor.release()
        }
        return infos
    }

    private fun candidateContainers(tracks: List<TrackInfo>): List<ContainerCandidate> {
        val results = mutableListOf<ContainerCandidate>()
        results.add(ContainerCandidate(MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4, "MP4", "mp4"))

        val webmCompatible = tracks.isNotEmpty() && tracks.all { info ->
            info.mime == "video/x-vnd.on2.vp8" ||
                info.mime == "video/x-vnd.on2.vp9" ||
                info.mime == "audio/vorbis" ||
                info.mime == "audio/opus"
        }
        if (webmCompatible) {
            results.add(ContainerCandidate(MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM, "WebM", "webm"))
        }
        return results
    }

    private fun remux(source: File, destination: File, muxerFormat: Int): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(source.absolutePath)
            muxer = MediaMuxer(destination.absolutePath, muxerFormat)

            val indexMap = HashMap<Int, Int>()
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    indexMap[i] = muxer.addTrack(format)
                    extractor.selectTrack(i)
                }
            }

            if (indexMap.isEmpty()) return false

            muxer.start()

            val buffer = ByteBuffer.allocateDirect(4 * 1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val srcTrack = extractor.sampleTrackIndex
                val dstTrack = indexMap[srcTrack]
                if (dstTrack == null) {
                    extractor.advance()
                    continue
                }

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags

                muxer.writeSampleData(dstTrack, buffer, bufferInfo)
                extractor.advance()
            }

            muxer.stop()
            return true
        } finally {
            try {
                muxer?.release()
            } catch (e: Exception) {
            }
            extractor.release()
        }
    }

    private fun verifyStreamsMatch(source: File, output: File): Boolean {
        val sourceDigests = digestPerTrack(source) ?: return false
        val outputDigests = digestPerTrack(output) ?: return false
        if (sourceDigests.size != outputDigests.size) return false
        val sourceValues = sourceDigests.toSortedMap().values.toList()
        val outputValues = outputDigests.toSortedMap().values.toList()
        return sourceValues == outputValues
    }

    private fun digestPerTrack(file: File): Map<Int, String>? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            val digests = HashMap<Int, MessageDigest>()
            for (i in 0 until extractor.trackCount) {
                extractor.selectTrack(i)
                digests[i] = MessageDigest.getInstance("SHA-256")
            }
            if (digests.isEmpty()) return null

            val buffer = ByteBuffer.allocateDirect(4 * 1024 * 1024)
            val bytes = ByteArray(4 * 1024 * 1024)
            while (true) {
                buffer.clear()
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                val track = extractor.sampleTrackIndex
                buffer.get(bytes, 0, size)
                digests[track]?.update(bytes, 0, size)
                extractor.advance()
            }
            digests.mapValues { (_, md) -> md.digest().joinToString("") { b -> "%02x".format(b) } }
        } catch (e: Exception) {
            null
        } finally {
            extractor.release()
        }
    }
}
