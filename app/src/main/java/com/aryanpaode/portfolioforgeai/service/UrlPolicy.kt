package com.aryanpaode.portfolioforgeai.service

import java.net.URI

/** URLs from documents are untrusted. Only public HTTP(S) links are rendered/launched. */
object UrlPolicy {
    fun normalize(value: String): String? {
        val raw = value.trim().trimEnd('.', ',', ';', ')', ']')
        if (raw.isBlank() || raw.any { it.isWhitespace() || it == '\\' || it == '"' || it == '<' || it == '>' || it.code < 32 }) return null
        val candidate = when {
            Regex("(?i)^10\\.\\d{4,9}/.+").matches(raw) -> "https://doi.org/$raw"
            raw.startsWith("http://", true) || raw.startsWith("https://", true) -> raw
            raw.contains(":") -> return null
            raw.contains('.') -> "https://$raw"
            else -> return null
        }
        return runCatching {
            val uri = URI(candidate)
            if (uri.scheme.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank() || uri.userInfo != null) null
            else uri.toASCIIString()
        }.getOrNull()
    }

    fun extract(text: String): List<String> = Regex("(?i)https?://[^\\s<>\"]+|www\\.[^\\s<>\"]+|\\b10\\.\\d{4,9}/[^\\s<>\"]+")
        .findAll(text).mapNotNull { normalize(it.value) }.distinct().toList()
}
