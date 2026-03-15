package com.lockin.app.data.remote.wger

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service for the wger Workout Manager REST API.
 *
 * Docs: https://wger.de/en/software/api
 * Base URL: https://wger.de/api/v2/
 *
 * Uses the /exerciseinfo/ endpoint which returns enriched data
 * (images, muscles, equipment in a single call).
 */
interface WgerApi {

    /**
     * Get exercises with full info (images, muscles, equipment).
     * @param format Always "json".
     * @param language 2 = English.
     * @param limit Page size.
     * @param offset Pagination offset.
     */
    @GET("exerciseinfo/?format=json")
    suspend fun getExercises(
        @Query("language") language: Int = 2,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): WgerResponse

    /**
     * Get exercises filtered by category (muscle group).
     * wger category IDs: 8=Arms, 9=Legs, 10=Abs, 11=Chest, 12=Back, 13=Shoulders, 14=Calves
     */
    @GET("exerciseinfo/?format=json")
    suspend fun getByCategory(
        @Query("language") language: Int = 2,
        @Query("category") category: Int,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): WgerResponse
}

