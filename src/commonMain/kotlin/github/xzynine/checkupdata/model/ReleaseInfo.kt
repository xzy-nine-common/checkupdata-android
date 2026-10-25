package github.xzynine.checkupdata.model

import github.xzynine.checkupdata.internal.optionalArrayOrNull
import github.xzynine.checkupdata.internal.optionalBoolean
import github.xzynine.checkupdata.internal.optionalString
import github.xzynine.checkupdata.internal.requireLong
import github.xzynine.checkupdata.internal.requireString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

data class ReleaseInfo(
    val id: Long,
    val version: String,
    val versionTag: String,
    val name: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val publishedAt: String?,
    val isPrerelease: Boolean,
    val isDraft: Boolean,
    val assets: List<ReleaseAsset>,
) {
    data class ReleaseAsset(
        val id: Long,
        val name: String,
        val contentType: String,
        val size: Long,
        val downloadUrl: String,
        val browserDownloadUrl: String,
    ) {
        fun toJson(): JsonObject =
            buildJsonObject {
                put("id", id)
                put("name", name)
                put("contentType", contentType)
                put("size", size)
                put("downloadUrl", downloadUrl)
                put("browserDownloadUrl", browserDownloadUrl)
            }

        companion object {
            fun fromJson(json: JsonObject): ReleaseAsset =
                ReleaseAsset(
                    id = json.requireLong("id"),
                    name = json.requireString("name"),
                    contentType = json.requireString("contentType"),
                    size = json.requireLong("size"),
                    downloadUrl = json.requireString("downloadUrl"),
                    browserDownloadUrl = json.requireString("browserDownloadUrl"),
                )
        }
    }

    fun toJson(): JsonObject =
        buildJsonObject {
            put("id", id)
            put("version", version)
            put("versionTag", versionTag)
            put("name", name)
            put("releaseNotes", releaseNotes)
            put("htmlUrl", htmlUrl)
            if (publishedAt != null) {
                put("publishedAt", publishedAt)
            }
            put("isPrerelease", isPrerelease)
            put("isDraft", isDraft)
            putJsonArray("assets") {
                assets.forEach { asset -> add(asset.toJson()) }
            }
        }

    companion object {
        fun fromJson(json: JsonObject): ReleaseInfo {
            val assetsArray: JsonArray? = json.optionalArrayOrNull("assets")
            val assets: List<ReleaseAsset> =
                if (assetsArray == null) {
                    emptyList()
                } else {
                    assetsArray.indices.map { index -> ReleaseAsset.fromJson(assetsArray[index] as JsonObject) }
                }
            return ReleaseInfo(
                id = json.requireLong("id"),
                version = json.requireString("version"),
                versionTag = json.requireString("versionTag"),
                name = json.requireString("name"),
                releaseNotes = json.requireString("releaseNotes"),
                htmlUrl = json.requireString("htmlUrl"),
                publishedAt = json.optionalString("publishedAt"),
                isPrerelease = json.optionalBoolean("isPrerelease"),
                isDraft = json.optionalBoolean("isDraft"),
                assets = assets,
            )
        }
    }
}
