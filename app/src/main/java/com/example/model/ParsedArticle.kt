package com.example.model

data class ParsedArticle(
    val cleanUrl: String,
    val originalUrl: String,
    val domain: String,
    val siteName: String,
    val title: String,
    val subtitle: String? = null,
    val author: String? = null,
    val publishedAt: String? = null,
    val heroImageUrl: String? = null,
    val blocks: List<ArticleBlock>,
    val wordCount: Int,
    val readingMinutes: Int,
    val isBookmarked: Boolean = false,
    val fetchedAt: Long = System.currentTimeMillis()
) {
    val plainTextBody: String
        get() = blocks.mapNotNull { block ->
            when (block) {
                is ArticleBlock.Paragraph -> block.text
                is ArticleBlock.Heading -> block.text
                is ArticleBlock.Quote -> "\"${block.text}\""
                is ArticleBlock.ListItem -> {
                    if (block.orderedIndex != null) "${block.orderedIndex}. ${block.text}"
                    else "• ${block.text}"
                }
                is ArticleBlock.InlineImage -> null
            }
        }.joinToString(separator = "\n\n")

    val fullCopyText: String
        get() = buildString {
            append(title)
            append("\n")
            val metaParts = listOfNotNull(
                siteName.takeIf { it.isNotBlank() },
                author?.takeIf { it.isNotBlank() },
                publishedAt?.takeIf { it.isNotBlank() }
            )
            if (metaParts.isNotEmpty()) {
                append(metaParts.joinToString(" • "))
                append("\n")
            }
            append("\n")
            append(plainTextBody)
            append("\n\n")
            append(cleanUrl)
        }
}
