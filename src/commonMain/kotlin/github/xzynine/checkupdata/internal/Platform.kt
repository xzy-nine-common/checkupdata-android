package github.xzynine.checkupdata.internal

import kotlinx.coroutines.CoroutineDispatcher

internal expect val ioDispatcher: CoroutineDispatcher

internal expect fun nowMillis(): Long

internal expect fun formatDateTime(epochMillis: Long): String

internal expect suspend fun httpGetText(
    url: String,
    headers: Map<String, String>,
    connectTimeoutMs: Long,
    readTimeoutMs: Long,
): String?

internal expect fun readTextFile(path: String): String?

internal expect fun writeTextFile(
    path: String,
    text: String,
): Boolean

internal expect fun deleteFileIfExists(path: String): Boolean

internal expect fun makeDirectories(path: String): Boolean

internal expect fun joinPath(
    parent: String,
    child: String,
): String

fun interface DownloadStarter {
    fun start(
        fileName: String,
        url: String,
        fileSize: Long,
    ): Long
}
