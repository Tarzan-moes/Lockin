package com.lockin.app.ai.inference

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lockin.app.BuildConfig
import com.lockin.app.ai.models.AiModelPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

private const val TAG = "ModelDownloadWorker"

/**
 * Downloads the Gemma TFLite model once after install (first app launch).
 * Safe behavior:
 * - Skips download when model already exists.
 * - Skips when no remote URL is configured.
 * - Retries on transient network/server failures.
 */
class ModelDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val targetFile = File(
            File(applicationContext.filesDir, AiModelPaths.MODELS_DIR),
            AiModelPaths.GEMMA_TFLITE_FILENAME
        )

        if (targetFile.exists() && targetFile.length() > 0L) {
            Log.i(TAG, "Model already present, skipping download: ${targetFile.absolutePath}")
            return@withContext Result.success()
        }

        val downloadUrl = BuildConfig.GEMMA_MODEL_DOWNLOAD_URL
            .ifBlank { AiModelPaths.DEFAULT_GEMMA_DOWNLOAD_URL }

        if (downloadUrl.isBlank()) {
            Log.w(TAG, "No GEMMA_MODEL_DOWNLOAD_URL configured; skipping auto-download")
            return@withContext Result.success()
        }

        targetFile.parentFile?.mkdirs()
        val tempFile = File(targetFile.absolutePath + ".part")

        return@withContext try {
            Log.i(TAG, "Downloading Gemma model from: $downloadUrl")
            downloadToFile(downloadUrl, tempFile)

            if (tempFile.length() <= 0L) {
                tempFile.delete()
                Log.e(TAG, "Downloaded file is empty")
                Result.retry()
            } else {
                if (targetFile.exists()) targetFile.delete()
                val renamed = tempFile.renameTo(targetFile)
                if (!renamed) {
                    tempFile.delete()
                    Log.e(TAG, "Failed to finalize downloaded model file")
                    Result.retry()
                } else {
                    Log.i(TAG, "Model download complete (${targetFile.length() / 1_048_576} MB)")
                    Result.success()
                }
            }
        } catch (e: Exception) {
            tempFile.delete()
            Log.e(TAG, "Model download failed: ${e.message}", e)
            Result.retry()
        }
    }

    private fun downloadToFile(downloadUrl: String, destination: File) {
        val connection = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            requestMethod = "GET"
            doInput = true
        }

        connection.connect()
        val code = connection.responseCode
        if (code !in 200..299) {
            connection.disconnect()
            throw IllegalStateException("HTTP $code from model URL")
        }

        connection.inputStream.use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(1024 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
        }

        connection.disconnect()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "download_gemma_model_once"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(UNIQUE_WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}

