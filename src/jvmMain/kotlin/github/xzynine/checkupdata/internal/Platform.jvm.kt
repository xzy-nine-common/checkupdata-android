package github.xzynine.checkupdata.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

internal actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

internal actual fun nowMillis(): Long = System.currentTimeMillis()

internal actual fun formatDateTime(epochMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(epochMillis))

internal actual suspend fun httpGetText(
    url: String,
    headers: Map<String, String>,
    connectTimeoutMs: Long,
    readTimeoutMs: Long,
): String? {
    val client =
        OkHttpClient.Builder()
            .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .build()
    val builder = Request.Builder().url(url)
    headers.forEach { (name, value) -> builder.header(name, value) }
    val request = builder.build()
    return try {
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body.string() else null
        }
    } catch (e: Exception) {
        null
    }
}

internal actual fun readTextFile(path: String): String? =
    try {
        val file = File(path)
        if (!file.exists()) null else file.readText()
    } catch (e: Exception) {
        null
    }

internal actual fun writeTextFile(
    path: String,
    text: String,
): Boolean =
    try {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(text)
        true
    } catch (e: Exception) {
        false
    }

internal actual fun deleteFileIfExists(path: String): Boolean =
    try {
        val file = File(path)
        if (file.exists()) file.delete() else true
    } catch (e: Exception) {
        false
    }

internal actual fun makeDirectories(path: String): Boolean =
    try {
        val dir = File(path)
        if (dir.exists()) dir.isDirectory else dir.mkdirs()
    } catch (e: Exception) {
        false
    }

internal actual fun joinPath(
    parent: String,
    child: String,
): String = File(parent, child).path
