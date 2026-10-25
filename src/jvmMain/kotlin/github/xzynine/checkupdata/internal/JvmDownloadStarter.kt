package github.xzynine.checkupdata.internal

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * JVM 侧下载：同步拉取到缓存目录下的 downloads 目录。
 * 返回的 downloadId 在该平台无系统语义，固定为 0。
 */
internal class JvmDownloadStarter(
    private val cacheRootDir: String,
) : DownloadStarter {
    override fun start(
        fileName: String,
        url: String,
        fileSize: Long,
    ): Long {
        val dir = File(cacheRootDir, "downloads")
        dir.mkdirs()
        val target = File(dir, fileName)
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP " + response.code + " for " + url)
            }
            target.outputStream().use { output ->
                response.body.byteStream().use { input -> input.copyTo(output) }
            }
        }
        return 0L
    }
}
