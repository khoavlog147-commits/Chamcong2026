package com.example.ui.screens

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class TimeVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }
        val trimmed = if (raw.length > 4) raw.substring(0, 4) else raw
        
        var out = ""
        for (i in trimmed.indices) {
            // If 3 digits: x:xx (colon at index 1)
            // If 4 digits: xx:xx (colon at index 2)
            if (trimmed.length == 3 && i == 1) out += ":"
            if (trimmed.length == 4 && i == 2) out += ":"
            out += trimmed[i]
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (trimmed.length < 3) return offset
                if (trimmed.length == 3) {
                    if (offset <= 1) return offset
                    return offset + 1
                }
                if (trimmed.length == 4) {
                    if (offset <= 2) return offset
                    return offset + 1
                }
                return offset
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (trimmed.length < 3) return offset
                if (trimmed.length == 3) {
                    if (offset <= 1) return offset
                    return offset - 1
                }
                if (trimmed.length == 4) {
                    if (offset <= 2) return offset
                    return offset - 1
                }
                return offset
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}
