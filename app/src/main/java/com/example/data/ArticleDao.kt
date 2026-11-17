package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY fetchedAt DESC")
    fun observeAllArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY fetchedAt DESC")
    fun observeBookmarkedArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE cleanUrl = :cleanUrl LIMIT 1")
    suspend fun getArticleByUrl(cleanUrl: String): ArticleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArticle(article: ArticleEntity)

    @Query("UPDATE articles SET isBookmarked = :bookmarked WHERE cleanUrl = :cleanUrl")
    suspend fun setBookmarked(cleanUrl: String, bookmarked: Boolean)

    @Query("DELETE FROM articles WHERE cleanUrl = :cleanUrl")
    suspend fun deleteArticle(cleanUrl: String)

    @Query("DELETE FROM articles WHERE isBookmarked = 0")
    suspend fun clearUnbookmarkedHistory()
}
