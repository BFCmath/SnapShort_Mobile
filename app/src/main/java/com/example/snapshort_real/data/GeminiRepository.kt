package com.example.snapshort_real.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class TaskSuggestion(
    val title: String,
    val description: String,
    val dueDate: Long?
)

class GeminiRepository @Inject constructor() {

    private val apiKey = "[ENCRYPTION_KEY]"
    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey
    )

    suspend fun generateTaskInfo(imagePath: String): TaskSuggestion? {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("GeminiRepository", "Starting task info generation for image: $imagePath")
                val bitmap = BitmapFactory.decodeFile(imagePath)
                if (bitmap == null) {
                    android.util.Log.e("GeminiRepository", "Failed to decode bitmap from path: $imagePath")
                    return@withContext null
                }
                
                val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())

                val prompt = """
                    You are a task extraction assistant. Analyze the provided image and extract task information.
                    
                    **Current Context:**
                    - Date: $currentDate ($dayOfWeek)
                    
                    **Goal:**
                    Extract:
                    1. **Task Name**: Short title (max 50 chars).
                    2. **Description**: Brief description.
                    3. **Due Date**: specific date mentioned or implied (e.g., "tomorrow").
                    
                    **Rules:**
                    - Only extract CLEAR info.
                    - Return JSON structure:
                    {
                        "task_name": "...", 
                        "description": "...", 
                        "due_date": "YYYY-MM-DDTHH:MM:SS" (ISO 8601) or null
                    }
                    - If vague/no task info, return null values.
                """.trimIndent()

                android.util.Log.d("GeminiRepository", "Sending request to Gemini...")
                val response = model.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )

                val responseText = response.text
                android.util.Log.d("GeminiRepository", "Raw Gemini Response: $responseText")

                if (responseText == null) {
                    android.util.Log.e("GeminiRepository", "Gemini response text is null")
                    return@withContext null
                }
                
                parseResponse(responseText)
            } catch (e: Exception) {
                android.util.Log.e("GeminiRepository", "Error generating task info", e)
                e.printStackTrace()
                null
            }
        }
    }

    private fun parseResponse(text: String): TaskSuggestion? {
        try {
            // Clean markdown code blocks if present
            val cleanText = text.replace("```json", "").replace("```", "").trim()
            val json = JSONObject(cleanText)

            val title = json.optString("task_name").takeIf { it != "null" && it.isNotEmpty() }
            val description = json.optString("description").takeIf { it != "null" && it.isNotEmpty() }
            val dueDateStr = json.optString("due_date").takeIf { it != "null" && it.isNotEmpty() }

            if (title == null && description == null && dueDateStr == null) {
                return TaskSuggestion(title = "", description = "", dueDate = null)
            }

            var dueDate: Long? = null
            if (dueDateStr != null) {
                try {
                    // Try parsing ISO 8601
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    dueDate = sdf.parse(dueDateStr)?.time
                } catch (e: Exception) {
                    // Fallback or ignore
                }
            }

            return TaskSuggestion(
                title = title ?: "",
                description = description ?: "",
                dueDate = dueDate
            )
        } catch (e: Exception) {
            android.util.Log.e("GeminiRepository", "Error parsing response: $text", e)
            e.printStackTrace()
            return null
        }
    }
}
