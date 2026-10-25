package github.xzynine.checkupdata.internal

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

internal class AndroidDownloadStarter(
    private val context: Context,
) : DownloadStarter {
    private val downloadManager: DownloadManager by lazy {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }

    override fun start(
        fileName: String,
        url: String,
        fileSize: Long,
    ): Long {
        val request =
            DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(fileName)
                setDescription(url)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
        return downloadManager.enqueue(request)
    }
}
