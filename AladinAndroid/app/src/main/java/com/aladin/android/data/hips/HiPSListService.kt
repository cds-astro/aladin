package com.aladin.android.data.hips

import com.aladin.android.data.net.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray

object HiPSListService {
    suspend fun fetchAvailableSurveys(): List<HiPSSurvey> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://alasky.cds.unistra.fr/MocServer/query?dataproduct_type=image&get=record&fmt=json")
            .build()

        try {
            val response = HttpClient.client.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) return@withContext SurveyRegistry.builtIn
                val jsonText = resp.body?.string() ?: return@withContext SurveyRegistry.builtIn
                val array = JSONArray(jsonText)
                val list = mutableListOf<HiPSSurvey>()
                
                // Add built-ins first to keep them at the top
                val builtInIds = SurveyRegistry.builtIn.map { it.id }.toSet()
                list.addAll(SurveyRegistry.builtIn)
                
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("ID", "")
                    if (id.isBlank() || builtInIds.contains(id)) continue
                    
                    val title = obj.optString("obs_title", id)
                    val url = obj.optString("hips_service_url", "")
                    val desc = obj.optString("obs_description", "")
                    val isColor = title.contains("color", ignoreCase = true)
                    
                    if (url.isNotBlank()) {
                        list.add(HiPSSurvey(id, title, url, isColor, desc))
                    }
                }
                list
            }
        } catch (e: Exception) {
            SurveyRegistry.builtIn
        }
    }
}
