package com.gamempires.messenger.data.nextcloud

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AttachmentDownloaderTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun `fast transfers do not flood UI with one update per chunk`() = runBlocking {
        val bytes = ByteArray(2 * 1024 * 1024)
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(bytes.toResponseBody()).build()
        }.build()
        val events = mutableListOf<Float>()
        val result = AttachmentDownloader(client, nanoTime = { 0L }).download(
            listOf("https://example.test/file"), folder.newFile(), "application/octet-stream", events::add)
        assertTrue(result.isSuccess)
        assertTrue("Too many UI updates: ${events.size}", events.size <= 4)
        assertEquals(1f, events.last())
    }

    @Test fun `unknown length starts indeterminate and completes normally`() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val body = object : okhttp3.ResponseBody() {
                override fun contentType() = "application/octet-stream".toMediaType()
                override fun contentLength() = -1L
                override fun source() = okio.Buffer().writeUtf8("file-data")
            }
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(body).build()
        }.build()
        val events = mutableListOf<Float>()
        val result = AttachmentDownloader(client).download(
            listOf("https://example.test/file"), folder.newFile(), "application/octet-stream", events::add)
        assertTrue(result.isSuccess)
        assertEquals(-1f, events.first())
        assertEquals(1f, events.last())
    }

    @Test fun `numeric file IDs decoded from rich message JSON remain integer IDs`() {
        assertEquals("123", normalizeAttachmentId(123.0))
        assertEquals("456", normalizeAttachmentId("456"))
    }

    @Test fun `network and file transfer run off the calling thread`() = runBlocking {
        val caller = Thread.currentThread()
        var worker: Thread? = null
        val bytes = byteArrayOf(0, 1, 2, 3, -1)
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            worker = Thread.currentThread()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(bytes.toResponseBody()).build()
        }.build()
        val file = folder.newFile()
        val result = AttachmentDownloader(client).download(listOf("https://example.test/file"), file, "application/pdf") {}
        assertTrue(result.isSuccess)
        assertNotSame(caller, worker)
        assertArrayEquals(bytes, file.readBytes())
    }

    @Test fun `login HTML is rejected and next file URL is tried`() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val html = chain.request().url.encodedPath == "/login"
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(if (html)
                    "<html>Sign in</html>".toResponseBody("text/html".toMediaType())
                    else "%PDF-real-file".toResponseBody("application/pdf".toMediaType())).build()
        }.build()
        val file = folder.newFile()
        val result = AttachmentDownloader(client).download(
            listOf("https://example.test/login", "https://example.test/file"), file, "application/pdf") {}
        assertTrue(result.isSuccess)
        assertEquals("%PDF-real-file", file.readText())
    }

    @Test fun `HTTP failure does not leave a downloaded file`() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(403).message("Forbidden").body("denied".toResponseBody()).build()
        }.build()
        val file = java.io.File(folder.root, "missing.pdf")
        val result = AttachmentDownloader(client).download(listOf("https://example.test/file"), file, "application/pdf") {}
        assertTrue(result.isFailure)
        assertFalse(file.exists())
    }

    @Test fun `extensionless JSON files with generic metadata can be downloaded`() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body("{\"value\":42}".toResponseBody("application/json".toMediaType())).build()
        }.build()
        val file = java.io.File(folder.root, "data")
        val result = AttachmentDownloader(client).download(listOf("https://example.test/file"), file, "application/octet-stream") {}
        assertTrue(result.isSuccess)
        assertEquals("{\"value\":42}", file.readText())
    }
}
