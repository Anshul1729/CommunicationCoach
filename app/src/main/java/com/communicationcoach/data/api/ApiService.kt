package com.communicationcoach.data.api

import com.communicationcoach.data.model.GeminiResponse
import com.communicationcoach.data.model.SpeechResponse
import com.communicationcoach.data.model.WorkerGeminiRequest
import com.communicationcoach.data.model.WorkerTranscribeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface WorkerApiService {

    @POST("transcribe")
    suspend fun transcribe(
        @Header("Authorization") auth: String,
        @Body request: WorkerTranscribeRequest
    ): Response<SpeechResponse>

    @POST("gemini")
    suspend fun gemini(
        @Header("Authorization") auth: String,
        @Body request: WorkerGeminiRequest
    ): Response<GeminiResponse>
}
