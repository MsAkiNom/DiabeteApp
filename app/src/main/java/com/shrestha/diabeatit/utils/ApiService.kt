package com.shrestha.diabeatit

import com.shrestha.diabeatit.models.PlacesApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {

    @GET("json")
    fun sendReq(
        @Query("location") location: String,
        @Query("radius") radius: String,
        @Query("key") key: String
    ): Call<PlacesApiResponse>
}