package com.lockin.app.ai.inference

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.lockin.app.ai.AIModel
import com.lockin.app.ai.models.AiModelPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.security.MessageDigest

private const val TAG = "GemmaModel"

/**
 * Gemma model wrapper.
 *
 * Runtime strategy:
 * 1) Prefer MediaPipe GenAI + .bin (works with gemma-2b-it-gpu-int4.bin)
 * 2) Fall back to TFLite .tflite path when available
 * 3) Never crash; return friendly fallback responses on failures
 */
class GemmaModel(private val context: Context) : AIModel {

    private enum class Backend {
        NONE,
        MEDIAPIPE_BIN,
        TFLITE
    }

    private var backend: Backend = Backend.NONE

    // MediaPipe backend
    private var llmInference: LlmInference? = null

    // TFLite backend (reflection)
    private var interpreter: Any? = null
    private var runMethod: Method? = null
    private var closeMethod: Method? = null

    private var isLoaded = false

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isLoaded) return@withContext true

        // Try MediaPipe .bin first (this matches your bundled model file).
        if (initializeMediaPipeBackend()) {
            isLoaded = true
            backend = Backend.MEDIAPIPE_BIN
            Log.i(TAG, "Gemma initialized with MediaPipe backend")
            return@withContext true
        }

        // Fallback to TFLite for compatibility with smaller .tflite models.
        if (initializeTfliteBackend()) {
            isLoaded = true
            backend = Backend.TFLITE
            Log.i(TAG, "Gemma initialized with TFLite backend")
            return@withContext true
        }

        isLoaded = false
        backend = Backend.NONE
        Log.w(TAG, "Gemma initialization failed: no compatible model backend could be loaded")
        return@withContext false
    }

    override suspend fun generate(input: String): String = withContext(Dispatchers.Default) {
        if (!isLoaded) {
            return@withContext fallbackResponse(input, "Model not loaded")
        }

        return@withContext when (backend) {
            Backend.MEDIAPIPE_BIN -> generateWithMediaPipe(input)
            Backend.TFLITE -> generateWithTflite(input)
            Backend.NONE -> fallbackResponse(input, "Backend unavailable")
        }
    }

    override fun isLoaded(): Boolean = isLoaded

    override fun release() {
        try {
            llmInference?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing MediaPipe LLM: ${e.message}")
        }
        llmInference = null

        try {
            closeMethod?.invoke(interpreter)
        } catch (e: Exception) {
            Log.w(TAG, "Error closing TFLite interpreter: ${e.message}")
        }

        interpreter = null
        runMethod = null
        closeMethod = null
        backend = Backend.NONE
        isLoaded = false
    }

    // ── MediaPipe backend ───────────────────────────────────────────────

    private fun initializeMediaPipeBackend(): Boolean {
        return try {
            val binFile = ensureBinModelFile()
            if (binFile == null || !binFile.exists() || binFile.length() <= 0L) {
                Log.i(TAG, "No .bin model available for MediaPipe backend")
                return false
            }

            val optionsBuilder = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(binFile.absolutePath)
                .setMaxTokens(MEDIAPIPE_MAX_TOKENS)
                .setMaxTopK(MEDIAPIPE_TOP_K)

            // Prefer GPU on S24 Ultra; fallback to DEFAULT if unavailable.
            val options = try {
                optionsBuilder
                    .setPreferredBackend(LlmInference.Backend.GPU)
                    .build()
            } catch (_: Exception) {
                optionsBuilder
                    .setPreferredBackend(LlmInference.Backend.DEFAULT)
                    .build()
            }

            llmInference = LlmInference.createFromOptions(context, options)
            llmInference != null
        } catch (e: Exception) {
            Log.e(TAG, "MediaPipe backend init failed: ${e.message}", e)
            llmInference = null
            false
        }
    }

    private fun ensureBinModelFile(): File? {
        val modelsDir = File(context.filesDir, AiModelPaths.MODELS_DIR)
        if (!modelsDir.exists()) modelsDir.mkdirs()

        val target = File(modelsDir, AiModelPaths.GEMMA_MODEL_FILENAME)
        if (target.exists() && target.length() > 0L) {
            return target
        }

        val assetPath = "${AiModelPaths.MODELS_DIR}/${AiModelPaths.GEMMA_MODEL_FILENAME}"
        return try {
            Log.i(TAG, "Copying bundled .bin model from assets/$assetPath to ${target.absolutePath}")
            context.assets.open(assetPath).use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }

            if (target.length() > 0L) target else null
        } catch (e: Exception) {
            Log.w(TAG, "Bundled .bin model not found or copy failed: ${e.message}")
            null
        }
    }

    private fun generateWithMediaPipe(input: String): String {
        val llm = llmInference ?: return fallbackResponse(input, "MediaPipe model unavailable")
        return try {
            llm.generateResponse(input)
        } catch (e: Exception) {
            Log.e(TAG, "MediaPipe generation failed: ${e.message}", e)
            fallbackResponse(input, "MediaPipe inference error")
        }
    }

    // ── TFLite backend ──────────────────────────────────────────────────

    private fun initializeTfliteBackend(): Boolean {
        val modelBuffer = loadTfliteModelFromStorageOrAssets() ?: return false

        return try {
            val interpreterClass = Class.forName("org.tensorflow.lite.Interpreter")
            val ctor: Constructor<*> = interpreterClass.getConstructor(java.nio.MappedByteBuffer::class.java)
            val instance = ctor.newInstance(modelBuffer)
            interpreter = instance
            runMethod = interpreterClass.getMethod("run", Any::class.java, Any::class.java)
            closeMethod = interpreterClass.getMethod("close")
            true
        } catch (e: Exception) {
            Log.e(TAG, "TFLite backend init failed: ${e.message}", e)
            interpreter = null
            runMethod = null
            closeMethod = null
            false
        }
    }

    private fun loadTfliteModelFromAssets(path: String): MappedByteBuffer? {
        return try {
            val afd: AssetFileDescriptor = context.assets.openFd(path)
            FileInputStream(afd.fileDescriptor).channel.map(
                FileChannel.MapMode.READ_ONLY,
                afd.startOffset,
                afd.declaredLength
            )
        } catch (e: Exception) {
            Log.w(TAG, "Unable to open TFLite model at $path: ${e.message}")
            null
        }
    }

    private fun loadTfliteModelFromStorageOrAssets(): MappedByteBuffer? {
        val localFile = File(
            File(context.filesDir, AiModelPaths.MODELS_DIR),
            AiModelPaths.GEMMA_TFLITE_FILENAME
        )

        if (localFile.exists() && localFile.length() > 0L) {
            return try {
                Log.i(TAG, "Loading TFLite model from internal storage: ${localFile.absolutePath}")
                FileInputStream(localFile).channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    0,
                    localFile.length()
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed loading local TFLite model: ${e.message}")
                null
            }
        }

        Log.i(TAG, "Loading TFLite model from assets/${AiModelPaths.GEMMA_TFLITE_PATH}")
        return loadTfliteModelFromAssets(AiModelPaths.GEMMA_TFLITE_PATH)
    }

    private fun generateWithTflite(input: String): String {
        val interp = interpreter
        val run = runMethod
        if (interp == null || run == null) {
            return fallbackResponse(input, "TFLite model unavailable")
        }

        return try {
            val inputIds = tokenize(input)
            val outputIds = IntArray(MAX_TOKENS)
            run.invoke(interp, arrayOf(inputIds), arrayOf(outputIds))
            decode(outputIds)
        } catch (e: Exception) {
            Log.e(TAG, "TFLite inference failed: ${e.message}", e)
            fallbackResponse(input, "TFLite inference error")
        }
    }

    // ── Shared helpers ──────────────────────────────────────────────────

    private fun tokenize(text: String): IntArray {
        val words = text.split(" ", "\n", "\t").filter { it.isNotBlank() }
        val tokens = IntArray(MAX_TOKENS)
        val digest = MessageDigest.getInstance("MD5")
        for (i in tokens.indices) {
            val word = words.getOrNull(i) ?: "<pad>"
            val hash = digest.digest(word.toByteArray()).take(2)
                .fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }
            tokens[i] = (hash % VOCAB_SIZE).coerceAtLeast(1)
        }
        return tokens
    }

    private fun decode(ids: IntArray): String {
        val builder = StringBuilder()
        ids.take(OUT_TOKENS_TO_READ).forEach { id ->
            if (id > 0) builder.append("tok").append(id).append(' ')
        }
        val text = builder.toString().trim()
        return if (text.isBlank()) {
            "Let's focus on steady progress: keep form clean and stay consistent."
        } else {
            text
        }
    }

    private fun fallbackResponse(input: String, reason: String): String {
        return "Model unavailable ($reason). Quick tip: prioritize good sleep, progressive overload, and clean form for steady gains. You asked: \"$input\"."
    }

    companion object {
        private const val MAX_TOKENS = 128
        private const val OUT_TOKENS_TO_READ = 32
        private const val VOCAB_SIZE = 32000

        private const val MEDIAPIPE_MAX_TOKENS = 256
        private const val MEDIAPIPE_TOP_K = 40
    }
}
