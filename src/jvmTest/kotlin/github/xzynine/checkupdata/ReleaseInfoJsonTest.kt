package github.xzynine.checkupdata

import github.xzynine.checkupdata.model.ReleaseInfo
import github.xzynine.checkupdata.version.VersionComparator
import github.xzynine.checkupdata.version.VersionRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReleaseInfoJsonTest {
    private val sample =
        ReleaseInfo(
            id = 42L,
            version = "1.2.3",
            versionTag = "v1.2.3",
            name = "Release",
            releaseNotes = "notes",
            htmlUrl = "https://example.com/r/42",
            publishedAt = "2026-01-02T03:04:05Z",
            isPrerelease = false,
            isDraft = false,
            assets =
                listOf(
                    ReleaseInfo.ReleaseAsset(
                        id = 7L,
                        name = "app.apk",
                        contentType = "application/vnd.android.package-archive",
                        size = 1234L,
                        downloadUrl = "https://api.example.com/a/7",
                        browserDownloadUrl = "https://example.com/a/7",
                    ),
                ),
        )

    @Test
    fun jsonRoundTrip() {
        val json = sample.toJson()
        assertEquals(42L, (json["id"] as kotlinx.serialization.json.JsonPrimitive).content.toLong())
        val restored = ReleaseInfo.fromJson(json)
        assertEquals(sample, restored)
    }

    @Test
    fun nullPublishedAtOmitted() {
        val json = sample.copy(publishedAt = null).toJson()
        assertNull(json["publishedAt"])
        // 与原 optString 语义一致：缺失键回到空串
        assertEquals("", ReleaseInfo.fromJson(json).publishedAt)
    }

    @Test
    fun versionComparison() {
        assertTrue(VersionComparator.isNewer("1.2.3", "1.2.4"))
        assertTrue(VersionComparator.isNewer("1.2.3", "1.10.0"))
        assertFalse(VersionComparator.isNewer("2.0.0", "1.9.9"))
        assertEquals(0, VersionComparator.compare("v1.2.3", "1.2.3"))
    }

    @Test
    fun latestReleaseSelection() {
        val older = sample.copy(id = 1, version = "1.0.0", isPrerelease = false)
        val newer = sample.copy(id = 2, version = "1.1.0", isPrerelease = false)
        val beta = sample.copy(id = 3, version = "2.0.0-beta.1", isPrerelease = true)
        val found = VersionComparator.findLatestRelease(listOf(older, newer, beta), "1.0.0", VersionRule.STABLE)
        assertEquals(newer, found)
    }

    @Test
    fun prereleaseFiltering() {
        val beta = sample.copy(id = 3, version = "2.0.0-beta.1", isPrerelease = true)
        assertEquals(beta, VersionComparator.getLatestReleaseInfo(listOf(beta), VersionRule.PRERELEASE))
        assertNull(VersionComparator.getLatestReleaseInfo(listOf(beta), VersionRule.STABLE))
    }
}
