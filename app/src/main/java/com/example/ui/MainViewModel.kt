package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ArticleRepository
import com.example.data.PreferencesRepository
import com.example.model.AppLanguage
import com.example.model.ParsedArticle
import com.example.model.ReaderFontStyle
import com.example.model.ReaderPreferences
import com.example.model.ReaderSurfaceTheme
import com.example.network.UrlCleaner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ReaderUiState {
    data object Idle : ReaderUiState
    data class Loading(val targetUrl: String, val domain: String) : ReaderUiState
    data class Success(val article: ParsedArticle) : ReaderUiState
    data class Error(val targetUrl: String, val isInvalidUrl: Boolean, val detail: String) : ReaderUiState
}

enum class HistoryFilter {
    RECENT,
    SAVED
}

class MainViewModel(
    private val articleRepository: ArticleRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _clipboardUrl = MutableStateFlow<String?>(null)
    val clipboardUrl: StateFlow<String?> = _clipboardUrl.asStateFlow()

    private val _readerState = MutableStateFlow<ReaderUiState>(ReaderUiState.Idle)
    val readerState: StateFlow<ReaderUiState> = _readerState.asStateFlow()

    private val _historyFilter = MutableStateFlow(HistoryFilter.RECENT)
    val historyFilter: StateFlow<HistoryFilter> = _historyFilter.asStateFlow()

    val historyArticles: StateFlow<List<ParsedArticle>> = articleRepository.allArticles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedArticles: StateFlow<List<ParsedArticle>> = articleRepository.bookmarkedArticles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val preferences: StateFlow<ReaderPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReaderPreferences())

    fun onUrlInputChanged(newValue: String) {
        _urlInput.value = newValue
    }

    fun updateClipboardCandidate(rawClipboardText: String?) {
        _clipboardUrl.value = UrlCleaner.extractFirstUrl(rawClipboardText)
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        _historyFilter.value = filter
    }

    fun loadArticle(rawUrl: String, forceRefresh: Boolean = false) {
        val extracted = UrlCleaner.extractFirstUrl(rawUrl)
        if (extracted == null) {
            _readerState.value = ReaderUiState.Error(
                targetUrl = rawUrl,
                isInvalidUrl = true,
                detail = "INVALID_URL"
            )
            return
        }

        val cleaned = UrlCleaner.stripTrackingParams(extracted)
        _urlInput.value = cleaned
        _readerState.value = ReaderUiState.Loading(
            targetUrl = cleaned,
            domain = UrlCleaner.extractDomain(cleaned)
        )

        viewModelScope.launch {
            val result = articleRepository.openOrFetchArticle(cleaned, forceRefresh = forceRefresh)
            result.fold(
                onSuccess = { article ->
                    _readerState.value = ReaderUiState.Success(article)
                },
                onFailure = { throwable ->
                    val invalid = throwable is IllegalArgumentException
                    _readerState.value = ReaderUiState.Error(
                        targetUrl = cleaned,
                        isInvalidUrl = invalid,
                        detail = throwable.message ?: "NETWORK_ERROR"
                    )
                }
            )
        }
    }

    fun openCachedArticle(article: ParsedArticle) {
        _readerState.value = ReaderUiState.Success(article)
    }

    fun toggleBookmark(article: ParsedArticle) {
        val nextValue = !article.isBookmarked
        viewModelScope.launch {
            articleRepository.toggleBookmark(article.cleanUrl, nextValue)
            val current = _readerState.value
            if (current is ReaderUiState.Success && current.article.cleanUrl == article.cleanUrl) {
                _readerState.value = ReaderUiState.Success(current.article.copy(isBookmarked = nextValue))
            }
        }
    }

    fun deleteArticle(cleanUrl: String) {
        viewModelScope.launch {
            articleRepository.deleteArticle(cleanUrl)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            articleRepository.clearHistory()
        }
    }

    fun updateFontSize(sizeSp: Int) {
        viewModelScope.launch { preferencesRepository.updateFontSize(sizeSp) }
    }

    fun updateLineHeight(multiplier: Float) {
        viewModelScope.launch { preferencesRepository.updateLineHeight(multiplier) }
    }

    fun updateSurfaceTheme(theme: ReaderSurfaceTheme) {
        viewModelScope.launch { preferencesRepository.updateSurfaceTheme(theme) }
    }

    fun updateFontStyle(style: ReaderFontStyle) {
        viewModelScope.launch { preferencesRepository.updateFontStyle(style) }
    }

    fun updateShowImages(show: Boolean) {
        viewModelScope.launch { preferencesRepository.updateShowImages(show) }
    }

    fun toggleLanguage() {
        val next = if (preferences.value.language == AppLanguage.BENGALI) {
            AppLanguage.ENGLISH
        } else {
            AppLanguage.BENGALI
        }
        viewModelScope.launch { preferencesRepository.updateLanguage(next) }
    }

    class Factory(
        private val articleRepository: ArticleRepository,
        private val preferencesRepository: PreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(articleRepository, preferencesRepository) as T
        }
    }

    companion object {
        const val SAMPLE_ITTEFAQ_URL =
            "https://www.ittefaq.com.bd/821351?fbclid=IwZXh0bgNhZW0CMTEAcGRvZgNzcnRjBmFwcF9pZA8yNzUyNTQ2OTI1OTgyNzkAAR-jz2sp1vfFavCdiUFbCcyjoIR5cPV4QaW5DMFfDHRAOnFi2o8eDSL7wiX0Aw_aem_Jb3fG43fJ0Uqdu6dHSrQCA"
    }
}
