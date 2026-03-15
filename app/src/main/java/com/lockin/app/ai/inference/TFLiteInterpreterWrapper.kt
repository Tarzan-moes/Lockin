@file:Suppress("INACCESSIBLE_TYPE")

package com.lockin.app.ai.inference

import android.util.Log
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer

/**
 * TFLiteInterpreterWrapper – Wraps TensorFlow Lite Interpreter via reflection.
 *
 * This avoids compile-time issues with the TFLite `-api` module exclusion
 * needed for AGP 9 compatibility. All TFLite methods are called via
 * the concrete Interpreter class which is present at runtime even when
 * the `-api` supertype module is excluded.
 *
 * The wrapper provides a thin, safe layer that catches any ClassNotFound
 * or reflection errors and returns false/null gracefully.
 */
class TFLiteInterpreterWrapper {

    companion object {
        private const val TAG = "TFLiteWrapper"
    }

    private var interpreterInstance: Any? = null

    /**
     * Load a TFLite model from a memory-mapped buffer with given options.
     *
     * @param modelBuffer The memory-mapped model file.
     * @param numThreads Number of CPU threads to use.
     * @param useNnapi Whether to attempt NNAPI delegate.
     * @param useGpu Whether to attempt GPU delegate.
     * @return true if the interpreter was created successfully.
     */
    fun load(
        modelBuffer: MappedByteBuffer,
        numThreads: Int = 4,
        useNnapi: Boolean = true,
        useGpu: Boolean = true
    ): Boolean {
        return try {
            val optionsClass = Class.forName("org.tensorflow.lite.Interpreter\$Options")
            val options = optionsClass.getDeclaredConstructor().newInstance()

            // Set thread count
            try {
                val setNumThreads = optionsClass.getMethod("setNumThreads", Int::class.javaPrimitiveType)
                setNumThreads.invoke(options, numThreads)
            } catch (e: Exception) {
                Log.d(TAG, "Could not set numThreads: ${e.message}")
            }

            // Try GPU delegate
            if (useGpu && tryAttachGpuDelegate(options, optionsClass)) {
                Log.i(TAG, "GPU delegate attached")
            } else if (useNnapi) {
                // Try NNAPI
                try {
                    val setUseNNAPI = optionsClass.getMethod("setUseNNAPI", Boolean::class.javaPrimitiveType)
                    setUseNNAPI.invoke(options, true)
                    Log.i(TAG, "NNAPI enabled")
                } catch (e: Exception) {
                    Log.d(TAG, "NNAPI not available: ${e.message}")
                }
            }

            // Create interpreter
            val interpreterClass = Class.forName("org.tensorflow.lite.Interpreter")
            val constructor = interpreterClass.getConstructor(
                MappedByteBuffer::class.java, optionsClass
            )
            interpreterInstance = constructor.newInstance(modelBuffer, options)
            Log.i(TAG, "TFLite interpreter created successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create TFLite interpreter: ${e.message}", e)
            false
        }
    }

    /**
     * Run inference with given input and output buffers.
     */
    fun run(input: ByteBuffer, output: ByteBuffer): Boolean {
        val interpreter = interpreterInstance ?: return false
        return try {
            val runMethod = interpreter.javaClass.getMethod(
                "run", Any::class.java, Any::class.java
            )
            runMethod.invoke(interpreter, input, output)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed: ${e.message}", e)
            false
        }
    }

    /**
     * Close the interpreter and release resources.
     */
    fun close() {
        try {
            interpreterInstance?.let {
                val closeMethod = it.javaClass.getMethod("close")
                closeMethod.invoke(it)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error closing interpreter: ${e.message}")
        }
        interpreterInstance = null
    }

    fun isLoaded(): Boolean = interpreterInstance != null

    // ── Private helpers ──────────────────────────────────────────────────

    private fun tryAttachGpuDelegate(options: Any, optionsClass: Class<*>): Boolean {
        return try {
            val compatListClass = Class.forName("org.tensorflow.lite.gpu.CompatibilityList")
            val compatList = compatListClass.getDeclaredConstructor().newInstance()
            val isSupported = compatListClass.getMethod("isDelegateSupportedOnThisDevice")
                .invoke(compatList) as Boolean

            if (isSupported) {
                val bestOptions = compatListClass.getMethod("getBestOptionsForThisDevice")
                    .invoke(compatList)
                val gpuDelegateClass = Class.forName("org.tensorflow.lite.gpu.GpuDelegate")
                val gpuConstructor = gpuDelegateClass.constructors.firstOrNull {
                    it.parameterCount == 1
                }
                val gpuDelegate = gpuConstructor?.newInstance(bestOptions)
                if (gpuDelegate != null) {
                    val delegateClass = Class.forName("org.tensorflow.lite.Delegate")
                    val addDelegateMethod = optionsClass.getMethod("addDelegate", delegateClass)
                    addDelegateMethod.invoke(options, gpuDelegate)
                    true
                } else false
            } else false
        } catch (e: Exception) {
            Log.d(TAG, "GPU delegate not available: ${e.message}")
            false
        }
    }
}

