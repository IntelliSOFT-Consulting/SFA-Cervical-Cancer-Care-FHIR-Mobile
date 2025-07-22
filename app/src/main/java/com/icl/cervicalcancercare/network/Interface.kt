package com.icl.cervicalcancercare.network

import com.icl.cervicalcancercare.models.DataProcessingResponse
import com.icl.cervicalcancercare.models.ExtractedData
import com.icl.cervicalcancercare.models.Login
import com.icl.cervicalcancercare.models.LoginResponse
import retrofit2.Response

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST


interface Interface {

    @POST("login")
    suspend fun signInUser(@Body login: Login): Response<LoginResponse>

    @POST("query_clinical_decision_making")
    suspend fun processData(
        @Body data: ExtractedData,
        @Header("Authorization") token: String?
    ): Response<DataProcessingResponse>

}