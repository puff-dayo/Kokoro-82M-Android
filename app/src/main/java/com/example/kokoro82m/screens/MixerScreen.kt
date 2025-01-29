import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.kokoro82m.loadModel
import com.example.kokoro82m.playAudio
import com.example.kokoro82m.utils.PhonemeConverter
import com.example.kokoro82m.utils.StyleLoader
import com.example.kokoro82m.utils.createAudio
import com.example.kokoro82m.utils.saveAudio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt


@Composable
fun MixerScreen(
    styleLoader: StyleLoader,
    onMixApplied: (FloatArray) -> Unit 
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    
    var selectedStyles by remember {
        mutableStateOf(listOf("af_sarah", "am_adam"))
    }
    var weights by remember {
        mutableStateOf(mapOf("af_sarah" to 0.5f, "am_adam" to 0.5f))
    }
    var interpolationMode by remember {
        mutableStateOf(InterpolationMode.LINEAR)
    }
    var isLoading by remember { mutableStateOf(false) }

    
    val styleNames = styleLoader.names

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        StyleSelector(
            styleNames = styleNames,
            selectedStyles = selectedStyles,
            onAddStyle = { style ->
                selectedStyles = selectedStyles.toMutableList().apply { add(style) }
                weights = weights.toMutableMap().apply { put(style, 1f) }
            },
            onRemoveStyle = { style ->
                selectedStyles = selectedStyles.toMutableList().apply { remove(style) }
                weights = weights.toMutableMap().apply { remove(style) }
            }
        )

        
        WeightSliders(
            selectedStyles = selectedStyles,
            weights = weights,
            onWeightChanged = { style, value ->
                weights = weights.toMutableMap().apply { put(style, value) }
            }
        )

        
        InterpolationModeSelector(
            currentMode = interpolationMode,
            onModeSelected = { interpolationMode = it }
        )

        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        val mixedVector = mixStyles(
                            styleLoader = styleLoader,
                            styles = selectedStyles,
                            weights = weights,
                            mode = interpolationMode,
                            context = context
                        )
                        onMixApplied(mixedVector)
                        isLoading = false
                    }
                },
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Mixing..." else "Apply Mix")
            }

            Button(
                onClick = {
                    
                    selectedStyles = listOf("af_sarah", "am_adam")
                    weights = mapOf("af_sarah" to 0.5f, "am_adam" to 0.5f)
                }
            ) {
                Text("Reset")
            }
        }
    }
}


private suspend fun mixStyles(
    styleLoader: StyleLoader,
    styles: List<String>,
    weights: Map<String, Float>,
    mode: InterpolationMode,
    context: Context
): FloatArray = withContext(Dispatchers.Default) {
    require(styles.isNotEmpty()) { "At least one style must be selected" }
    require(styles.all { it in weights }) { "All styles must have weights" }

    
    val styleVectors = styles.map { styleName ->
        styleLoader.getStyleArray(styleName).first() 
    }

    
    val totalWeight = weights.values.sum()
    val normalizedWeights = weights.values.map { it / totalWeight }

    
    when (mode) {
        InterpolationMode.LINEAR -> linearInterpolation(styleVectors, normalizedWeights)
        InterpolationMode.SPHERICAL -> sphericalInterpolation(styleVectors, normalizedWeights)
    }
}

private fun linearInterpolation(vectors: List<FloatArray>, weights: List<Float>): FloatArray {
    return FloatArray(256) { i ->
        vectors.mapIndexed { idx, vec -> vec[i] * weights[idx] }.sum()
    }
}

private fun sphericalInterpolation(vectors: List<FloatArray>, weights: List<Float>): FloatArray {
    
    val normalizedVectors = vectors.map { vec ->
        val norm = sqrt(vec.sumOf { it.toDouble().pow(2) })
        vec.map { (it / norm).toFloat() }.toFloatArray()
    }

    return FloatArray(256) { i ->
        val components = normalizedVectors.map { it[i] }
        val dotProduct = components.zip(weights) { c, w -> c * w }.sum()
        dotProduct / components.size.toFloat()
    }
}


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun StyleSelector(
    styleNames: List<String>,
    selectedStyles: List<String>,
    onAddStyle: (String) -> Unit,
    onRemoveStyle: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text("Selected Styles:", style = MaterialTheme.typography.labelLarge)

        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedStyles.forEach { style ->
                SuggestionChip(
                    onClick = { onRemoveStyle(style) },
                    label = { Text(style) },
                    icon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove"
                        )
                    }






                )
            }
        }

        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = "",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                placeholder = { Text("Add style...") },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                styleNames.filter { it !in selectedStyles }.forEach { style ->
                    DropdownMenuItem(
                        text = { Text(style) },
                        onClick = {
                            onAddStyle(style)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightSliders(
    selectedStyles: List<String>,
    weights: Map<String, Float>,
    onWeightChanged: (String, Float) -> Unit
) {
    Column {
        Text("Style Weights:", style = MaterialTheme.typography.labelLarge)

        selectedStyles.forEach { style ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = style, modifier = Modifier.width(120.dp))
                Slider(
                    value = weights[style] ?: 0f,
                    onValueChange = { onWeightChanged(style, it) },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f)
                )
                Text(text = "%.2f".format(weights[style] ?: 0f))
            }
        }
    }
}

@Composable
private fun InterpolationModeSelector(
    currentMode: InterpolationMode,
    onModeSelected: (InterpolationMode) -> Unit
) {
    Column {
        Text("Interpolation Mode:", style = MaterialTheme.typography.labelLarge)

        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            InterpolationMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onModeSelected(mode) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentMode == mode,
                        onClick = { onModeSelected(mode) }
                    )
                    Text(mode.displayName)
                }
            }
        }
    }
}

enum class InterpolationMode(val displayName: String) {
    LINEAR("Linear"),
    SPHERICAL("Spherical")
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

            val (audioData, _) = createAudio(
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
