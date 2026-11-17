package com.example.data

import com.example.model.ParsedArticle
import com.example.network.ArticleExtractor
import com.example.network.UrlCleaner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ArticleRepository(
    private val articleDao: ArticleDao,
    private val extractor: ArticleExtractor = ArticleExtractor()
) {
    val allArticles: Flow<List<ParsedArticle>> = articleDao.observeAllArticles()
        .map { list -> list.map { it.toDomain() } }

    val bookmarkedArticles: Flow<List<ParsedArticle>> = articleDao.observeBookmarkedArticles()
        .map { list -> list.map { it.toDomain() } }

    suspend fun openOrFetchArticle(rawUrl: String, forceRefresh: Boolean = false): Result<ParsedArticle> {
        val cleaned = UrlCleaner.normalizeAndClean(rawUrl)
            ?: return Result.failure(IllegalArgumentException("INVALID_URL"))

        val cached = articleDao.getArticleByUrl(cleaned)
        if (cached != null && !forceRefresh) {
            // Update timestamp so it floats to top of recent history
            val refreshed = cached.copy(fetchedAt = System.currentTimeMillis())
            articleDao.upsertArticle(refreshed)
            return Result.success(refreshed.toDomain())
        }

        val fetchedResult = extractor.fetchAndExtract(rawUrl)
        return fetchedResult.map { article ->
            val existingBookmark = cached?.isBookmarked ?: articleDao.getArticleByUrl(article.cleanUrl)?.isBookmarked ?: false
            val finalArticle = article.copy(isBookmarked = existingBookmark)
            articleDao.upsertArticle(ArticleEntity.fromDomain(finalArticle))
            finalArticle
        }.recoverCatching { error ->
            // If refresh failed offline but we have cache, return cached version
            if (cached != null) cached.toDomain() else throw error
        }
    }

    suspend fun getCachedArticle(cleanUrl: String): ParsedArticle? {
        return articleDao.getArticleByUrl(cleanUrl)?.toDomain()
    }

    suspend fun toggleBookmark(cleanUrl: String, isBookmarked: Boolean) {
        articleDao.setBookmarked(cleanUrl, isBookmarked)
    }

    suspend fun deleteArticle(cleanUrl: String) {
        articleDao.deleteArticle(cleanUrl)
    }

    suspend fun clearHistory() {
        articleDao.clearUnbookmarkedHistory()
    }
}
