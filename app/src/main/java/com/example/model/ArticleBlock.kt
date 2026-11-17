package com.example.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface ArticleBlock {
    @Serializable
    data class Paragraph(val text: String) : ArticleBlock

    @Serializable
    data class Heading(val text: String, val level: Int = 2) : ArticleBlock

    @Serializable
    data class Quote(val text: String) : ArticleBlock

    @Serializable
    data class ListItem(val text: String, val orderedIndex: Int? = null) : ArticleBlock

    @Serializable
    data class InlineImage(val url: String, val caption: String? = null) : ArticleBlock
}
