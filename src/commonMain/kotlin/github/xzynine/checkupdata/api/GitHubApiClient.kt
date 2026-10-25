package github.xzynine.checkupdata.api

import github.xzynine.checkupdata.internal.httpGetText
import github.xzynine.checkupdata.internal.optionalBoolean
import github.xzynine.checkupdata.internal.optionalLongOrNull
import github.xzynine.checkupdata.internal.optionalStringOrNull
import github.xzynine.checkupdata.internal.parseJsonArray
import github.xzynine.checkupdata.internal.parseJsonObject
import github.xzynine.checkupdata.model.CheckUpdateConfig
import github.xzynine.checkupdata.model.ReleaseInfo
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

class GitHubApiClient(
    private val config: CheckUpdateConfig,
) {
    suspend fun getLatestRelease(): ReleaseInfo? {
        val url = API_BASE_URL + "/repos/" + config.owner + "/" + config.repo + "/releases/latest"
        val body = fetch(url) ?: return null
        return parseRelease(body)
    }

    suspend fun getReleases(): List<ReleaseInfo> {
        val url = API_BASE_URL + "/repos/" + config.owner + "/" + config.repo + "/releases"
        val body = fetch(url) ?: return emptyList()
        return parseReleases(body)
    }

    private suspend fun fetch(url: String): String? =
        try {
            httpGetText(
                url = url,
                headers = buildHeaders(),
                connectTimeoutMs = config.connectTimeout,
                readTimeoutMs = config.readTimeout,
            )
        } catch (e: Exception) {
            null
        }

    private fun buildHeaders(): Map<String, String> {
        val headers = mutableMapOf("Accept" to "application/vnd.github.v3+json")
        val token = config.githubToken
        if (token != null && token.isNotEmpty()) {
            headers["Authorization"] = "token " + token
        }
        return headers
    }

    private fun parseRelease(json: String): ReleaseInfo? =
        try {
            parseReleaseFromJson(parseJsonObject(json))
        } catch (e: Exception) {
            null
        }

    private fun parseReleases(json: String): List<ReleaseInfo> =
        try {
            val array = parseJsonArray(json)
            array.indices.mapNotNull { index ->
                val obj = array[index] as? JsonObject ?: return@mapNotNull null
                parseReleaseFromJson(obj)
            }
        } catch (e: Exception) {
            emptyList()
        }

    private fun parseReleaseFromJson(obj: JsonObject): ReleaseInfo? =
        try {
            val id = obj.optionalLongOrNull("id") ?: return null
            val tagName = obj.optionalStringOrNull("tag_name") ?: return null
            val name = obj.optionalStringOrNull("name") ?: tagName
            val body = obj.optionalStringOrNull("body") ?: ""
            val htmlUrl = obj.optionalStringOrNull("html_url") ?: ""
            val publishedAt = obj.optionalStringOrNull("published_at")
            val isPrerelease = obj.optionalBoolean("prerelease")
            val isDraft = obj.optionalBoolean("draft")
            val assets = parseAssets(obj)

            ReleaseInfo(
                id = id,
                version = cleanVersion(tagName),
                versionTag = tagName,
                name = name,
                releaseNotes = body,
                htmlUrl = htmlUrl,
                publishedAt = publishedAt,
                isPrerelease = isPrerelease,
                isDraft = isDraft,
                assets = assets,
            )
        } catch (e: Exception) {
            null
        }

    private fun parseAssets(obj: JsonObject): List<ReleaseInfo.ReleaseAsset> {
        val assetsArray: JsonArray = obj["assets"] as? JsonArray ?: return emptyList()
        return assetsArray.indices.mapNotNull { index ->
            val assetObj = assetsArray[index] as? JsonObject ?: return@mapNotNull null
            val assetName = assetObj.optionalStringOrNull("name") ?: return@mapNotNull null
            try {
                ReleaseInfo.ReleaseAsset(
                    id = assetObj.optionalLongOrNull("id") ?: 0L,
                    name = assetName,
                    contentType = assetObj.optionalStringOrNull("content_type") ?: "",
                    size = assetObj.optionalLongOrNull("size") ?: 0L,
                    downloadUrl = assetObj.optionalStringOrNull("url") ?: "",
                    browserDownloadUrl = assetObj.optionalStringOrNull("browser_download_url") ?: "",
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun cleanVersion(tagName: String): String {
        var version = tagName
        if (version.startsWith("v", ignoreCase = true)) {
            version = version.substring(1)
        }
        return version
    }

    private companion object {
        private const val API_BASE_URL = "https://api.github.com"
    }
}
