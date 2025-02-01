package com.example.kokoro82m.utils

import android.media.AudioFormat
import android.media.AudioFormat.CHANNEL_OUT_MONO
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun playAudio(audioData: FloatArray, scope: CoroutineScope, onComplete: () -> Unit) {
    scope.launch(Dispatchers.IO) {
        val sampleRate = 22050
        val channelConfig = CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize,
            AudioTrack.MODE_STREAM
        )

        val byteBuffer = ByteBuffer.allocate(audioData.size * 2)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val shortBuffer = byteBuffer.asShortBuffer()

        for (sample in audioData) {
            val pcmValue = (sample * Short.MAX_VALUE).toInt().toShort()
            shortBuffer.put(pcmValue)
        }

        audioTrack.play()
        audioTrack.write(byteBuffer.array(), 0, byteBuffer.array().size)

        audioTrack.release()

        withContext(Dispatchers.Main) {
            onComplete()
        }
    }
}
