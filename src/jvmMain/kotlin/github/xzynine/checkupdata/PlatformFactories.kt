package github.xzynine.checkupdata

import github.xzynine.checkupdata.download.SystemDownloader
import github.xzynine.checkupdata.internal.JvmDownloadStarter

/**
 * 以缓存根目录构造 [CheckUpdateManager]。
 */
fun CheckUpdateManager(cacheRootDir: String): CheckUpdateManager =
    CheckUpdateManager(
        cacheRootDir = cacheRootDir,
        downloader = SystemDownloader(JvmDownloadStarter(cacheRootDir)),
    )

/**
 * 以缓存根目录构造 [SystemDownloader]。
 */
fun SystemDownloader(cacheRootDir: String): SystemDownloader = SystemDownloader(JvmDownloadStarter(cacheRootDir))
