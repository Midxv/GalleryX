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
import kotlin.math.sqrt

class SearchEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptedStorageManager: EncryptedStorageManager
) {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()

    private var visionSession: OrtSession? = null
    private var textSession: OrtSession? = null
    private var vocab: Map<String, Int> = emptyMap()

    private val IMAGE_SIZE = 224

    private suspend fun ensureInitialized() = withContext(Dispatchers.IO) {
        try {
            if (visionSession == null) {
                // Ensure your OLD models are back in the assets folder!
                val visionPath = getAssetFilePath("vision_model.onnx")
                visionSession = env.createSession(visionPath)
                Log.d("AI_INDEXER", "VISION MODEL LOADED SUCCESS!")
            }
            if (textSession == null) {
                val textPath = getAssetFilePath("text_model.onnx")
                textSession = env.createSession(textPath)
                Log.d("AI_INDEXER", "TEXT MODEL LOADED SUCCESS!")
            }

            // --- REVERTED: Standard Flat JSON Vocab Parser ---
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
                Log.d("AI_INDEXER", "VOCAB PARSED SUCCESS! Size: ${vocab.size}")
            }
        } catch (e: Exception) {
            Log.e("AI_INDEXER", "FAILED TO INITIALIZE MODELS!", e)
            throw e
        }
    }

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
            val fileNameToLoad = if (photo.type.isVideo) {
                photo.internalVideoPreviewFileName
            } else {
                photo.internalFileName
            }

            inputStream = encryptedStorageManager.internalOpenEncryptedFileInput(fileNameToLoad)

            if (inputStream == null && photo.type.isVideo) {
                inputStream = encryptedStorageManager.internalOpenEncryptedFileInput(photo.internalThumbnailFileName)
            }

            if (inputStream == null) return@withContext null

            bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap == null) return@withContext null

            val tensor = preprocessForModel(bitmap, env)

            val inputName = visionSession!!.inputNames.iterator().next()
            val result = visionSession!!.run(mapOf(inputName to tensor))

            // --- REVERTED: Standard 2D FloatArray Extraction ---
            @Suppress("UNCHECKED_CAST")
            val embeddingArray = result[0].value as Array<FloatArray>
            val vector = embeddingArray[0]

            tensor.close()
            result.close()

            floatArrayToByteArray(vector)

        } catch (e: Exception) {
            Log.e("AI_INDEXER", "Exception processing ${photo.fileName}", e)
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
            val shape = longArrayOf(1, 77) // Standard CLIP sequence length
            val tensor = OnnxTensor.createTensor(env, LongBuffer.wrap(tokens), shape)

            val inputName = textSession!!.inputNames.iterator().next()
            val result = textSession!!.run(mapOf(inputName to tensor))

            // --- REVERTED: Standard 2D FloatArray Extraction ---
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

    private fun preprocessForModel(bitmap: Bitmap, env: OrtEnvironment): OnnxTensor {
        val dimension = Math.min(bitmap.width, bitmap.height)
        val x = (bitmap.width - dimension) / 2
        val y = (bitmap.height - dimension) / 2
        val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, dimension, dimension)

        val resized = Bitmap.createScaledBitmap(croppedBitmap, IMAGE_SIZE, IMAGE_SIZE, true)

        val floatArray = FloatArray(3 * IMAGE_SIZE * IMAGE_SIZE)
        val pixels = IntArray(IMAGE_SIZE * IMAGE_SIZE)
        resized.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE)

        // Standard CLIP Normalization math
        val normMeanR = 0.48145466f
        val normMeanG = 0.4578275f
        val normMeanB = 0.40821073f
        val normStdR = 0.26862954f
        val normStdG = 0.26130258f
        val normStdB = 0.27577711f

        val rOffset = 0
        val gOffset = IMAGE_SIZE * IMAGE_SIZE
        val bOffset = 2 * IMAGE_SIZE * IMAGE_SIZE

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = ((pixel shr 16 and 0xFF) / 255.0f - normMeanR) / normStdR
            val g = ((pixel shr 8 and 0xFF) / 255.0f - normMeanG) / normStdG
            val b = ((pixel and 0xFF) / 255.0f - normMeanB) / normStdB

            floatArray[rOffset + i] = r
            floatArray[gOffset + i] = g
            floatArray[bOffset + i] = b
        }

        croppedBitmap.recycle()
        resized.recycle()

        val floatBuffer = FloatBuffer.wrap(floatArray)
        val shape = longArrayOf(1, 3, IMAGE_SIZE.toLong(), IMAGE_SIZE.toLong())
        return OnnxTensor.createTensor(env, floatBuffer, shape)
    }

    private fun tokenizeText(query: String): LongArray {
        // Standard CLIP sequence length is 77
        val tokens = LongArray(77) { 0L }
        val words = query.lowercase().replace(Regex("[^a-z0-9 ]"), "").split(" ")

        var tokenIndex = 0
        tokens[tokenIndex++] = 49406L // Standard CLIP Start of Sequence token

        for (word in words) {
            if (word.isBlank() || tokenIndex >= 75) continue

            val tokenId = vocab[word]
            if (tokenId != null) {
                tokens[tokenIndex] = tokenId.toLong()
                tokenIndex++
            }
        }

        tokens[tokenIndex] = 49407L // Standard CLIP End of Sequence token
        return tokens
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
        else (dotProduct / (sqrt(normA.toDouble()) * sqrt(normB.toDouble()))).toFloat()
    }

    private fun floatArrayToByteArray(floatArray: FloatArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(floatArray.size * 4)
            .order(ByteOrder.LITTLE_ENDIAN)
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