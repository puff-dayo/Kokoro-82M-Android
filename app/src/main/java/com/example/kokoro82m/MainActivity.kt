package com.example.kokoro82m

import KokoroTheme
import LinkColorDark
import LinkColorLight
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioFormat.CHANNEL_OUT_MONO
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.kokoro82m.utils.PhonemeConverter
import com.example.kokoro82m.utils.createAudio
import com.example.kokoro82m.utils.saveAudio
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var phonemeConverter: PhonemeConverter
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KokoroTheme {
                LaunchedEffect(Unit) {
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                }
                KokoroInputScreen(
                    onGenerateAudio = { text, style, speed, shouldSave, onComplete ->
                        generateAudio(
                            phonemeConverter,
                            text,
                            style,
                            speed,
                            this@MainActivity,
                            scope,
                            shouldSave,
                            onComplete
                        )
                    }
                )
            }
        }

        phonemeConverter = PhonemeConverter(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

private fun playAudio(audioData: FloatArray, scope: CoroutineScope, onComplete: () -> Unit) {
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

private fun generateAudio(
    phonemeConverter: PhonemeConverter,
    text: String,
    style: String,
    speed: Float,
    context: Context,
    scope: CoroutineScope,
    shouldSave: Boolean,
    onComplete: () -> Unit
) {
    scope.launch(Dispatchers.IO) {
        try {
            val phonemes = phonemeConverter.phonemize(text)
            Log.d("Kokoro", "Phonemes: $phonemes")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Phonemes: $phonemes", Toast.LENGTH_LONG).show()
            }

            val session = loadModel(context)

            val (audioData, sampleRate) = createAudio(
                voice = style, phonemes = phonemes, speed = speed, context = context,
                session = session
            )

            playAudio(
                audioData, scope,
                onComplete = onComplete
            )

            if (shouldSave) {
                saveAudio(audioData, context)
            }

            session.close()
        } catch (e: Exception) {
            Log.e("Kokoro", "Error: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } finally {
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}

private fun loadModel(context: Context): OrtSession {
    val env = OrtEnvironment.getEnvironment()
    val options = SessionOptions()

    options.addConfigEntry("nnapi.flags", "USE_FP16")
    options.addConfigEntry("nnapi.use_gpu", "true")
    options.addConfigEntry("nnapi.gpu_precision_loss_allowed", "true")

    val modelStream: InputStream = context.resources.openRawResource(R.raw.kokoro)
    val modelBytes = modelStream.readBytes()
    return env.createSession(modelBytes, options)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KokoroInputScreen(
    onGenerateAudio: (String, String, Float, Boolean, () -> Unit) -> Unit
) {
    var text by remember { mutableStateOf("This is her warm heart, her warmest kokoro, unwavering love and comfort.") }
    var style by remember { mutableStateOf("af_sarah") }
    var speed by remember { mutableFloatStateOf(1.0f) }
    var isProcessing by remember { mutableStateOf(false) }
    var shouldSaveFile by remember { mutableStateOf(false) }

    val names = listOf(
        "af",
        "af_bella",
        "af_nicole",
        "af_sarah",
        "af_sky",
        "am_adam",
        "am_michael",
        "bf_emma",
        "bf_isabella",
        "bm_george",
        "bm_lewis"
    )
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Kokoro 82M (int8)") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = text,
                maxLines = 12,
                onValueChange = { text = it },
                label = { Text("Text to speak") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text
                )
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = style,
                    onValueChange = { style = it },
                    label = { Text("Style") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    names.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                style = name
                                expanded = false
                            }
                        )
                    }
                }
            }

            Text("Speed: $speed")
            Slider(
                value = speed,
                onValueChange = { speed = it },
                valueRange = 0.5f..2.0f,
                steps = 5,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        shouldSaveFile = false
                        isProcessing = true
                        onGenerateAudio(text, style, speed, shouldSaveFile) {
                            isProcessing = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    enabled = !isProcessing
                ) {
                    Text(if (isProcessing) "GPU Processing..." else "Play")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        shouldSaveFile = true
                        isProcessing = true
                        onGenerateAudio(text, style, speed, shouldSaveFile) {
                            isProcessing = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    enabled = !isProcessing
                ) {
                    Text(if (isProcessing) "GPU Processing..." else "Play & Save")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Acknowledgements()
        }
    }
}

@Composable
fun Acknowledgements() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = "Thank You for Making This Happen!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        ClickableLink(
            text = "Kokoro: a frontier TTS model (Apache 2.0)",
            url = "https://huggingface.co/hexgrad/Kokoro-82M",
            context = context
        )

        Spacer(modifier = Modifier.height(8.dp))

        ClickableLink(
            text = "Kokoro-ONNX: converted kokoro (MIT)",
            url = "https://github.com/thewh1teagle/kokoro-onnx",
            context = context
        )

        Spacer(modifier = Modifier.height(2.dp))

        ClickableLink(
            text = "CMU dict: a pronunciation dictionary",
            url = "http://www.speech.cs.cmu.edu/cgi-bin/cmudict",
            context = context
        )

        Spacer(modifier = Modifier.height(2.dp))

        ClickableLink(
            text = "IPA Transcribers: language transliterators (GPL-3.0)",
            url = "https://github.com/kotlinguistics/IPA-Transcribers",
            context = context
        )

        Spacer(modifier = Modifier.height(2.dp))

        ClickableLink(
            text = "Android NNAPI: a machine learning API",
            url = "https://developer.android.com/ndk/guides/neuralnetworks",
            context = context
        )

    }
}


@Composable
fun ClickableLink(text: String, url: String, context: Context) {

    val linkColor = if (!isSystemInDarkTheme()) {
        LinkColorLight
    } else {
        LinkColorDark
    }

    val annotatedString = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.Underline,
                fontSize = 14.sp
            )
        ) {
            append(text.substringBefore(":"))
            append(":")
        }

        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        ) {
            append(text.substringAfter(":"))
        }
    }

    Text(
        text = annotatedString,
        modifier = Modifier
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http$url"))
                context.startActivity(intent)
            }
            .padding(vertical = 4.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun KokoroInputScreenPreview() {
    KokoroInputScreen(
        onGenerateAudio = { text, style, speed, _, onComplete ->
            println("Generating audio for text: $text, style: $style, speed: $speed")
            onComplete()
        }
    )
}