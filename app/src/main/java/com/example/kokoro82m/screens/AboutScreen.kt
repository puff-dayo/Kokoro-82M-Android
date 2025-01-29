package com.example.kokoro82m.screens

import LinkColorDark
import LinkColorLight
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


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

        Spacer(modifier = Modifier.height(2.dp))

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
