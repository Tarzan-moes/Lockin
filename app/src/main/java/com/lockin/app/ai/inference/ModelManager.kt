package com.lockin.app.ai.inference

import android.content.Context
import android.util.Log
import com.lockin.app.ai.models.AiModelPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * ModelManager – Handles copying and managing AI model files.
 *
 * Large model files (like Gemma 2B at ~1.4 GB) cannot be bundled in APK
 * assets. Instead, they are stored in the app's private files directory
 * and can be copied from an external source path on first run.
 *
 * The model is expected at:
 *   `context.filesDir/models/gemma-2b-it-gpu-int4.bin`
 */
class ModelManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelManager"
        private const val COPY_BUFFER_SIZE = 8 * 1024 * 1024 // 8 MB buffer for fast copy
    }

    /**
     * Check if the Gemma model file is available in app-private storage.
     */
    fun isModelAvailable(): Boolean {
        return getModelFile().exists()
    }

    /**
     * Get the absolute path to the model file in app-private storage.
     */
    fun getModelPath(): String {
        return getModelFile().absolutePath
    }

    /**
     * Get the model file size in megabytes, or 0 if not found.
     */
    fun getModelSizeMB(): Long {
        val file = getModelFile()
        return if (file.exists()) file.length() / 1_048_576 else 0
    }

    /**
     * Copy a model file from an external path into the app's private storage.
     *
     * @param sourcePath Absolute path to the source model file.
     * @param onProgress Callback with progress percentage (0-100).
     * @return true if copy was successful.
     */
    suspend fun copyModelFromPath(
        sourcePath: String,
        onProgress: ((Int) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) {
            Log.e(TAG, "Source file not found: $sourcePath")
            return@withContext false
        }

        val destFile = getModelFile()
        destFile.parentFile?.mkdirs()

        Log.i(TAG, "Copying model: ${sourceFile.name} (${sourceFile.length() / 1_048_576} MB)")
        Log.i(TAG, "  From: $sourcePath")
        Log.i(TAG, "  To:   ${destFile.absolutePath}")

        try {
            val totalBytes = sourceFile.length()
            var copiedBytes = 0L
            val buffer = ByteArray(COPY_BUFFER_SIZE)

            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        copiedBytes += bytesRead
                        val progress = ((copiedBytes * 100) / totalBytes).toInt()
                        onProgress?.invoke(progress)
                    }
                    output.flush()
                }
            }

            // Verify copy
            if (destFile.length() == totalBytes) {
                Log.i(TAG, "✅ Model copied successfully (${destFile.length() / 1_048_576} MB)")
                true
            } else {
                Log.e(TAG, "Copy verification failed: expected $totalBytes, got ${destFile.length()}")
                destFile.delete()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy model: ${e.message}", e)
            destFile.delete()
            false
        }
    }

    /**
     * Delete the model file from app-private storage to free disk space.
     */
    fun deleteModel(): Boolean {
        val file = getModelFile()
        return if (file.exists()) {
            val deleted = file.delete()
            Log.i(TAG, if (deleted) "Model deleted" else "Failed to delete model")
            deleted
        } else {
            true
        }
    }

    private fun getModelFile(): File {
        val modelsDir = File(context.filesDir, AiModelPaths.MODELS_DIR)
        return File(modelsDir, AiModelPaths.GEMMA_MODEL_FILENAME)
    }
}

