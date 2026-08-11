package com.example.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class ThousandSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val formattedText = try {
            val longValue = originalText.toLong()
            String.format("%,d", longValue).replace(",", ".")
        } catch (e: Exception) {
            originalText
        }

        val annotatedString = AnnotatedString(formattedText)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val substring = originalText.substring(0, offset.coerceAtMost(originalText.length))
                val formattedSubstring = try {
                    val longValue = substring.toLong()
                    String.format("%,d", longValue).replace(",", ".")
                } catch (e: Exception) {
                    substring
                }
                return formattedSubstring.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val transformedSubstring = formattedText.substring(0, offset.coerceAtMost(formattedText.length))
                return transformedSubstring.replace(".", "").length
            }
        }

        return TransformedText(annotatedString, offsetMapping)
    }
}
