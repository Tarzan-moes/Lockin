package com.lockin.app.data.external

/**
 * External Data Source Interfaces
 *
 * These define contracts for platform-specific health and sensor APIs.
 * The data layer depends on these interfaces; concrete implementations
 * will use Health Connect SDK, Samsung Health SDK, or Android sensor APIs.
 *
 * Keeping them as interfaces means we can swap real implementations
 * for fakes in tests or on devices that lack certain hardware.
 */

/** Adapter for Google Health Connect – reads sleep, heart rate, steps, etc. */
interface HealthConnectDataSource {
    suspend fun isAvailable(): Boolean
    suspend fun hasPermissions(): Boolean
    suspend fun requestPermissions(): Boolean
    suspend fun readSleepSessions(fromEpochMillis: Long, toEpochMillis: Long): Any? // TODO: typed model
    suspend fun readHeartRate(fromEpochMillis: Long, toEpochMillis: Long): Any?     // TODO: typed model
    suspend fun readSteps(fromEpochMillis: Long, toEpochMillis: Long): Long
}

/** Adapter for Samsung Health SDK – specific Samsung device integrations. */
interface SamsungHealthDataSource {
    suspend fun isAvailable(): Boolean
    suspend fun connect(): Boolean
    suspend fun readBodyComposition(): Any?  // TODO: typed model
    suspend fun readStressLevel(): Int?
}

/** Adapter for on-device sensors (accelerometer, gyroscope, etc.). */
interface SensorDataSource {
    fun startListening()
    fun stopListening()
    suspend fun getLatestAccelerometer(): Triple<Float, Float, Float>?
    suspend fun getLatestGyroscope(): Triple<Float, Float, Float>?
}

