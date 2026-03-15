package com.lockin.app.ai

/**
 * AIModel – Base abstraction for on-device text-generative AI models.
 *
 * Implementations wrap a TFLite interpreter (or similar) and expose a
 * simple generate(prompt)->response API. The rest of the app never
 * touches TFLite directly; it depends on this interface instead.
 *
 * All heavy operations are suspend functions so they can safely run on
 * background dispatchers without blocking the main thread.
 */
interface AIModel {

    /**
     * Load the model into memory and prepare the interpreter.
     * Must be called before [generate]. Safe to call multiple times
     * (subsequent calls are no-ops if already loaded).
     *
     * @return true if the model loaded successfully, false otherwise.
     */
    suspend fun initialize(): Boolean

    /**
     * Run text generation on [input] and return the model's response.
     * If the model is not loaded, returns a friendly error string
     * rather than throwing.
     */
    suspend fun generate(input: String): String

    /** Whether the model is currently loaded and ready for inference. */
    fun isLoaded(): Boolean

    /**
     * Release the model, interpreter, and any GPU/NNAPI delegates.
     * After calling this, [isLoaded] returns false and [generate]
     * returns an error string until [initialize] is called again.
     */
    fun release()
}

