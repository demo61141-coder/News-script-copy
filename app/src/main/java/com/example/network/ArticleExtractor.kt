package com.example.network

import com.example.model.ArticleBlock
import com.example.model.ParsedArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.math.max

class ArticleExtractor(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(18, TimeUnit.SECONDS)
        .readTimeout(22, TimeUnit.SECONDS)
        .build()
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun fetchAndExtract(rawUrl: String): Result<ParsedArticle> = withContext(Dispatchers.IO) {
        val cleanedInput = UrlCleaner.normalizeAndClean(rawUrl)
            ?: return@withContext Result.failure(IllegalArgumentException("INVALID_URL"))

        try {
            val request = Request.Builder()
                .url(cleanedInput)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
                )
                .header(
                    "Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
                )
                .header("Accept-Language", "bn-BD,bn;q=0.95,en-US;q=0.85,en;q=0.75")
                .header("Cache-Control", "no-cache")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP_${response.code}"))
                }
                val bodyBytes = response.body?.bytes()
                    ?: return@withContext Result.failure(IOException("EMPTY_RESPONSE"))
                val finalUrl = UrlCleaner.stripTrackingParams(response.request.url.toString())
                val contentType = response.header("Content-Type").orEmpty()
                val charset = parseCharset(contentType)
                val html = String(bodyBytes, charset)
                val article = parseHtml(html = html, originalUrl = rawUrl, resolvedUrl = finalUrl)
                if (article.blocks.isEmpty() && article.title.isBlank()) {
                    Result.failure(IllegalStateException("NO_ARTICLE_FOUND"))
                } else {
                    Result.success(article)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseHtml(html: String, originalUrl: String, resolvedUrl: String): ParsedArticle {
        val doc = Jsoup.parse(html, resolvedUrl)
        val ldMeta = extractJsonLdMetadata(doc)

        val canonicalUrl = doc.selectFirst("link[rel=canonical]")?.absUrl("href")
            ?.takeIf { it.startsWith("http") }
            ?.let { UrlCleaner.stripTrackingParams(it) }
            ?: UrlCleaner.stripTrackingParams(resolvedUrl)

        val domain = UrlCleaner.extractDomain(canonicalUrl)
        val siteName = firstNonBlank(
            doc.metaContent("og:site_name"),
            doc.metaContent("application-name"),
            ldMeta.publisherName,
            domain
        ) ?: domain

        val rawTitle = firstNonBlank(
            ldMeta.headline,
            doc.metaContent("og:title"),
            doc.metaContent("twitter:title"),
            doc.selectFirst("article h1, main h1, h1.title, h1.headline, h1")?.text(),
            doc.title()
        ).orEmpty()

        val title = cleanHeadline(rawTitle, siteName, domain)

        val subtitle = firstNonBlank(
            doc.selectFirst(".subtitle, .sub-headline, .subhead, .excerpt, .lead, .article-lead")?.text(),
            doc.metaContent("description"),
            doc.metaContent("og:description")
        )?.takeIf { sub ->
            sub.length in 24..280 && !title.contains(sub, ignoreCase = true)
        }

        val author = firstNonBlank(
            ldMeta.authorName,
            doc.metaContent("author"),
            doc.metaContent("article:author"),
            doc.selectFirst("[rel=author], .author-name, .byline, .writer, .reporter, .author")?.text()
        )?.take(100)

        val publishedAt = firstNonBlank(
            formatPublishedDate(ldMeta.datePublished),
            formatPublishedDate(doc.metaContent("article:published_time")),
            formatPublishedDate(doc.metaContent("og:updated_time")),
            doc.selectFirst("time[datetime]")?.attr("datetime")?.let { formatPublishedDate(it) },
            doc.selectFirst(".publish-time, .published-date, .post-date, .date-time, .time, time")?.text()
        )?.take(80)

        val heroImageUrl = resolveUrl(
            canonicalUrl,
            firstNonBlank(
                doc.metaContent("og:image"),
                doc.metaContent("twitter:image"),
                ldMeta.imageUrl
            )
        )

        // Strip ads, scripts, navs, popups, related news boxes, comments before content scoring
        stripNoiseElements(doc)

        val contentRoot = findBestContentContainer(doc)
        val extractedBlocks = extractBlocksFromContainer(
            container = contentRoot,
            baseUrl = canonicalUrl,
            heroImageUrl = heroImageUrl
        ).toMutableList()

        // Fallback to JSON-LD articleBody if DOM yielded very little text
        val domWords = countWords(extractedBlocks)
        if (domWords < 35 && !ldMeta.articleBody.isNullOrBlank()) {
            val ldParagraphs = splitPlainTextIntoParagraphs(ldMeta.articleBody)
            if (ldParagraphs.size > extractedBlocks.count { it is ArticleBlock.Paragraph }) {
                extractedBlocks.clear()
                ldParagraphs.forEach { extractedBlocks.add(ArticleBlock.Paragraph(it)) }
            }
        }

        // Fallback to meta description if still empty (e.g., strict JS-only site)
        if (extractedBlocks.isEmpty() && !subtitle.isNullOrBlank()) {
            extractedBlocks.add(ArticleBlock.Paragraph(subtitle))
        }

        val wordCount = countWords(extractedBlocks)
        val readingMinutes = max(1, (wordCount + 165) / 180)

        return ParsedArticle(
            cleanUrl = canonicalUrl,
            originalUrl = originalUrl,
            domain = domain,
            siteName = siteName,
            title = title.ifBlank { domain },
            subtitle = subtitle?.takeIf { sub ->
                extractedBlocks.none { block ->
                    block is ArticleBlock.Paragraph && block.text.startsWith(sub.take(30))
                }
            },
            author = author?.takeIf { it.isNotBlank() },
            publishedAt = publishedAt?.takeIf { it.isNotBlank() },
            heroImageUrl = heroImageUrl,
            blocks = extractedBlocks,
            wordCount = wordCount,
            readingMinutes = readingMinutes
        )
    }

    private fun stripNoiseElements(doc: Document) {
        doc.select(
            "script, style, noscript, iframe, ins, svg, canvas, form, button, input, select, textarea, " +
                "nav, footer, aside, dialog, amp-ad, amp-embed, amp-sticky-ad, amp-auto-ads"
        ).remove()

        val noiseRegex = Regex(
            "(^|[-_\\s])(ad|ads|advert|advertisement|adsense|adslot|ad-container|ad-box|banner|sponsor|sponsored|" +
                "promo|popup|modal|newsletter|subscribe|subscription|social|share|sharing|related|recommended|" +
                "trending|popular|most-read|more-news|read-more|comment|comments|disqus|sidebar|widget|taboola|" +
                "outbrain|mgid|dfp|gpt-ad|cookie|gdpr|paywall|ticker|breaking-bar|breadcrumb|menu|navbar|topbar|" +
                "bottom-bar|floating|sticky-footer|tags|tag-list|author-bio-box)($|[-_\\s])",
            RegexOption.IGNORE_CASE
        )

        val candidates = doc.body()?.select("*")?.toList().orEmpty()
        for (el in candidates) {
            if (el.parent() == null) continue
            val tag = el.tagName().lowercase()
            if (tag == "body" || tag == "html" || tag == "article" || tag == "main") continue

            val idAndClass = "${el.id()} ${el.className()} ${el.attr("role")} ${el.attr("aria-label")}"
            val hasAdAttr = el.hasAttr("data-ad-slot") || el.hasAttr("data-ad-client") || el.hasAttr("data-google-query-id")
            if (hasAdAttr || noiseRegex.containsMatchIn(idAndClass)) {
                // Guard against accidentally deleting a huge outer wrapper whose class happens to contain a word
                val textLen = el.text().length
                val pCount = el.select("p").size
                if (pCount < 4 || textLen < 700 || hasAdAttr) {
                    el.remove()
                }
            }
        }
    }

    private fun findBestContentContainer(doc: Document): Element {
        // Priority 1: Well-known news & CMS content containers (including Bangladeshi/Bengali news portals)
        val prioritySelectors = listOf(
            "[itemprop=articleBody]",
            ".jw_detail_content_holder",
            ".detail-content",
            ".details-content",
            ".details_content",
            ".story-content",
            ".story-body",
            ".story-element-text",
            ".news-details",
            ".news-content",
            ".article-body",
            ".article-content",
            ".entry-content",
            ".post-content",
            ".content-details",
            "#news-details",
            "article"
        )

        var bestPriorityEl: Element? = null
        var bestPriorityScore = 0.0

        for (selector in prioritySelectors) {
            val matches = doc.select(selector)
            for (el in matches) {
                val score = scoreContainer(el) + 45.0
                if (score > bestPriorityScore && el.text().length >= 160) {
                    bestPriorityScore = score
                    bestPriorityEl = el
                }
            }
        }
        if (bestPriorityEl != null && bestPriorityScore >= 85.0) {
            return bestPriorityEl
        }

        // Priority 2: Full Readability-style heuristic scoring across all candidate containers
        var bestEl: Element = doc.body() ?: doc
        var bestScore = Double.NEGATIVE_INFINITY

        val containers = doc.body()?.select("article, main, section, div") ?: emptyList()
        for (el in containers) {
            val score = scoreContainer(el)
            if (score > bestScore) {
                bestScore = score
                bestEl = el
            }
        }
        return bestEl
    }

    private fun scoreContainer(el: Element): Double {
        val paragraphs = el.select("p")
        if (paragraphs.isEmpty()) return -100.0

        var textLength = 0
        var sentenceMarks = 0
        var goodParagraphs = 0

        for (p in paragraphs) {
            val pText = p.text().trim()
            if (pText.length < 25) continue
            if (isBoilerplateLine(pText)) continue
            goodParagraphs++
            textLength += pText.length
            sentenceMarks += pText.count { it == '।' || it == '.' || it == ',' || it == '،' }
        }

        if (textLength < 100) return -50.0

        val linkTextLength = el.select("a").sumOf { it.text().length }
        val totalTextLength = max(1, el.text().length)
        val linkDensity = linkTextLength.toDouble() / totalTextLength.toDouble()
        if (linkDensity > 0.45) return -60.0

        var score = (goodParagraphs * 14.0) + (textLength / 28.0) + (sentenceMarks * 2.5) - (linkDensity * 90.0)

        val hint = "${el.id()} ${el.className()}".lowercase()
        if (hint.contains("article") || hint.contains("content") || hint.contains("story") ||
            hint.contains("body") || hint.contains("detail") || hint.contains("news") || hint.contains("post")
        ) {
            score += 25.0
        }
        if (hint.contains("comment") || hint.contains("related") || hint.contains("sidebar") ||
            hint.contains("footer") || hint.contains("menu") || hint.contains("nav") || hint.contains("header")
        ) {
            score -= 50.0
        }

        // Slight penalty for giant page-wide wrappers so a tighter article wrapper wins
        val childDivs = el.select("> div").size
        if (childDivs > 18) score -= 18.0

        return score
    }

    private fun extractBlocksFromContainer(
        container: Element,
        baseUrl: String,
        heroImageUrl: String?
    ): List<ArticleBlock> {
        val blocks = mutableListOf<ArticleBlock>()
        val seenTexts = HashSet<String>()
        val seenImages = HashSet<String>()
        if (!heroImageUrl.isNullOrBlank()) {
            seenImages.add(normalizeImageKey(heroImageUrl))
        }

        val relevantNodes = container.select("p, h2, h3, h4, blockquote, ul, ol, figure, img")
        for (node in relevantNodes) {
            // Skip nested elements already processed inside blockquote, list, or figure
            if (node.tagName() == "p" && (node.parents().any { it.tagName() == "blockquote" || it.tagName() == "li" || it.tagName() == "figure" })) {
                continue
            }
            if (node.tagName() == "img" && node.parents().any { it.tagName() == "figure" }) {
                continue
            }

            when (node.tagName().lowercase()) {
                "h2", "h3", "h4" -> {
                    val text = cleanText(node.text())
                    if (text.length in 4..180 && !isBoilerplateLine(text) && seenTexts.add(text)) {
                        val level = node.tagName().removePrefix("h").toIntOrNull() ?: 2
                        blocks.add(ArticleBlock.Heading(text = text, level = level))
                    }
                }

                "p" -> {
                    // Check if paragraph is mostly a link ("Read more: ...")
                    val text = cleanText(node.text())
                    if (text.length < 18) continue
                    if (isBoilerplateLine(text)) continue
                    val linkLen = node.select("a").sumOf { it.text().length }
                    if (text.length < 130 && linkLen.toDouble() / max(1, text.length) > 0.80) continue
                    if (seenTexts.add(text)) {
                        blocks.add(ArticleBlock.Paragraph(text))
                    }
                }

                "blockquote" -> {
                    val text = cleanText(node.text())
                    if (text.length >= 20 && !isBoilerplateLine(text) && seenTexts.add(text)) {
                        blocks.add(ArticleBlock.Quote(text))
                    }
                }

                "ul", "ol" -> {
                    val isOrdered = node.tagName().equals("ol", ignoreCase = true)
                    val items = node.select("> li")
                    val linkItems = items.count { li ->
                        val t = li.text().trim()
                        val a = li.select("a").text().trim()
                        t.isNotEmpty() && a.length.toDouble() / t.length > 0.85
                    }
                    // Skip navigation/related-link lists
                    if (items.isNotEmpty() && linkItems.toDouble() / items.size > 0.65) continue

                    items.forEachIndexed { index, li ->
                        val text = cleanText(li.text())
                        if (text.length >= 12 && !isBoilerplateLine(text) && seenTexts.add(text)) {
                            blocks.add(
                                ArticleBlock.ListItem(
                                    text = text,
                                    orderedIndex = if (isOrdered) index + 1 else null
                                )
                            )
                        }
                    }
                }

                "figure" -> {
                    val img = node.selectFirst("img")
                    val imgUrl = extractImageUrl(img, baseUrl)
                    val caption = cleanText(node.selectFirst("figcaption")?.text().orEmpty())
                        .takeIf { it.isNotBlank() }
                    if (imgUrl != null && isContentImage(img, imgUrl) && seenImages.add(normalizeImageKey(imgUrl))) {
                        blocks.add(ArticleBlock.InlineImage(url = imgUrl, caption = caption))
                    }
                }

                "img" -> {
                    val imgUrl = extractImageUrl(node, baseUrl)
                    if (imgUrl != null && isContentImage(node, imgUrl) && seenImages.add(normalizeImageKey(imgUrl))) {
                        val alt = cleanText(node.attr("alt")).takeIf { it.length in 10..180 }
                        blocks.add(ArticleBlock.InlineImage(url = imgUrl, caption = alt))
                    }
                }
            }
        }

        // If <p> tags weren't used (some older sites use <br><br> inside a div), split container text cleanly
        if (blocks.count { it is ArticleBlock.Paragraph } == 0) {
            val rawHtmlWithBreaks = container.html()
                .replace(Regex("(?i)<br\\s*/?>"), "\n")
                .replace(Regex("(?i)</(div|p|li)>"), "\n")
            val plain = Jsoup.parse(rawHtmlWithBreaks).text()
            splitPlainTextIntoParagraphs(plain).forEach { para ->
                if (!isBoilerplateLine(para) && seenTexts.add(para)) {
                    blocks.add(ArticleBlock.Paragraph(para))
                }
            }
        }

        return blocks
    }

    private fun extractImageUrl(img: Element?, baseUrl: String): String? {
        if (img == null) return null
        val candidate = firstNonBlank(
            img.attr("data-src"),
            img.attr("data-original"),
            img.attr("data-lazy-src"),
            img.attr("src")
        ) ?: return null
        if (candidate.startsWith("data:", ignoreCase = true)) return null
        return resolveUrl(baseUrl, candidate)
    }

    private fun isContentImage(img: Element?, url: String): Boolean {
        val lower = url.lowercase()
        if (lower.endsWith(".svg") || lower.contains("logo") || lower.contains("icon") ||
            lower.contains("avatar") || lower.contains("pixel") || lower.contains("doubleclick") ||
            lower.contains("googlesyndication") || lower.contains("ads/") || lower.contains("spinner") ||
            lower.contains("placeholder")
        ) {
            return false
        }
        val width = img?.attr("width")?.toIntOrNull()
        val height = img?.attr("height")?.toIntOrNull()
        if (width != null && width < 140) return false
        if (height != null && height < 90) return false
        return true
    }

    private fun normalizeImageKey(url: String): String {
        return url.substringBefore("?").substringAfterLast("/").lowercase()
    }

    private fun isBoilerplateLine(text: String): Boolean {
        val lower = text.lowercase().trim()
        val exactOrPrefixBoilerplate = listOf(
            "আরও পড়ুন", "আরও পড়ুন", "বিস্তারিত", "বিজ্ঞাপন", "সর্বশেষ খবর",
            "আমাদের সঙ্গে থাকুন", "ফলো করুন", "শেয়ার করুন", "শেয়ার করুন",
            "মন্তব্য করুন", "গুগল নিউজে", "হোয়াটসঅ্যাপ", "টেলিগ্রাম",
            "advertisement", "sponsored", "read more", "also read", "related news",
            "follow us", "subscribe", "sign up", "all rights reserved", "copyright ©",
            "share this", "leave a comment", "most popular", "trending now"
        )
        if (exactOrPrefixBoilerplate.any { lower == it || (lower.length < 95 && lower.startsWith(it)) }) {
            return true
        }
        if (lower.contains("google news") && lower.length < 110) return true
        if (lower.contains("গুগল নিউজ") && lower.length < 110) return true
        return false
    }

    private data class JsonLdMeta(
        val headline: String? = null,
        val authorName: String? = null,
        val datePublished: String? = null,
        val publisherName: String? = null,
        val imageUrl: String? = null,
        val articleBody: String? = null
    )

    private fun extractJsonLdMetadata(doc: Document): JsonLdMeta {
        var headline: String? = null
        var authorName: String? = null
        var datePublished: String? = null
        var publisherName: String? = null
        var imageUrl: String? = null
        var articleBody: String? = null

        val scripts = doc.select("script[type=application/ld+json]")
        for (script in scripts) {
            val raw = script.data().trim()
            if (raw.isBlank()) continue
            try {
                val root = json.parseToJsonElement(raw)
                val objects = flattenJsonLdObjects(root)
                for (obj in objects) {
                    val type = obj["@type"]?.toString().orEmpty()
                    if (type.contains("Article", ignoreCase = true) ||
                        obj.containsKey("articleBody") ||
                        obj.containsKey("headline")
                    ) {
                        headline = headline ?: obj.stringVal("headline")
                        datePublished = datePublished ?: obj.stringVal("datePublished")
                        articleBody = articleBody ?: obj.stringVal("articleBody")
                        authorName = authorName ?: extractNameFromJsonField(obj["author"])
                        publisherName = publisherName ?: extractNameFromJsonField(obj["publisher"])
                        imageUrl = imageUrl ?: extractImageFromJsonField(obj["image"])
                    }
                }
            } catch (_: Exception) {
                // Ignore malformed JSON-LD blocks
            }
        }
        return JsonLdMeta(headline, authorName, datePublished, publisherName, imageUrl, articleBody)
    }

    private fun flattenJsonLdObjects(element: JsonElement): List<JsonObject> {
        val result = mutableListOf<JsonObject>()
        when (element) {
            is JsonObject -> {
                result.add(element)
                element["@graph"]?.let { result.addAll(flattenJsonLdObjects(it)) }
            }
            is JsonArray -> element.forEach { result.addAll(flattenJsonLdObjects(it)) }
            else -> {}
        }
        return result
    }

    private fun JsonObject.stringVal(key: String): String? {
        val el = this[key] ?: return null
        return runCatching { el.jsonPrimitive.contentOrNull }.getOrNull()
            ?.let { Parser.unescapeEntities(it, false) }
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun extractNameFromJsonField(el: JsonElement?): String? {
        return when (el) {
            is JsonObject -> el.stringVal("name")
            is JsonArray -> el.firstOrNull()?.let { extractNameFromJsonField(it) }
            else -> runCatching { el?.jsonPrimitive?.contentOrNull }.getOrNull()
        }?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun extractImageFromJsonField(el: JsonElement?): String? {
        return when (el) {
            is JsonObject -> el.stringVal("url")
            is JsonArray -> el.firstOrNull()?.let { extractImageFromJsonField(it) }
            else -> runCatching { el?.jsonPrimitive?.contentOrNull }.getOrNull()
        }?.trim()?.takeIf { it.startsWith("http") }
    }

    private fun Document.metaContent(key: String): String? {
        return selectFirst("meta[property='$key'], meta[name='$key']")
            ?.attr("content")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun cleanHeadline(raw: String, siteName: String, domain: String): String {
        var cleaned = cleanText(raw)
        val separators = listOf(" | ", " - ", " :: ", " — ", " / ")
        for (sep in separators) {
            if (cleaned.contains(sep)) {
                val tail = cleaned.substringAfterLast(sep).trim()
                if (tail.equals(siteName, ignoreCase = true) ||
                    tail.equals(domain, ignoreCase = true) ||
                    tail.length <= 28
                ) {
                    val head = cleaned.substringBeforeLast(sep).trim()
                    if (head.length >= 12) {
                        cleaned = head
                        break
                    }
                }
            }
        }
        return cleaned
    }

    private fun formatPublishedDate(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        // Convert ISO-8601 like 2026-11-16T10:30:00+06:00 -> 2026-11-16 • 10:30
        val isoMatch = Regex("""^(\d{4}-\d{2}-\d{2})[T\s](\d{2}:\d{2})""").find(trimmed)
        if (isoMatch != null) {
            return "${isoMatch.groupValues[1]} • ${isoMatch.groupValues[2]}"
        }
        return cleanText(trimmed)
    }

    private fun splitPlainTextIntoParagraphs(text: String): List<String> {
        return text.split(Regex("""\n{2,}|\r\n\r\n"""))
            .map { cleanText(it) }
            .filter { it.length >= 25 }
    }

    private fun cleanText(raw: String): String {
        return Parser.unescapeEntities(raw, false)
            .replace(Regex("[\\u00A0\\s]+"), " ")
            .trim()
    }

    private fun countWords(blocks: List<ArticleBlock>): Int {
        return blocks.sumOf { block ->
            val text = when (block) {
                is ArticleBlock.Paragraph -> block.text
                is ArticleBlock.Heading -> block.text
                is ArticleBlock.Quote -> block.text
                is ArticleBlock.ListItem -> block.text
                is ArticleBlock.InlineImage -> ""
            }
            if (text.isBlank()) 0 else text.split(Regex("\\s+")).count { it.isNotBlank() }
        }
    }

    private fun resolveUrl(baseUrl: String, candidate: String?): String? {
        if (candidate.isNullOrBlank()) return null
        return try {
            URI(baseUrl).resolve(candidate.trim()).toString()
        } catch (_: Exception) {
            candidate.takeIf { it.startsWith("http") }
        }
    }

    private fun parseCharset(contentType: String): java.nio.charset.Charset {
        val match = Regex("""charset=([^\s;]+)""", RegexOption.IGNORE_CASE).find(contentType)
        val name = match?.groupValues?.getOrNull(1)?.trim('"', '\'')
        return runCatching { if (name != null) charset(name) else Charsets.UTF_8 }
            .getOrDefault(Charsets.UTF_8)
    }

    private fun firstNonBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim()
    }
}
