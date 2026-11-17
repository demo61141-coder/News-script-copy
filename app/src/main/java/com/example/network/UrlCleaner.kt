package com.example.network

import java.net.URI

object UrlCleaner {
    private val URL_REGEX = Regex("""(https?://[^\s<>"'\)\]]+)""", RegexOption.IGNORE_CASE)
    private val BARE_DOMAIN_REGEX = Regex("""^([a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}(/.*)?$""")

    private val TRACKING_PARAM_PREFIXES = listOf("utm_", "fb_action_", "fb_", "ga_", "hsa_")
    private val TRACKING_PARAMS = setOf(
        "fbclid", "gclid", "dclid", "gbraid", "wbraid", "msclkid",
        "mc_cid", "mc_eid", "igshid", "igsh", "si", "mibextid",
        "ref", "ref_src", "ref_url", "source", "spm", "yclid",
        "_hsenc", "_hsmi", "vero_id", "oly_enc_id", "oly_anon_id"
    )

    fun extractFirstUrl(rawText: String?): String? {
        if (rawText.isNullOrBlank()) return null
        val trimmed = rawText.trim()
        val match = URL_REGEX.find(trimmed)
        if (match != null) {
            return match.value.trimEnd('.', ',', ';', ')', ']', '"', '\'')
        }
        val firstToken = trimmed.split(Regex("\\s+")).firstOrNull().orEmpty()
        if (BARE_DOMAIN_REGEX.matches(firstToken)) {
            return "https://$firstToken"
        }
        return null
    }

    fun normalizeAndClean(rawInput: String): String? {
        val extracted = extractFirstUrl(rawInput) ?: return null
        return stripTrackingParams(extracted)
    }

    fun stripTrackingParams(rawUrl: String): String {
        return try {
            val uri = URI(rawUrl.trim())
            val scheme = uri.scheme?.lowercase() ?: "https"
            if (scheme != "http" && scheme != "https") return rawUrl.trim()

            val query = uri.rawQuery
            val cleanQuery = if (!query.isNullOrBlank()) {
                query.split("&")
                    .filter { param ->
                        val key = param.substringBefore("=").lowercase()
                        key.isNotBlank() &&
                            key !in TRACKING_PARAMS &&
                            TRACKING_PARAM_PREFIXES.none { key.startsWith(it) }
                    }
                    .joinToString("&")
                    .takeIf { it.isNotBlank() }
            } else {
                null
            }

            URI(
                scheme,
                uri.rawUserInfo,
                uri.host,
                uri.port,
                uri.rawPath?.takeIf { it.isNotEmpty() } ?: "/",
                cleanQuery,
                null
            ).toString()
        } catch (_: Exception) {
            rawUrl.trim()
        }
    }

    fun extractDomain(url: String): String {
        return try {
            val host = URI(url).host ?: return url
            host.removePrefix("www.").removePrefix("m.")
        } catch (_: Exception) {
            url
        }
    }
}
