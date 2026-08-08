package com.example.downloader

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.data.AdItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AdManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences("ad_manager_cache_prefs", Context.MODE_PRIVATE)
    private val keySavedAdsJson = "saved_ads_json"

    private val adsUrl = "https://raw.githubusercontent.com/cafenetnetadib24-design/ads/refs/heads/main/ads.html"
    private var cachedAdsList: List<AdItem> = emptyList()

    init {
        // Load offline cached ads on initialization
        cachedAdsList = loadAdsFromPrefs()
    }

    suspend fun fetchAndCacheAds(): List<AdItem> = withContext(Dispatchers.IO) {
        if (isNetworkAvailable()) {
            try {
                val request = Request.Builder()
                    .url(adsUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                    .build()

                val response = client.newCall(request).execute()
                val htmlContent = response.body?.string() ?: ""

                val parsedAds = parseAdsFromHtml(htmlContent)
                if (parsedAds.isNotEmpty()) {
                    cachedAdsList = parsedAds
                    saveAdsToPrefs(parsedAds)
                    preCacheImages(parsedAds)
                    Log.d("AdManager", "Successfully updated & cached online ${parsedAds.size} ads")
                    return@withContext cachedAdsList
                }
            } catch (e: Exception) {
                Log.e("AdManager", "Error fetching online ads, falling back to cached ads: ${e.message}")
            }
        } else {
            Log.d("AdManager", "No active internet connection. Utilizing cached ad content.")
        }

        // Fallback to local disk cache if network request failed or offline
        if (cachedAdsList.isEmpty()) {
            cachedAdsList = loadAdsFromPrefs()
        }
        
        // Also ensure cached images are requested with disk cache enabled
        if (cachedAdsList.isNotEmpty()) {
            preCacheImages(cachedAdsList)
        }
        
        return@withContext cachedAdsList
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
                capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
            } else false
        } catch (e: Exception) {
            true // Attempt network fetch if check fails
        }
    }

    private fun saveAdsToPrefs(ads: List<AdItem>) {
        try {
            val array = JSONArray()
            for (ad in ads) {
                val obj = JSONObject().apply {
                    put("id", ad.id)
                    put("imageUrl", ad.imageUrl)
                    put("targetUrl", ad.targetUrl)
                    put("title", ad.title)
                }
                array.put(obj)
            }
            prefs.edit().putString(keySavedAdsJson, array.toString()).apply()
        } catch (e: Exception) {
            Log.e("AdManager", "Error saving ads to prefs: ${e.message}")
        }
    }

    private fun loadAdsFromPrefs(): List<AdItem> {
        val list = mutableListOf<AdItem>()
        val savedJson = prefs.getString(keySavedAdsJson, null) ?: return emptyList()
        try {
            val array = JSONArray(savedJson)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString("id", "cached_$i")
                val img = obj.optString("imageUrl", "")
                val target = obj.optString("targetUrl", "")
                val title = obj.optString("title", "پیشنهاد ویژه")
                if (img.isNotBlank()) {
                    list.add(AdItem(id = id, imageUrl = img, targetUrl = target, title = title))
                }
            }
        } catch (e: Exception) {
            Log.e("AdManager", "Error reading ads from prefs: ${e.message}")
        }
        return list
    }

    private fun parseAdsFromHtml(html: String): List<AdItem> {
        val list = mutableListOf<AdItem>()
        val trimmed = html.trim()

        // 1. Check if JSON Object with "ads" or "data" or "items" array
        try {
            val objStart = trimmed.indexOf("{")
            val objEnd = trimmed.lastIndexOf("}")
            if (objStart != -1 && objEnd > objStart) {
                val jsonStr = trimmed.substring(objStart, objEnd + 1)
                val jsonObject = JSONObject(jsonStr)
                val adsArray = jsonObject.optJSONArray("ads")
                    ?: jsonObject.optJSONArray("data")
                    ?: jsonObject.optJSONArray("items")

                if (adsArray != null) {
                    for (i in 0 until adsArray.length()) {
                        val obj = adsArray.optJSONObject(i) ?: continue
                        val img = extractImageUrl(obj)
                        val link = extractTargetUrl(obj)
                        val title = extractTitle(obj, i)
                        if (img.isNotBlank()) {
                            list.add(AdItem(id = "json_obj_$i", imageUrl = img, targetUrl = link, title = title))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AdManager", "JSONObject parse error: ${e.message}")
        }

        if (list.isNotEmpty()) return list

        // 2. Check if JSON Array
        try {
            val jsonStart = trimmed.indexOf("[")
            val jsonEnd = trimmed.lastIndexOf("]")
            if (jsonStart != -1 && jsonEnd > jsonStart) {
                val jsonStr = trimmed.substring(jsonStart, jsonEnd + 1)
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    val img = extractImageUrl(obj)
                    val link = extractTargetUrl(obj)
                    val title = extractTitle(obj, i)
                    if (img.isNotBlank()) {
                        list.add(AdItem(id = "json_arr_$i", imageUrl = img, targetUrl = link, title = title))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AdManager", "JSONArray parse error: ${e.message}")
        }

        if (list.isNotEmpty()) return list

        // 3. HTML anchor + img pattern regex
        val anchorImgRegex = Regex(
            """<a[^>]+href=["']([^"']+)["'][^>]*>[\s\S]*?<img[^>]+src=["']([^"']+)["'][^>]*(?:alt=["']([^"']*)["'])?[^>]*>""",
            RegexOption.IGNORE_CASE
        )
        val matches = anchorImgRegex.findAll(html)
        var count = 0
        for (match in matches) {
            val link = match.groupValues[1]
            val img = match.groupValues[2]
            val alt = match.groupValues.getOrNull(3)?.ifBlank { null } ?: "تبلیغات ویژه"
            if (img.isNotBlank()) {
                list.add(AdItem(id = "html_a_img_$count", imageUrl = img, targetUrl = link, title = alt))
                count++
            }
        }

        if (list.isNotEmpty()) return list

        // 4. Independent img tags and hrefs fallback
        val imgRegex = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val linkRegex = Regex("""<a[^>]+href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

        val imgMatches = imgRegex.findAll(html).map { it.groupValues[1] }.toList()
        val linkMatches = linkRegex.findAll(html).map { it.groupValues[1] }.toList()

        for (i in imgMatches.indices) {
            val img = imgMatches[i]
            val link = linkMatches.getOrNull(i) ?: ""
            list.add(AdItem(id = "html_img_$i", imageUrl = img, targetUrl = link, title = "تبلیغات ویژه"))
        }

        return list
    }

    private fun extractImageUrl(obj: JSONObject): String {
        return obj.optString("imageUrl",
            obj.optString("image",
                obj.optString("img",
                    obj.optString("src",
                        obj.optString("banner",
                            obj.optString("pic", "")
                        )
                    )
                )
            )
        )
    }

    private fun extractTargetUrl(obj: JSONObject): String {
        return obj.optString("clickUrl",
            obj.optString("targetUrl",
                obj.optString("link",
                    obj.optString("url",
                        obj.optString("href", "")
                    )
                )
            )
        )
    }

    private fun extractTitle(obj: JSONObject, index: Int): String {
        val rawTitle = obj.optString("title",
            obj.optString("caption",
                obj.optString("name", "")
            )
        ).trim()

        if (rawTitle.isNotBlank() && !rawTitle.contains(Regex("""(?i)\bad\d*\b"""))) {
            return rawTitle
        }
        return "پیشنهاد ویژه"
    }

    private fun preCacheImages(ads: List<AdItem>) {
        val imageLoader = ImageLoader(context)
        for (ad in ads) {
            if (ad.imageUrl.isNotBlank()) {
                val request = ImageRequest.Builder(context)
                    .data(ad.imageUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }

    fun getRandomAd(): AdItem? {
        if (cachedAdsList.isEmpty()) return null
        return cachedAdsList.random()
    }

    fun getAllAds(): List<AdItem> = cachedAdsList

    fun hasAds(): Boolean = cachedAdsList.isNotEmpty()
}
