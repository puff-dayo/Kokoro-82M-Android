package com.example.kokoro82m.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PhonemeConverterTest {

    private lateinit var phonemeConverter: PhonemeConverter

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        phonemeConverter = PhonemeConverter(context)
    }

    @Test
    fun testPhonemize() {
        val text = "Hello, world! How are you?"
        val expectedPhonemes = "həˈloʊ, ˈwɝːld! ˈhaʊ ˈɑːɹ ˈjuː?"
        val actualPhonemes = phonemeConverter.phonemize(text)
        assertEquals(expectedPhonemes, actualPhonemes)
    }
}