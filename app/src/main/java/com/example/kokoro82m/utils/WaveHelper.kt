package com.example.kokoro82m.utils

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun saveAudio(audioData: FloatArray, context: Context) {
    val sampleRate = 22050


    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "KOKORO_$timeStamp.wav")
        put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
    }


    val header = createWavHeader(audioData.size, sampleRate)


    val byteBuffer = ByteBuffer.allocate(audioData.size * 2)
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    val shortBuffer = byteBuffer.asShortBuffer()

    for (sample in audioData) {
        val pcmValue = (sample * Short.MAX_VALUE).toInt().toShort()
        shortBuffer.put(pcmValue)
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)

    uri?.let {
        try {
            resolver.openOutputStream(it)?.use { outputStream: OutputStream ->

                outputStream.write(header)

                outputStream.write(byteBuffer.array())
            }
            Log.d("Kokoro", "Audio saved to: $uri")
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "Audio saved to Music directory", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("Kokoro", "Error saving audio: ${e.message}")
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "Error saving audio", Toast.LENGTH_LONG).show()
            }
        }
    } ?: run {
        Log.e("Kokoro", "Failed to create audio file")
        (context as? Activity)?.runOnUiThread {
            Toast.makeText(context, "Failed to create audio file", Toast.LENGTH_LONG).show()
        }
    }
}

private fun createWavHeader(dataSize: Int, sampleRate: Int): ByteArray {
    val header = ByteArray(44)
    val totalDataSize = dataSize * 2 + 36
    val byteRate = sampleRate * 2


    header[0] = 'R'.code.toByte()
    header[1] = 'I'.code.toByte()
    header[2] = 'F'.code.toByte()
    header[3] = 'F'.code.toByte()


    header[4] = (totalDataSize and 0xff).toByte()
    header[5] = (totalDataSize shr 8 and 0xff).toByte()
    header[6] = (totalDataSize shr 16 and 0xff).toByte()
    header[7] = (totalDataSize shr 24 and 0xff).toByte()


    header[8] = 'W'.code.toByte()
    header[9] = 'A'.code.toByte()
    header[10] = 'V'.code.toByte()
    header[11] = 'E'.code.toByte()


    header[12] = 'f'.code.toByte()
    header[13] = 'm'.code.toByte()
    header[14] = 't'.code.toByte()
    header[15] = ' '.code.toByte()


    header[16] = 16
    header[17] = 0
    header[18] = 0
    header[19] = 0


    header[20] = 1
    header[21] = 0


    header[22] = 1
    header[23] = 0


    header[24] = (sampleRate and 0xff).toByte()
    header[25] = (sampleRate shr 8 and 0xff).toByte()
    header[26] = (sampleRate shr 16 and 0xff).toByte()
    header[27] = (sampleRate shr 24 and 0xff).toByte()


    header[28] = (byteRate and 0xff).toByte()
    header[29] = (byteRate shr 8 and 0xff).toByte()
    header[30] = (byteRate shr 16 and 0xff).toByte()
    header[31] = (byteRate shr 24 and 0xff).toByte()


    header[32] = 2
    header[33] = 0


    header[34] = 16
    header[35] = 0


    header[36] = 'd'.code.toByte()
    header[37] = 'a'.code.toByte()
    header[38] = 't'.code.toByte()
    header[39] = 'a'.code.toByte()


    header[40] = (dataSize * 2 and 0xff).toByte()
    header[41] = (dataSize * 2 shr 8 and 0xff).toByte()
    header[42] = (dataSize * 2 shr 16 and 0xff).toByte()
    header[43] = (dataSize * 2 shr 24 and 0xff).toByte()

    return header
}