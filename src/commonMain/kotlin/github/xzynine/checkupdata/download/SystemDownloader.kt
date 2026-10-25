package github.xzynine.checkupdata.download

import github.xzynine.checkupdata.internal.DownloadStarter
import github.xzynine.checkupdata.model.ReleaseInfo

/**
 * 发行包下载器。
 *
 * 平台差异经 [starter] 注入：Android 使用系统 DownloadManager，JVM 直接写入缓存目录。
 */
class SystemDownloader internal constructor(
    private val starter: DownloadStarter,
) {
    fun downloadRelease(
        releaseInfo: ReleaseInfo,
        proxyUrl: String = "",
        assetFilter: ((ReleaseInfo.ReleaseAsset) -> Boolean)? = null,
    ): DownloadResult {
        val asset =
            assetFilter?.let { filter ->
                releaseInfo.assets.find(filter)
            } ?: releaseInfo.assets.firstOrNull {
                it.name.endsWith(".apk", ignoreCase = true)
            } ?: releaseInfo.assets.firstOrNull()

        if (asset == null) {
            return DownloadResult.NoAsset(
                message = "No matching asset found",
                availableAssets = releaseInfo.assets.map { it.name },
            )
        }

        val downloadUrl =
            if (proxyUrl.isNotEmpty()) {
                proxyUrl + asset.browserDownloadUrl
            } else {
                asset.browserDownloadUrl
            }

        return startDownload(asset.name, downloadUrl, asset.size)
    }

    private fun startDownload(
        fileName: String,
        url: String,
        fileSize: Long,
    ): DownloadResult =
        try {
            val downloadId = starter.start(fileName, url, fileSize)
            DownloadResult.Success(downloadId, fileName, fileSize)
        } catch (e: Exception) {
            DownloadResult.Error(
                message = "Failed to start download: " + (e.message ?: ""),
                exception = e,
                fileName = fileName,
                url = url,
            )
        }

    sealed class DownloadResult {
        data class Success(
            val downloadId: Long,
            val fileName: String,
            val fileSize: Long,
        ) : DownloadResult()

        data class NoAsset(
            val message: String,
            val availableAssets: List<String>,
        ) : DownloadResult()

        data class Error(
            val message: String,
            val exception: Throwable? = null,
            val fileName: String? = null,
            val url: String? = null,
        ) : DownloadResult()
    }
}
