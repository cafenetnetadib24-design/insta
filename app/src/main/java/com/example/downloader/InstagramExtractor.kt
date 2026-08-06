package com.example.downloader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class InstagramExtractor {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val userAgents = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1",
        "Instagram 320.0.0.33.109 Android (33/13; 420dpi; 1080x2220; Samsung; SM-G998B)"
    )

    companion object {
        fun extractUrlFromText(text: String): String {
            if (text.isBlank()) return ""
            val pattern = Pattern.compile("(https?://[\\w\\d.-]*instagram\\.com/[^\\s\"'<>]+)", Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1) ?: text.trim()
            }
            val generalUrlPattern = Pattern.compile("(https?://[^\\s\"'<>]+)", Pattern.CASE_INSENSITIVE)
            val generalMatcher = generalUrlPattern.matcher(text)
            if (generalMatcher.find()) {
                return generalMatcher.group(1) ?: text.trim()
            }
            return text.trim()
        }
    }

    suspend fun extractVideoInfo(input: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        try {
            val rawInput = input.trim()
            val extractedUrl = extractUrlFromText(rawInput)

            // 1. AUTOMATIC BACKGROUND HTML CHECK
            if (isRawHtml(rawInput)) {
                val extractedInfo = parseFromHtmlSource(rawInput, "کد HTML واردشده")
                if (extractedInfo != null && extractedInfo.videoUrl.isNotBlank()) {
                    return@withContext Result.success(extractedInfo)
                }
            }

            // 2. DIRECT MP4 / VIDEO LINK
            val cleanUrl = cleanUrl(extractedUrl)
            if (cleanUrl.endsWith(".mp4", ignoreCase = true) || cleanUrl.contains(".mp4?", ignoreCase = true)) {
                return@withContext Result.success(
                    VideoInfo(
                        title = "ویدیو مستقیم (${getShortFileName(cleanUrl)})",
                        videoUrl = cleanUrl,
                        thumbnailUrl = "",
                        author = "@direct_video",
                        platform = "Direct MP4",
                        rawUrl = input,
                        qualities = emptyList()
                    )
                )
            }

            val author = extractAuthorFromUrl(extractedUrl)
            val shortcode = extractShortcode(extractedUrl)

            if (shortcode.isNullOrBlank()) {
                return@withContext Result.failure(
                    Exception("شناسه ویدیو (Shortcode) از لینک وارد شده قابل تشخیص نیست. لطفاً آدرس معتبر اینستاگرام را وارد کنید.")
                )
            }

            // 3. LAYER 1: COBALT OPEN API MIRRORS (Extremely reliable for Instagram Reels)
            val cobaltResult = tryCobaltApi(shortcode, author)
            if (cobaltResult != null) {
                return@withContext Result.success(cobaltResult)
            }

            // 4. LAYER 2: INSTAGRAM GRAPHQL QUERY API
            val graphQlResult = tryGraphQLApi(shortcode, author)
            if (graphQlResult != null) {
                return@withContext Result.success(graphQlResult)
            }

            // 5. LAYER 3: PUBLER API
            val publerResult = tryPublerApi(shortcode, author)
            if (publerResult != null) {
                return@withContext Result.success(publerResult)
            }

            // 6. LAYER 4: SAVEIG API
            val saveIgResult = trySaveIgApi(shortcode, author)
            if (saveIgResult != null) {
                return@withContext Result.success(saveIgResult)
            }

            // 7. LAYER 5: THIRD-PARTY HELPER APIS
            val thirdPartyResult = tryThirdPartyApis(shortcode, author)
            if (thirdPartyResult != null) {
                return@withContext Result.success(thirdPartyResult)
            }

            // 8. LAYER 6: PROXY MIRRORS & EMBED SCRAPING
            val proxyResult = tryProxyMirrorsAndEmbeds(shortcode, author, extractedUrl)
            if (proxyResult != null) {
                return@withContext Result.success(proxyResult)
            }

            // 9. LAYER 7: CDN DIRECT PATTERN SEARCH
            val directCdnResult = fetchDirectCdnUrl(shortcode, author)
            if (directCdnResult != null) {
                return@withContext Result.success(directCdnResult)
            }

            return@withContext Result.failure(
                Exception("امکان دریافت مستقیم این ویدیو به دلیل محدودیت‌های شبکه اینستاگرام وجود نداشت. لطفاً دوباره تلاش کنید یا سورس HTML صفحه را وارد نمایید.")
            )

        } catch (e: Exception) {
            Result.failure(
                Exception("خطا در پردازش لینک: ${e.localizedMessage}")
            )
        }
    }

    private fun tryCobaltApi(shortcode: String, author: String): VideoInfo? {
        val cobaltEndpoints = listOf(
            "https://api.cobalt.tools/api/json",
            "https://co.wuk.sh/api/json",
            "https://cobalt.qtf.rs/api/json",
            "https://cobalt-api.kwi.re/api/json",
            "https://api.cobalt.v07.me/api/json"
        )

        val jsonBody = "{\"url\":\"https://www.instagram.com/reel/$shortcode/\"}"
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

        for (endpoint in cobaltEndpoints) {
            try {
                val reqBody = jsonBody.toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(endpoint)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", userAgents[0])
                    .post(reqBody)
                    .build()

                val response = client.newCall(request).execute()
                val bodyText = response.body?.string() ?: ""
                response.close()

                if (bodyText.isNotBlank()) {
                    val extractedUrl = parseCobaltJson(bodyText)
                    if (!extractedUrl.isNullOrBlank() && (extractedUrl.startsWith("http://") || extractedUrl.startsWith("https://"))) {
                        return VideoInfo(
                            title = "ویدیو ریلز اینستاگرام (@$author)",
                            videoUrl = extractedUrl,
                            thumbnailUrl = "",
                            author = "@$author",
                            platform = "Instagram Reel",
                            rawUrl = "https://www.instagram.com/reel/$shortcode/",
                            qualities = emptyList()
                        )
                    }
                }
            } catch (e: Exception) {
                // Try next endpoint
            }
        }
        return null
    }

    private fun parseCobaltJson(jsonText: String): String? {
        return try {
            val urlPattern = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE)
            val matcher = urlPattern.matcher(jsonText)
            if (matcher.find()) {
                val rawUrl = matcher.group(1) ?: ""
                val cleanUrl = unescapeUrl(rawUrl)
                if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
                    return cleanUrl
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun tryGraphQLApi(shortcode: String, author: String): VideoInfo? {
        val docIds = listOf("8833621430043128", "10015901848480474", "7302213753133663")
        for (docId in docIds) {
            try {
                val url = "https://www.instagram.com/graphql/query/?doc_id=$docId&variables=%7B%22shortcode%22%3A%22$shortcode%22%7D"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgents[0])
                    .header("X-IG-App-ID", "936619743392459")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Accept", "*/*")
                    .build()

                val response = client.newCall(request).execute()
                val bodyText = response.body?.string() ?: ""
                response.close()

                if (bodyText.isNotBlank()) {
                    val parsed = parseFromHtmlSource(bodyText, author)
                    if (parsed != null && parsed.videoUrl.isNotBlank()) {
                        return parsed
                    }
                }
            } catch (e: Exception) {
                // Continue
            }
        }
        return null
    }

    private fun tryPublerApi(shortcode: String, author: String): VideoInfo? {
        val endpoints = listOf(
            "https://publer.io/api/v1/media/download",
            "https://api.publer.io/v1/media/download"
        )
        val jsonBody = "{\"url\":\"https://www.instagram.com/reel/$shortcode/\",\"is_job\":false}"
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

        for (endpoint in endpoints) {
            try {
                val reqBody = jsonBody.toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(endpoint)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", userAgents[0])
                    .post(reqBody)
                    .build()

                val response = client.newCall(request).execute()
                val bodyText = response.body?.string() ?: ""
                response.close()

                if (bodyText.isNotBlank()) {
                    val parsed = parseFromHtmlSource(bodyText, author)
                    if (parsed != null && parsed.videoUrl.isNotBlank()) {
                        return parsed
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        return null
    }

    private fun trySaveIgApi(shortcode: String, author: String): VideoInfo? {
        try {
            val formBody = FormBody.Builder()
                .add("q", "https://www.instagram.com/p/$shortcode/")
                .add("t", "media")
                .add("lang", "en")
                .build()

            val request = Request.Builder()
                .url("https://v3.saveig.app/api/ajaxSearch")
                .header("User-Agent", userAgents[0])
                .header("X-Requested-With", "XMLHttpRequest")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            val bodyText = response.body?.string() ?: ""
            response.close()

            if (bodyText.isNotBlank()) {
                val parsed = parseFromHtmlSource(bodyText, author)
                if (parsed != null && parsed.videoUrl.isNotBlank()) {
                    return parsed
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return null
    }

    private fun tryThirdPartyApis(shortcode: String, author: String): VideoInfo? {
        val apis = listOf(
            "https://api.vkrdown.com/api/item?url=https://www.instagram.com/p/$shortcode/"
        )
        for (apiUrl in apis) {
            try {
                val request = Request.Builder()
                    .url(apiUrl)
                    .header("User-Agent", userAgents[0])
                    .header("Accept", "application/json, text/plain, */*")
                    .build()

                val response = client.newCall(request).execute()
                val bodyText = response.body?.string() ?: ""
                response.close()

                if (bodyText.isNotBlank()) {
                    val parsed = parseFromHtmlSource(bodyText, author)
                    if (parsed != null && parsed.videoUrl.isNotBlank()) {
                        return parsed
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        return null
    }

    private fun tryProxyMirrorsAndEmbeds(shortcode: String, author: String, extractedUrl: String): VideoInfo? {
        val proxyUrls = mutableListOf<String>()

        proxyUrls.add("https://ddinstagram.com/reel/$shortcode")
        proxyUrls.add("https://ddinstagram.com/p/$shortcode")
        proxyUrls.add("https://vxinstagram.com/reel/$shortcode")
        proxyUrls.add("https://vxinstagram.com/p/$shortcode")
        proxyUrls.add("https://kkinstagram.com/reel/$shortcode")
        proxyUrls.add("https://kkinstagram.com/p/$shortcode")
        proxyUrls.add("https://fixinstagram.com/reel/$shortcode")

        if (author != "instagram_user" && author.isNotBlank()) {
            proxyUrls.add("https://www.instagram.com/$author/reel/$shortcode/embed/")
        }
        proxyUrls.add("https://www.instagram.com/p/$shortcode/embed/captioned/")
        proxyUrls.add("https://www.instagram.com/reel/$shortcode/embed/")
        proxyUrls.add("https://www.instagram.com/p/$shortcode/embed/")
        proxyUrls.add("https://www.instagram.com/p/$shortcode/?__a=1&__d=dis")

        for (proxyUrl in proxyUrls) {
            try {
                val reqBuilder = Request.Builder().url(proxyUrl)
                if (proxyUrl.contains("__a=1")) {
                    reqBuilder.header("User-Agent", userAgents[0])
                    reqBuilder.header("X-IG-App-ID", "936619743392459")
                    reqBuilder.header("X-Requested-With", "XMLHttpRequest")
                } else {
                    reqBuilder.header("User-Agent", userAgents[0])
                    reqBuilder.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                }

                val response = client.newCall(reqBuilder.build()).execute()
                val bodyText = response.body?.string() ?: ""
                response.close()

                if (bodyText.isNotBlank()) {
                    val parsedFromProxy = parseFromHtmlSource(bodyText, author)
                    if (parsedFromProxy != null && parsedFromProxy.videoUrl.isNotBlank()) {
                        return parsedFromProxy.copy(
                            title = "ویدیو ریلز اینستاگرام (@$author)",
                            author = "@$author",
                            platform = "Instagram Reel",
                            rawUrl = extractedUrl
                        )
                    }
                }
            } catch (e: Exception) {
                // Continue
            }
        }
        return null
    }

    private fun fetchDirectCdnUrl(shortcode: String, author: String): VideoInfo? {
        val testUrls = listOf(
            "https://ddinstagram.com/reel/$shortcode",
            "https://vxinstagram.com/reel/$shortcode",
            "https://kkinstagram.com/reel/$shortcode"
        )
        for (u in testUrls) {
            try {
                val req = Request.Builder().url(u).header("User-Agent", userAgents[1]).build()
                val res = client.newCall(req).execute()
                val html = res.body?.string() ?: ""
                res.close()

                val decodedHtml = decodeHtmlEntities(html)
                val cdnPattern = Pattern.compile("(https?://[\\w.-]*(?:cdninstagram\\.com|fbcdn\\.net)[^\"'\\s<>]+\\.(?:mp4|m4v)[^\"'\\s<>]*)", Pattern.CASE_INSENSITIVE)
                val matcher = cdnPattern.matcher(decodedHtml)
                if (matcher.find()) {
                    val rawMp4 = matcher.group(1) ?: ""
                    val cleanMp4 = unescapeUrl(rawMp4)
                    if (cleanMp4.isNotBlank()) {
                        return VideoInfo(
                            title = "ویدیو ریلز اینستاگرام (@$author)",
                            videoUrl = cleanMp4,
                            thumbnailUrl = "",
                            author = "@$author",
                            platform = "Instagram Reel",
                            rawUrl = "https://www.instagram.com/p/$shortcode/",
                            qualities = emptyList()
                        )
                    }
                }
            } catch (e: Exception) {
                // Continue
            }
        }
        return null
    }

    private fun isRawHtml(input: String): Boolean {
        return input.contains("<html", ignoreCase = true) ||
                input.contains("<meta", ignoreCase = true) ||
                input.contains("<video", ignoreCase = true) ||
                input.contains("video_url", ignoreCase = true) ||
                input.contains("og:video", ignoreCase = true) ||
                input.contains("contentUrl", ignoreCase = true) ||
                (input.contains("<") && input.contains(">") && input.length > 80)
    }

    private fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&quot;", "\"")
            .replace("&#34;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("\\\"", "\"")
            .replace("\\x22", "\"")
            .replace("\\/", "/")
            .replace("\\u0026", "&")
            .replace("\\u002F", "/")
    }

    private fun parseFromHtmlSource(html: String, authorOrSource: String): VideoInfo? {
        if (html.isBlank()) return null

        val decodedHtml = decodeHtmlEntities(html)

        val videoPatterns = listOf(
            "\"video_url\"\\s*:\\s*\"([^\"]+)\"",
            "\"video_hd\"\\s*:\\s*\"([^\"]+)\"",
            "\"video_sd\"\\s*:\\s*\"([^\"]+)\"",
            "\"videoUrl\"\\s*:\\s*\"([^\"]+)\"",
            "\"download_url\"\\s*:\\s*\"([^\"]+)\"",
            "\"downloadUrl\"\\s*:\\s*\"([^\"]+)\"",
            "\"path\"\\s*:\\s*\"(https?://[^\"]+)\"",
            "\"url\"\\s*:\\s*\"(https?://[^\"]+\\.mp4[^\"]*)\"",
            "<meta\\s+property=\"og:video\"\\s+content=\"([^\"]+)\"",
            "<meta\\s+property=\"og:video:secure_url\"\\s+content=\"([^\"]+)\"",
            "<meta\\s+name=\"twitter:player:stream\"\\s+content=\"([^\"]+)\"",
            "<meta\\s+property=\"twitter:player:stream\"\\s+content=\"([^\"]+)\"",
            "\"contentUrl\"\\s*:\\s*\"([^\"]+)\"",
            "\"browser_native_hd_url\"\\s*:\\s*\"([^\"]+)\"",
            "\"browser_native_sd_url\"\\s*:\\s*\"([^\"]+)\"",
            "<video[^>]+src=\"([^\"]+)\"",
            "<source[^>]+src=\"([^\"]+)\"",
            "\"video_versions\"\\s*:\\s*\\[\\s*\\{\\s*\"height\"\\s*:\\s*\\d+\\s*,\\s*\"url\"\\s*:\\s*\"([^\"]+)\"",
            "(https?://[\\w.-]*(?:cdninstagram\\.com|fbcdn\\.net)[^\"'\\s<>]+\\.(?:mp4|m4v)[^\"'\\s<>]*)",
            "(https?://[\\w.-]*(?:cdninstagram\\.com|fbcdn\\.net)[^\"'\\s<>]+)"
        )

        val thumbnailPatterns = listOf(
            "\"display_url\"\\s*:\\s*\"([^\"]+)\"",
            "<meta\\s+property=\"og:image\"\\s+content=\"([^\"]+)\"",
            "\"thumbnailUrl\"\\s*:\\s*\"([^\"]+)\"",
            "\"poster\"\\s*:\\s*\"([^\"]+)\"",
            "\"thumbnail_src\"\\s*:\\s*\"([^\"]+)\"",
            "\"thumb\"\\s*:\\s*\"([^\"]+)\""
        )

        val titlePatterns = listOf(
            "<meta\\s+property=\"og:title\"\\s+content=\"([^\"]+)\"",
            "<title>([^<]+)</title>",
            "\"description\"\\s*:\\s*\"([^\"]+)\"",
            "\"caption\"\\s*:\\s*\"([^\"]+)\""
        )

        val rawVideoUrl = findMatch(decodedHtml, videoPatterns) ?: return null
        val unescapedVideoUrl = unescapeUrl(rawVideoUrl)

        if (!unescapedVideoUrl.startsWith("http://") && !unescapedVideoUrl.startsWith("https://")) {
            return null
        }

        val rawThumbUrl = findMatch(decodedHtml, thumbnailPatterns) ?: ""
        val unescapedThumbUrl = if (rawThumbUrl.isNotBlank()) unescapeUrl(rawThumbUrl) else ""
        val rawTitle = findMatch(decodedHtml, titlePatterns)

        val cleanedTitle = cleanTitle(rawTitle, authorOrSource)
        val cleanAuthor = if (authorOrSource.startsWith("@")) authorOrSource.substring(1) else authorOrSource

        return VideoInfo(
            title = cleanedTitle.ifBlank { "ویدیو ریلز اینستاگرام (@$cleanAuthor)" },
            videoUrl = unescapedVideoUrl,
            thumbnailUrl = unescapedThumbUrl,
            author = "@$cleanAuthor",
            platform = "Instagram Reel",
            rawUrl = "",
            qualities = emptyList()
        )
    }

    private fun extractShortcode(inputUrl: String): String? {
        val clean = inputUrl.split("?")[0].split("#")[0].trim()
        val patterns = listOf(
            "(?:reel|reels|p|tv|share/reel|share/p)/([A-Za-z0-9_\\-]+)",
            "/([A-Za-z0-9_\\-]{5,35})(?:/|\\?|#|\$)"
        )
        for (pattern in patterns) {
            val matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(clean)
            if (matcher.find()) {
                val code = matcher.group(1)
                if (code != null && code.lowercase() !in listOf("reel", "reels", "p", "tv", "share", "stories", "instagram", "explore", "m", "www")) {
                    return code
                }
            }
        }
        return null
    }

    private fun cleanUrl(url: String): String {
        var u = url.trim()
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "https://$u"
        }
        return u.split("?")[0].split("#")[0]
    }

    private fun extractAuthorFromUrl(url: String): String {
        return try {
            val clean = url.split("?")[0].split("#")[0].replace("https://", "").replace("http://", "").trim('/')
            val parts = clean.split("/")
            if (parts.size >= 2 && (parts[0].contains("instagram") || parts[0].contains("instagr.am"))) {
                val candidate = parts[1]
                if (candidate.lowercase() !in listOf("reel", "reels", "p", "tv", "share", "stories", "explore", "m", "www")) {
                    candidate
                } else {
                    "instagram_user"
                }
            } else {
                "instagram_user"
            }
        } catch (e: Exception) {
            "instagram_user"
        }
    }

    private fun cleanTitle(rawTitle: String?, author: String): String {
        if (rawTitle.isNullOrEmpty()) return "ویدیو ریلز اینستاگرام (@$author)"
        var t = rawTitle
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&amp;", "&")
            .replace("Instagram: ", "")
            .replace("Instagram", "")
            .trim()

        if (t.startsWith(" “") || t.startsWith("“")) {
            t = t.trim(' ', '“', '”', '"')
        }
        return t.take(120)
    }

    private fun unescapeUrl(url: String): String {
        var u = url
            .replace("\\u0026", "&")
            .replace("\\u002F", "/")
            .replace("\\/", "/")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("\\", "")
            .trim()
        if (u.startsWith("//")) {
            u = "https:$u"
        }
        return u
    }

    private fun findMatch(text: String, patterns: List<String>): String? {
        for (p in patterns) {
            val matcher = Pattern.compile(p, Pattern.CASE_INSENSITIVE).matcher(text)
            if (matcher.find()) {
                val match = matcher.group(1)
                if (!match.isNullOrEmpty()) {
                    return match
                }
            }
        }
        return null
    }

    private fun getShortFileName(url: String): String {
        val lastPart = url.substringAfterLast('/').substringBefore('?')
        return if (lastPart.length > 20) lastPart.take(15) + ".mp4" else lastPart
    }
}
