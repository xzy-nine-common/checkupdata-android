package github.xzynine.checkupdata.cache

import github.xzynine.checkupdata.internal.deleteFileIfExists
import github.xzynine.checkupdata.internal.formatDateTime
import github.xzynine.checkupdata.internal.ioDispatcher
import github.xzynine.checkupdata.internal.joinPath
import github.xzynine.checkupdata.internal.nowMillis
import github.xzynine.checkupdata.internal.optionalLongOrNull
import github.xzynine.checkupdata.internal.optionalStringOrNull
import github.xzynine.checkupdata.internal.parseJsonObject
import github.xzynine.checkupdata.internal.readTextFile
import github.xzynine.checkupdata.internal.prettyJson
import github.xzynine.checkupdata.internal.writeTextFile
import github.xzynine.checkupdata.model.ReleaseInfo
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

object UpdateCache {
    private const val CACHE_DIR = "checkupdata_cache"
    private const val CACHE_FILE = "releases_cache.json"
    private const val CACHE_DURATION = 60L * 60L * 1000L

    suspend fun saveCache(
        cacheRootDir: String,
        owner: String,
        repo: String,
        releases: List<ReleaseInfo>,
        errorLog: String? = null,
    ): Unit =
        withContext(ioDispatcher) {
            try {
                val json: JsonObject =
                    buildJsonObject {
                        put("owner", owner)
                        put("repo", repo)
                        put("cachedAt", nowMillis())
                        putJsonArray("releases") {
                            releases.forEach { release -> add(release.toJson()) }
                        }
                        if (errorLog != null) {
                            put("errorLog", errorLog)
                        }
                    }
                writeTextFile(cacheFilePath(cacheRootDir), prettyJson().encodeToString(JsonObject.serializer(), json))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    suspend fun loadCache(
        cacheRootDir: String,
        owner: String,
        repo: String,
    ): CachedData? =
        withContext(ioDispatcher) {
            try {
                val content = readTextFile(cacheFilePath(cacheRootDir)) ?: return@withContext null
                val json = parseJsonObject(content)
                if (json.optionalStringOrNull("owner") != owner || json.optionalStringOrNull("repo") != repo) {
                    return@withContext null
                }
                val cachedAt = json.optionalLongOrNull("cachedAt") ?: return@withContext null
                if (nowMillis() - cachedAt > CACHE_DURATION) {
                    return@withContext null
                }
                val releasesArray: JsonArray? = json["releases"] as? JsonArray
                val releases: List<ReleaseInfo> =
                    if (releasesArray == null) {
                        emptyList()
                    } else {
                        releasesArray.indices.map { index ->
                            ReleaseInfo.fromJson(releasesArray[index] as JsonObject)
                        }
                    }
                CachedData(
                    owner = owner,
                    repo = repo,
                    cachedAt = cachedAt,
                    releases = releases,
                    errorLog = json.optionalStringOrNull("errorLog"),
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    fun clearCache(cacheRootDir: String) {
        try {
            deleteFileIfExists(cacheFilePath(cacheRootDir))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cacheFilePath(cacheRootDir: String): String = joinPath(joinPath(cacheRootDir, CACHE_DIR), CACHE_FILE)

    data class CachedData(
        val owner: String,
        val repo: String,
        val cachedAt: Long,
        val releases: List<ReleaseInfo>,
        val errorLog: String?,
    ) {
        val cachedAtFormatted: String
            get() = formatDateTime(cachedAt)
    }
}
