package github.xzynine.checkupdata

import github.xzynine.checkupdata.api.GitHubApiService
import github.xzynine.checkupdata.cache.UpdateCache
import github.xzynine.checkupdata.download.SystemDownloader
import github.xzynine.checkupdata.internal.formatDateTime
import github.xzynine.checkupdata.internal.ioDispatcher
import github.xzynine.checkupdata.internal.nowMillis
import github.xzynine.checkupdata.model.CheckUpdateConfig
import github.xzynine.checkupdata.model.ReleaseInfo
import github.xzynine.checkupdata.model.UpdateResult
import github.xzynine.checkupdata.version.VersionComparator
import github.xzynine.checkupdata.version.VersionRule
import kotlinx.coroutines.withContext

/**
 * 更新检查入口。
 *
 * 平台差异经 [cacheRootDir] 与 [downloader] 注入，逻辑全部位于公共源集。
 * 各平台在自身源集提供同名工厂函数以保持既有构造形式。
 */
class CheckUpdateManager internal constructor(
    private val cacheRootDir: String,
    private val downloader: SystemDownloader,
) {
    private val logBuilder = StringBuilder()

    private fun log(message: String) {
        val timestamp = formatDateTime(nowMillis())
        logBuilder.append("[").append(timestamp).append("] ").append(message).append("\n")
    }

    private fun getAndClearLog(): String {
        val log = logBuilder.toString()
        logBuilder.clear()
        return log
    }

    suspend fun checkUpdate(
        owner: String,
        repo: String,
        currentVersion: String,
        rule: VersionRule = VersionRule.STABLE,
        githubToken: String? = null,
        useCache: Boolean = true,
    ): UpdateResult =
        withContext(ioDispatcher) {
            log("开始检查更新: owner=" + owner + ", repo=" + repo + ", rule=" + rule)
            log("当前版本: " + currentVersion)

            if (useCache) {
                val cached = UpdateCache.loadCache(cacheRootDir, owner, repo)
                if (cached != null) {
                    log("使用缓存数据 (缓存时间: " + cached.cachedAtFormatted + ")")
                    return@withContext processReleases(
                        cached.releases,
                        currentVersion,
                        rule,
                        cached.errorLog,
                    )
                }
            }

            try {
                val config =
                    CheckUpdateConfig(
                        owner = owner,
                        repo = repo,
                        currentVersion = currentVersion,
                        githubToken = githubToken,
                    )

                val apiService = GitHubApiService.create(config)

                val releases =
                    when (rule) {
                        VersionRule.STABLE -> {
                            log("获取最新稳定版本...")
                            val latest = apiService.getLatestRelease()
                            if (latest != null) {
                                log("获取到最新版本: " + latest.version)
                                listOf(latest)
                            } else {
                                log("未获取到最新版本")
                                emptyList()
                            }
                        }
                        else -> {
                            log("获取所有版本列表...")
                            val all = apiService.getReleases()
                            log("获取到 " + all.size + " 个版本")
                            all
                        }
                    }

                if (releases.isEmpty()) {
                    log("错误: 未找到任何版本")
                    UpdateResult.Error("No releases found", errorLog = getAndClearLog())
                } else {
                    val filteredReleases =
                        when (rule) {
                            VersionRule.STABLE -> releases.filter { !it.isPrerelease && !it.isDraft }
                            VersionRule.LATEST -> releases.filter { !it.isDraft }
                            VersionRule.PRERELEASE -> releases.filter { it.isPrerelease && !it.isDraft }
                        }
                    log("过滤后剩余 " + filteredReleases.size + " 个版本")

                    UpdateCache.saveCache(cacheRootDir, owner, repo, filteredReleases)

                    processReleases(filteredReleases, currentVersion, rule, null)
                }
            } catch (e: Exception) {
                log("错误: " + (e.message ?: ""))
                e.printStackTrace()
                UpdateResult.Error(
                    message = e.message ?: "Unknown error",
                    exception = e,
                    errorLog = getAndClearLog(),
                )
            }
        }

    private fun processReleases(
        releases: List<ReleaseInfo>,
        currentVersion: String,
        rule: VersionRule,
        cachedErrorLog: String?,
    ): UpdateResult {
        val latestRelease =
            VersionComparator.findLatestRelease(
                releases = releases,
                currentVersion = currentVersion,
                rule = rule,
            )

        val errorLog = cachedErrorLog ?: getAndClearLog()

        return if (latestRelease != null) {
            log("发现新版本: " + latestRelease.version)
            UpdateResult.HasUpdate(
                releaseInfo = latestRelease,
                currentVersion = currentVersion,
                allReleases = releases,
                errorLog = errorLog,
            )
        } else {
            val remoteReleaseInfo = VersionComparator.getLatestReleaseInfo(releases, rule)
            val remoteVersion = remoteReleaseInfo?.version ?: "unknown"
            log("当前已是最新版本，远端版本: " + remoteVersion)
            UpdateResult.NoUpdate(
                currentVersion = currentVersion,
                remoteVersion = remoteVersion,
                releaseInfo = remoteReleaseInfo,
                allReleases = releases,
                errorLog = errorLog,
            )
        }
    }

    fun downloadRelease(
        releaseInfo: ReleaseInfo,
        proxyUrl: String = "",
        assetFilter: ((ReleaseInfo.ReleaseAsset) -> Boolean)? = null,
    ): SystemDownloader.DownloadResult = downloader.downloadRelease(releaseInfo, proxyUrl, assetFilter)

    fun clearCache() {
        UpdateCache.clearCache(cacheRootDir)
    }
}
