package github.xzynine.checkupdata

import android.content.Context
import github.xzynine.checkupdata.download.SystemDownloader
import github.xzynine.checkupdata.internal.AndroidDownloadStarter

/**
 * 以 Android Context 构造 [CheckUpdateManager]，缓存目录取应用私有缓存目录。
 */
fun CheckUpdateManager(context: Context): CheckUpdateManager =
    CheckUpdateManager(
        cacheRootDir = context.cacheDir.absolutePath,
        downloader = SystemDownloader(AndroidDownloadStarter(context.applicationContext)),
    )

/**
 * 以 Android Context 构造 [SystemDownloader]，下载经系统 DownloadManager 完成。
 */
fun SystemDownloader(context: Context): SystemDownloader = SystemDownloader(AndroidDownloadStarter(context.applicationContext))
