package com.example.kokoro82m.utils

import com.github.medavox.ipa_transcribers.Language
import com.github.medavox.ipa_transcribers.Transcriber
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class G2pTest {
    private lateinit var g2pter: Transcriber

    @Before
    fun setUp() {
        g2pter = Language.ENGLISH.transcriber
    }

    @Test
    fun testPhoneticizeSingleWord() {
        val word = "hello"
        val pronunciations = g2pter.transcribe(word)

        assertTrue(pronunciations.isNotEmpty())

        println("Phonemes for '$word': $pronunciations")
    }

    @Test
    fun testPhoneticizeUnknownWord() {
        val word = "kokoro"
        val pronunciations = g2pter.transcribe(word)

        assertTrue(pronunciations.isNotEmpty())

        println("Phonemes for '$word': $pronunciations")
    }

    @Test
    fun testPhoneticizeSentence() {
        val word = "My kokoro is sabishii yo!"
        val pronunciations = g2pter.transcribe(word)

        assertTrue(pronunciations.isNotEmpty())

        println("Phonemes for '$word': $pronunciations")
    }
}