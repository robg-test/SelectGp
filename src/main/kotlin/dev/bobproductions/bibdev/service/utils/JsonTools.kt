package dev.bobproductions.bibdev.service.utils

import com.fasterxml.jackson.core.io.JsonStringEncoder

class JsonTools {
    fun jsonEscape(value: String): String {
        return String(
            JsonStringEncoder.getInstance()
                .quoteAsString(value)
        )
    }
}