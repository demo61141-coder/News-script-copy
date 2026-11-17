package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ArticleBlock
import com.example.model.ParsedArticle
import kotlinx.serialization.json.Json

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val cleanUrl: String,
    val originalUrl: String,
    val domain: String,
    val siteName: String,
    val title: String,
    val subtitle: String?,
    val author: String?,
    val publishedAt: String?,
    val heroImageUrl: String?,
    val blocksJson: String,
    val wordCount: Int,
    val readingMinutes: Int,
    val isBookmarked: Boolean,
    val fetchedAt: Long
) {
    fun toDomain(json: Json = defaultJson): ParsedArticle {
        val decodedBlocks = runCatching {
            json.decodeFromString<List<ArticleBlock>>(blocksJson)
        }.getOrDefault(emptyList())
        return ParsedArticle(
            cleanUrl = cleanUrl,
            originalUrl = originalUrl,
            domain = domain,
            siteName = siteName,
            title = title,
            subtitle = subtitle,
            author = author,
            publishedAt = publishedAt,
            heroImageUrl = heroImageUrl,
            blocks = decodedBlocks,
            wordCount = wordCount,
            readingMinutes = readingMinutes,
            isBookmarked = isBookmarked,
            fetchedAt = fetchedAt
        )
    }

    companion object {
        private val defaultJson = Json { ignoreUnknownKeys = true }

        fun fromDomain(article: ParsedArticle, json: Json = defaultJson): ArticleEntity {
            return ArticleEntity(
                cleanUrl = article.cleanUrl,
                originalUrl = article.originalUrl,
                domain = article.domain,
                siteName = article.siteName,
                title = article.title,
                subtitle = article.subtitle,
                author = article.author,
                publishedAt = article.publishedAt,
                heroImageUrl = article.heroImageUrl,
                blocksJson = json.encodeToString(article.blocks),
                wordCount = article.wordCount,
                readingMinutes = article.readingMinutes,
                isBookmarked = article.isBookmarked,
                fetchedAt = article.fetchedAt
            )
        }
    }
}
