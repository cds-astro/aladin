package com.aladin.android.data.hips

import com.aladin.android.data.net.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import java.io.File

object HiPSListService {
    suspend fun fetchAvailableSurveys(localDir: File): List<HiPSSurvey> = withContext(Dispatchers.IO) {
        val list = mutableListOf<HiPSSurvey>()
        val builtInIds = SurveyRegistry.builtIn.map { it.id }.toSet()
        list.addAll(SurveyRegistry.builtIn)
        
        // Find local datasets
        if (localDir.exists() && localDir.isDirectory) {
            localDir.listFiles()?.forEach { dir ->
                if (dir.isDirectory) {
                    val propsFile = File(dir, "properties")
                    if (propsFile.exists() && propsFile.length() > 0) {
                        val id = dir.name
                        val title = id.replace("_", " ") + " (local)"
                        val baseUrl = "file://" + dir.absolutePath
                        list.add(HiPSSurvey(id + "_local", title, baseUrl, true, "Local dataset"))
                    }
                }
            }
        }
        
        val request = Request.Builder()
            .url("https://alasky.cds.unistra.fr/MocServer/query?dataproduct_type=image&get=record&fmt=json")
            .build()

        try {
            val response = HttpClient.client.newCall(request).execute()
            response.use { resp ->
                if (resp.isSuccessful) {
                    val jsonText = resp.body?.string()
                    if (jsonText != null) {
                        val array = JSONArray(jsonText)
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
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore offline network error
        }
        
        list.sortBy { it.title }
        list
    }
}
