package com.lockin.app.data.remote.exercisedb

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service for the ExerciseDB API.
 *
 * Default base URL targets the free public instance.
 * Override via Hilt / NetworkModule if self-hosting.
 *
 * All list endpoints return { success, metadata, data: [...] }
 */
interface ExerciseDbApi {

    @GET("exercises")
    suspend fun getExercises(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): ExerciseDbResponse

    @GET("exercises/bodyPart/{bodyPart}")
    suspend fun getByBodyPart(
        @Path("bodyPart") bodyPart: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): ExerciseDbResponse

    @GET("exercises/target/{target}")
    suspend fun getByTarget(
        @Path("target") target: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): ExerciseDbResponse

    @GET("exercises/equipment/{equipment}")
    suspend fun getByEquipment(
        @Path("equipment") equipment: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): ExerciseDbResponse

    @GET("exercises/name/{name}")
    suspend fun searchByName(
        @Path("name") name: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): ExerciseDbResponse

    /**
     * Get a single exercise by its ID.
     */
    @GET("exercises/exercise/{id}")
    suspend fun getExerciseById(
        @Path("id") id: String
    ): ExerciseDbDto
}
