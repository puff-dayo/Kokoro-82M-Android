package com.example.kokoro82m.utils

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kokoro82m.R
import kotlinx.coroutines.launch
import java.io.InputStream

class MainViewModel(context: Context) : ViewModel() {
    init {
        viewModelScope.launch {
            OnnxRuntimeManager.initialize(context.applicationContext)
        }
    }

    fun getSession() = OnnxRuntimeManager.getSession()
}

object OnnxRuntimeManager {
    private var environment: OrtEnvironment? = null
    private var session: OrtSession? = null

    @Synchronized
    fun initialize(context: Context) {
        if (environment == null) {
            environment = OrtEnvironment.getEnvironment()
            session = createSession(context)
        }
    }

    private fun createSession(context: Context): OrtSession {
        val options = SessionOptions().apply {
            addConfigEntry("nnapi.flags", "USE_FP16")
            addConfigEntry("nnapi.use_gpu", "true")
            addConfigEntry("nnapi.gpu_precision_loss_allowed", "true")
        }

        return context.resources.openRawResource(R.raw.kokoro).use { stream ->
            environment!!.createSession(stream.readBytes(), options)
        }
    }

    fun getSession() = requireNotNull(session) { "ONNX Session not initialized" }
}
