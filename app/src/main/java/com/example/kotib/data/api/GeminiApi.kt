package com.example.kotib.data.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GeminiApi {

    /**
     * Gemini REST API orqali kontent va vosita chaqiruvlarini generatsiya qilish
     */
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body requestBody: RequestBody
    ): Response<ResponseBody>
}
