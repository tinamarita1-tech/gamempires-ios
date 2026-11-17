package com.gamempires.messenger.data.nextcloud

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal fun normalizeAttachmentId(value: Any?): String = when (value) {
    is Number -> value.toLong().toString()
    else -> value?.toString().orEmpty()
}

internal class AttachmentDownloader(
    private val client: OkHttpClient,
    private val nanoTime: () -> Long = System::nanoTime,
    private val report: (String) -> Unit = {}
) {
    suspend fun download(urls: List<String>, target: File, mimeType: String, onProgress: (Float) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        var error: Exception = IOException("No download URL available")
        for ((attempt, url) in urls.mapNotNull { it.toHttpUrlOrNull() }.distinct().withIndex()) {
            var partial: File? = null
            val started = nanoTime()
            var headersAt = started
            var status = 0
            var bytes = 0L
            try {
                currentCoroutineContext().ensureActive()
                onProgress(-1f) // Waiting for headers, or the server has not supplied a length.
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    headersAt = nanoTime()
                    status = response.code
                    if (!response.isSuccessful) throw IOException("Server returned HTTP ${response.code}")
                    val body = response.body
                    val receivedType = body.contentType()?.let { "${it.type}/${it.subtype}" }
                    if (receivedType in setOf("text/html", "application/xhtml+xml", "application/json", "application/xml", "text/xml") &&
                        (receivedType in setOf("text/html", "application/xhtml+xml") ||
                            mimeType.isNotBlank() && mimeType != "application/octet-stream") &&
                        !receivedType.equals(mimeType.substringBefore(';'), ignoreCase = true) &&
                        target.extension.lowercase() !in when (receivedType) {
                            "text/html", "application/xhtml+xml" -> setOf("html", "htm", "xhtml")
                            "application/json" -> setOf("json")
                            else -> setOf("xml")
                        }) {
                        throw IOException("Server returned a page or API response instead of the file")
                    }
                    target.parentFile?.mkdirs()
                    val staging = File.createTempFile("attachment-", ".part", target.absoluteFile.parentFile)
                    partial = staging
                    var downloaded = 0L
                    val length = body.contentLength()
                    var lastProgressTime = nanoTime()
                    if (length > 0) onProgress(0f)
                    body.byteStream().use { input -> staging.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count == -1) break
                            output.write(buffer, 0, count)
                            downloaded += count
                            bytes = downloaded
                            val now = nanoTime()
                            if (length > 0 && now - lastProgressTime >= 100_000_000L) {
                                onProgress((downloaded.toFloat() / length).coerceIn(0f, 0.99f))
                                lastProgressTime = now
                            }
                        }
                    } }
                    if (length >= 0 && downloaded != length) throw IOException("Incomplete file download")
                    currentCoroutineContext().ensureActive()
                    Files.move(staging.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    onProgress(1f)
                    return@withContext Result.success(target)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) { error = e }
            finally {
                partial?.delete()
                val finished = nanoTime()
                report("attempt=${attempt + 1} host=${url.host} status=$status bytes=$bytes " +
                    "headersMs=${(headersAt - started) / 1_000_000} " +
                    "bodyMs=${(finished - headersAt) / 1_000_000}")
            }
        }
        Result.failure(error)
    }
}
