package com.lockin.app.data.local.datastore

/**
 * LockinPreferences – Abstraction over DataStore<Preferences>.
 *
 * Provides typed read/write access to app-wide preferences like
 * theme mode, measurement units, and first-run flags.
 *
 * The concrete implementation will use Jetpack DataStore under the hood.
 * For Part 1 this is just the interface contract.
 */
interface LockinPreferences {

    /** Has the user completed the onboarding flow? */
    suspend fun isFirstRun(): Boolean
    suspend fun setFirstRunCompleted()

    /** Measurement unit preference (metric / imperial). */
    suspend fun getUseMetric(): Boolean
    suspend fun setUseMetric(metric: Boolean)

    /** User's preferred theme (dark is the only option for now). */
    suspend fun getThemeMode(): String   // "dark" | "system" | "light"
    suspend fun setThemeMode(mode: String)
}

