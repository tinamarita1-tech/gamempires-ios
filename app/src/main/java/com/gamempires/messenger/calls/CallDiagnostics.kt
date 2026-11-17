package com.gamempires.messenger.calls

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Local, user-exported diagnostics: no credentials, SDP, audio, or push tokens. */
object CallDiagnostics {
    @Synchronized
    fun record(context: Context, event: String) {
        runCatching {
            val file = File(context.filesDir, "call-diagnostics.log")
            if (file.length() > 128 * 1024) file.writeText(file.readText().takeLast(64 * 1024))
            file.appendText("${System.currentTimeMillis()} $event\n")
        }
    }

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            record(context, "UNCAUGHT thread=${thread.name}\n${error.stackTraceToString()}")
            previous?.uncaughtException(thread, error)
        }
    }

    suspend fun export(context: Context): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "gamempires-call-diagnostics.txt")
        file.bufferedWriter().use { out ->
            out.appendLine("GamEmpires call diagnostics; Android ${Build.VERSION.RELEASE}; API ${Build.VERSION.SDK_INT}")
            val local = File(context.filesDir, "call-diagnostics.log")
            if (local.exists()) out.appendLine(local.readText())
            if (Build.VERSION.SDK_INT >= 30) {
                val manager = context.getSystemService(ActivityManager::class.java)
                manager.getHistoricalProcessExitReasons(context.packageName, 0, 3).forEach { exit ->
                    out.appendLine("Previous process exit: time=${exit.timestamp} reason=${exit.reason} status=${exit.status} description=${exit.description}")
                    runCatching { exit.traceInputStream?.use { input ->
                        out.appendLine("Exit trace:")
                        val bytes = ByteArray(256 * 1024)
                        var count = 0
                        while (count < bytes.size) {
                            val read = input.read(bytes, count, bytes.size - count)
                            if (read < 0) break
                            count += read
                        }
                        out.appendLine(String(bytes, 0, count, Charsets.UTF_8))
                    } }
                }
            }
        }
        file
    }
}
