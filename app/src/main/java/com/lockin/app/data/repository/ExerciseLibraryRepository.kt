package com.lockin.app.data.repository

import android.util.Log
import com.lockin.app.data.local.DefaultExercises
import com.lockin.app.data.local.database.dao.ExerciseDetailDao
import com.lockin.app.data.local.models.ExerciseDetailEntity
import com.lockin.app.data.remote.exercisedb.ExerciseDbApi
import com.lockin.app.data.remote.exercisedb.toEntity
import com.lockin.app.data.remote.wger.WgerApi
import com.lockin.app.data.remote.wger.toEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ExerciseLibraryRepository – merges ExerciseDB + wger into a single
 * de-duplicated local Room cache.
 *
 * Strategy:
 * 1. Always observe from Room (offline-first).
 * 2. On first load, sync from BOTH APIs in parallel.
 * 3. De-duplicate by normalized (name, primaryMuscle) key, preferring
 *    entries that have a GIF/image URL.
 * 4. If all APIs fail and Room is empty, seed with built-in defaults.
 */
@Singleton
class ExerciseLibraryRepository @Inject constructor(
    private val dao: ExerciseDetailDao,
    private val exerciseDbApi: ExerciseDbApi,
    private val wgerApi: WgerApi
) {
    companion object {
        private const val TAG = "ExerciseLibRepo"
        private const val PAGE_SIZE = 50
        private const val EXERCISEDB_PAGES = 6   // 50 × 6 = 300 from ExerciseDB
        private const val WGER_PAGES = 4          // 50 × 4 = 200 from wger
        private const val MIN_CACHE_FOR_SKIP = 200
    }

    var lastSyncMessage: String = ""
        private set

    fun observeAll(): Flow<List<ExerciseDetailEntity>> = dao.observeAll()

    suspend fun getById(id: String): ExerciseDetailEntity? = dao.getById(id)

    /**
     * Sync from both ExerciseDB and wger in parallel, merge, dedupe, and cache.
     */
    suspend fun syncFromApi(): Boolean {
        val cachedCount = dao.count()
        if (cachedCount >= MIN_CACHE_FOR_SKIP) {
            lastSyncMessage = "Cache OK ($cachedCount exercises)"
            Log.d(TAG, lastSyncMessage)
            return true
        }

        Log.d(TAG, "Starting multi-source sync (cache=$cachedCount)…")
        lastSyncMessage = "Syncing from 2 sources…"

        return try {
            val (exerciseDbEntities, wgerEntities) = coroutineScope {
                val edbDeferred = async { fetchExerciseDb() }
                val wgerDeferred = async { fetchWger() }
                Pair(edbDeferred.await(), wgerDeferred.await())
            }

            Log.d(TAG, "ExerciseDB: ${exerciseDbEntities.size}, wger: ${wgerEntities.size}")

            // Merge and deduplicate
            val merged = deduplicateExercises(exerciseDbEntities + wgerEntities)
            Log.d(TAG, "After dedupe: ${merged.size} exercises")

            if (merged.isNotEmpty()) {
                dao.upsertAll(merged)
                lastSyncMessage = "Synced ${merged.size} exercises ✓ (EDB:${exerciseDbEntities.size} + wger:${wgerEntities.size})"
                Log.d(TAG, lastSyncMessage)
                true
            } else {
                lastSyncMessage = "Both APIs returned empty"
                Log.w(TAG, lastSyncMessage)
                seedIfEmpty()
                false
            }
        } catch (e: Exception) {
            lastSyncMessage = "Sync error: ${e.javaClass.simpleName}: ${e.message}"
            Log.e(TAG, lastSyncMessage, e)
            seedIfEmpty()
            false
        }
    }

    // ── ExerciseDB fetcher ───────────────────────────────────────────

    private suspend fun fetchExerciseDb(): List<ExerciseDetailEntity> {
        val all = mutableListOf<ExerciseDetailEntity>()
        try {
            for (page in 0 until EXERCISEDB_PAGES) {
                val offset = page * PAGE_SIZE
                val response = exerciseDbApi.getExercises(limit = PAGE_SIZE, offset = offset)
                if (response.data.isEmpty()) break
                all.addAll(response.data.map { it.toEntity() })
            }
            Log.d(TAG, "ExerciseDB fetched ${all.size} exercises")
        } catch (e: Exception) {
            Log.w(TAG, "ExerciseDB fetch failed: ${e.message}")
        }
        return all
    }

    // ── wger fetcher ─────────────────────────────────────────────────

    private suspend fun fetchWger(): List<ExerciseDetailEntity> {
        val all = mutableListOf<ExerciseDetailEntity>()
        try {
            for (page in 0 until WGER_PAGES) {
                val offset = page * PAGE_SIZE
                val response = wgerApi.getExercises(language = 2, limit = PAGE_SIZE, offset = offset)
                if (response.results.isEmpty()) break

                // Filter out exercises with blank names
                val entities = response.results
                    .filter { it.name.isNotBlank() }
                    .map { it.toEntity() }
                all.addAll(entities)
            }
            Log.d(TAG, "wger fetched ${all.size} exercises")
        } catch (e: Exception) {
            Log.w(TAG, "wger fetch failed: ${e.message}")
        }
        return all
    }

    // ── Deduplication ────────────────────────────────────────────────

    /**
     * Deduplicate by normalized (name, primaryMuscle).
     * Within each group, prefer entries that have a GIF/image URL and instructions.
     */
    private fun deduplicateExercises(
        exercises: List<ExerciseDetailEntity>
    ): List<ExerciseDetailEntity> {
        return exercises
            .groupBy { dedupeKey(it) }
            .map { (_, group) ->
                group.sortedWith(
                    compareByDescending<ExerciseDetailEntity> { !it.gifUrl.isNullOrBlank() }
                        .thenByDescending { it.instructions.length }
                        .thenByDescending { it.secondaryMuscles.size }
                ).first()
            }
    }

    private fun dedupeKey(e: ExerciseDetailEntity): String {
        val normalizedName = e.name.trim().lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
        return "$normalizedName|${e.primaryMuscleGroup.lowercase()}"
    }

    // ── Seed fallback ────────────────────────────────────────────────

    suspend fun seedIfEmpty() {
        if (dao.count() == 0) {
            Log.d(TAG, "Cache empty – seeding with built-in defaults")
            dao.upsertAll(DefaultExercises.all())
        }
    }
}
