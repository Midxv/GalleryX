package com.app.galleryx.videoplayer.ui

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.app.galleryx.security.EncryptionManager
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Feeds decrypted bytes directly into ExoPlayer's memory buffer completely offline.
 */
class EncryptedFileDataSource(
    private val encryptionManager: EncryptionManager
) : BaseDataSource(/* isNetwork = */ false) {

    private var cipherInputStream: InputStream? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = 0
    private var opened: Boolean = false

    override fun open(dataSpec: DataSpec): Long {
        try {
            uri = dataSpec.uri
            val file = File(uri!!.path!!)
            val fileInputStream = FileInputStream(file)

            cipherInputStream = encryptionManager.createCipherInputStream(fileInputStream)
                ?: throw IOException("Failed to initialize CipherInputStream")

            // Safely skip bytes to support the user jumping/seeking through the timeline
            var bytesToSkip = dataSpec.position
            val skipBuffer = ByteArray(8192)
            while (bytesToSkip > 0) {
                val read = cipherInputStream!!.read(skipBuffer, 0, minOf(bytesToSkip, skipBuffer.size.toLong()).toInt())
                if (read == -1) break
                bytesToSkip -= read
            }

            // Encrypted Format V1 Header is 33 bytes (1 byte version + 16 bytes salt + 16 bytes IV)
            val approximateSize = (file.length() - 33).coerceAtLeast(1)

            bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                approximateSize - dataSpec.position
            }

            opened = true
            transferStarted(dataSpec)

            return bytesRemaining
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val bytesToRead = if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            minOf(bytesRemaining, length.toLong()).toInt()
        } else {
            length
        }

        val bytesRead = try {
            cipherInputStream!!.read(buffer, offset, bytesToRead)
        } catch (e: Exception) {
            throw IOException(e)
        }

        if (bytesRead == -1) return C.RESULT_END_OF_INPUT

        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= bytesRead
        }
        bytesTransferred(bytesRead)

        return bytesRead
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        try {
            cipherInputStream?.close()
        } catch (e: Exception) {
            throw IOException(e)
        } finally {
            cipherInputStream = null
            if (opened) {
                opened = false
                transferEnded()
            }
        }
    }
}