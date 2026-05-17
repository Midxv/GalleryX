package com.app.galleryx.search

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.app.galleryx.model.database.entity.Photo
import com.app.galleryx.model.io.EncryptedStorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.LongBuffer
import javax.inject.Inject

class SearchEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptedStorageManager: EncryptedStorageManager
) {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()

    // Nullable so they aren't forced to load instantly on the Main UI Thread
    private var visionSession: OrtSession? = null
    private var textSession: OrtSession? = null
    private var vocab: Map<String, Int> = emptyMap()

    /**
     * Safely copies assets to cache and loads them into ONNX via file paths (Memory Mapping).
     * This prevents OutOfMemory errors and keeps the UI thread perfectly smooth.
     */
    private suspend fun ensureInitialized() = withContext(Dispatchers.IO) {
        try {
            if (visionSession == null) {
                val visionPath = getAssetFilePath("vision_model.onnx")
                visionSession = env.createSession(visionPath)

                // --- ADD THIS LOG ---
                val inputInfo = visionSession!!.inputInfo
                Log.d("AI_INDEXER", "VISION MODEL EXPECTS INPUT: $inputInfo")
            }
            if (textSession == null) {
                val textPath = getAssetFilePath("text_model.onnx")
                textSession = env.createSession(textPath)
                val inputInfo = textSession!!.inputInfo
                Log.d("AI_INDEXER", "TEXT MODEL EXPECTS INPUT: $inputInfo")
            }
            if (vocab.isEmpty()) {
                val vocabJsonString = context.assets.open("vocab.json").bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(vocabJsonString)
                val tempVocab = mutableMapOf<String, Int>()
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    tempVocab[key] = jsonObject.getInt(key)
                }
                vocab = tempVocab
            }
        } catch (e: Exception) {
            Log.e("AI_INDEXER", "FAILED TO INITIALIZE AI MODELS!", e)
            throw e
        }
    }

    /**
     * Extracts an asset to the cache directory so ONNX can memory-map it directly from the SSD.
     */
    private fun getAssetFilePath(assetName: String): String {
        val file = File(context.cacheDir, assetName)
        if (!file.exists()) {
            context.assets.open(assetName).use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        return file.absolutePath
    }

    suspend fun indexPhoto(photo: Photo): ByteArray? = withContext(Dispatchers.IO) {
        ensureInitialized()

        var bitmap: Bitmap? = null
        var inputStream: java.io.InputStream? = null

        return@withContext try {
            inputStream = encryptedStorageManager.internalOpenEncryptedFileInput(photo.internalFileName)
            if (inputStream == null) return@withContext null

            // If it's a video file, BitmapFactory will naturally return null here,
            // gracefully skipping the video without crashing.
            bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap == null) return@withContext null

            val tensor = preprocess(bitmap, env)

            val inputName = visionSession!!.inputNames.iterator().next()
            val result = visionSession!!.run(mapOf(inputName to tensor))

            @Suppress("UNCHECKED_CAST")
            val embeddingArray = result[0].value as Array<FloatArray>
            val vector = embeddingArray[0]

            tensor.close()
            result.close()

            floatArrayToByteArray(vector)

        } catch (e: Exception) {
            Log.e("AI_INDEXER", "Exception thrown while processing ${photo.fileName}", e)
            null
        } finally {
            inputStream?.close()
            bitmap?.recycle()
        }
    }

    suspend fun getQueryEmbedding(query: String): FloatArray? = withContext(Dispatchers.Default) {
        ensureInitialized()

        return@withContext try {
            val tokens = tokenizeText(query)
            val shape = longArrayOf(1, 77)
            val tensor = OnnxTensor.createTensor(env, LongBuffer.wrap(tokens), shape)

            val inputName = textSession!!.inputNames.iterator().next()
            val result = textSession!!.run(mapOf(inputName to tensor))

            @Suppress("UNCHECKED_CAST")
            val embeddingArray = result[0].value as Array<FloatArray>
            val vector = embeddingArray[0]

            tensor.close()
            result.close()

            vector
        } catch (e: Exception) {
            Log.e("AI_INDEXER", "Exception generating query vector", e)
            null
        }
    }

    fun cosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i] * vectorA[i]
            normB += vectorB[i] * vectorB[i]
        }
        return if (normA == 0.0f || normB == 0.0f) 0.0f
        else (dotProduct / (Math.sqrt(normA.toDouble()) * Math.sqrt(normB.toDouble()))).toFloat()
    }

    private fun preprocess(bitmap: Bitmap, env: OrtEnvironment): OnnxTensor {
        val dimension = Math.min(bitmap.width, bitmap.height)
        val x = (bitmap.width - dimension) / 2
        val y = (bitmap.height - dimension) / 2
        val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, dimension, dimension)

        // --- THE FIX: Xenova explicitly needs 224x224 ---
        val resized = Bitmap.createScaledBitmap(croppedBitmap, 224, 224, true)

        val floatBuffer = FloatBuffer.allocate(3 * 224 * 224)
        val pixels = IntArray(224 * 224)
        resized.getPixels(pixels, 0, 224, 0, 0, 224, 224)

        // Standard OpenAI CLIP Color Normalization
        val normMeanRGB = floatArrayOf(0.48145466f, 0.4578275f, 0.40821073f)
        val normStdRGB = floatArrayOf(0.26862954f, 0.26130258f, 0.27577711f)

        val rOffset = 0
        val gOffset = 224 * 224
        val bOffset = 2 * 224 * 224

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = ((pixel shr 16 and 0xFF) / 255.0f - normMeanRGB[0]) / normStdRGB[0]
            val g = ((pixel shr 8 and 0xFF) / 255.0f - normMeanRGB[1]) / normStdRGB[1]
            val b = ((pixel and 0xFF) / 255.0f - normMeanRGB[2]) / normStdRGB[2]

            floatBuffer.put(rOffset + i, r)
            floatBuffer.put(gOffset + i, g)
            floatBuffer.put(bOffset + i, b)
        }

        croppedBitmap.recycle()
        resized.recycle()

        // --- THE FIX: Tell the tensor it's 224x224 ---
        val shape = longArrayOf(1, 3, 224, 224)
        return OnnxTensor.createTensor(env, floatBuffer, shape)
    }

    private fun tokenizeText(query: String): LongArray {
        val tokens = LongArray(77) { 0L }
        tokens[0] = 49406L

        val words = query.lowercase().replace(Regex("[^a-z0-9 ]"), "").split(" ")

        var tokenIndex = 1
        for (word in words) {
            if (word.isBlank() || tokenIndex >= 76) continue

            val clipWord = "$word</w>"
            val tokenId = vocab[clipWord] ?: vocab[word]

            if (tokenId != null) {
                tokens[tokenIndex] = tokenId.toLong()
                tokenIndex++
            }
        }

        tokens[tokenIndex] = 49407L
        return tokens
    }

    private fun floatArrayToByteArray(floatArray: FloatArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(floatArray.size * 4)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.asFloatBuffer().put(floatArray)
        return byteBuffer.array()
    }

    fun byteArrayToFloatArray(byteArray: ByteArray): FloatArray {
        val floatBuffer = ByteBuffer.wrap(byteArray)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asFloatBuffer()
        val floatArray = FloatArray(floatBuffer.limit())
        floatBuffer.get(floatArray)
        return floatArray
    }
}