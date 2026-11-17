package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.ArticleRepository
import com.example.data.PreferencesRepository
import com.example.network.UrlCleaner
import com.example.ui.HomeScreen
import com.example.ui.MainViewModel
import com.example.ui.ReaderScreen
import com.example.ui.theme.CleanReadTheme
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object ReaderRoute

class MainActivity : ComponentActivity() {
    private var incomingSharedUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        incomingSharedUrl = extractIncomingUrl(intent)

        val database = AppDatabase.getInstance(applicationContext)
        val articleRepository = ArticleRepository(database.articleDao())
        val preferencesRepository = PreferencesRepository(applicationContext)

        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.Factory(articleRepository, preferencesRepository)
            )
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()
            val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
            val clipboardUrl by viewModel.clipboardUrl.collectAsStateWithLifecycle()
            val historyArticles by viewModel.historyArticles.collectAsStateWithLifecycle()
            val bookmarkedArticles by viewModel.bookmarkedArticles.collectAsStateWithLifecycle()
            val historyFilter by viewModel.historyFilter.collectAsStateWithLifecycle()
            val readerState by viewModel.readerState.collectAsStateWithLifecycle()

            val navController = rememberNavController()

            LaunchedEffect(incomingSharedUrl) {
                val url = incomingSharedUrl
                if (!url.isNullOrBlank()) {
                    incomingSharedUrl = null
                    viewModel.loadArticle(url)
                    navController.navigate(ReaderRoute) {
                        launchSingleTop = true
                    }
                }
            }

            CleanReadTheme {
                NavHost(
                    navController = navController,
                    startDestination = HomeRoute
                ) {
                    composable<HomeRoute> {
                        HomeScreen(
                            urlInput = urlInput,
                            clipboardUrl = clipboardUrl,
                            historyArticles = historyArticles,
                            bookmarkedArticles = bookmarkedArticles,
                            historyFilter = historyFilter,
                            preferences = preferences,
                            onUrlInputChanged = viewModel::onUrlInputChanged,
                            onCheckClipboard = viewModel::updateClipboardCandidate,
                            onReadUrl = { rawUrl ->
                                viewModel.loadArticle(rawUrl)
                                navController.navigate(ReaderRoute) {
                                    launchSingleTop = true
                                }
                            },
                            onOpenCachedArticle = { article ->
                                viewModel.openCachedArticle(article)
                                navController.navigate(ReaderRoute) {
                                    launchSingleTop = true
                                }
                            },
                            onToggleBookmark = viewModel::toggleBookmark,
                            onDeleteArticle = viewModel::deleteArticle,
                            onClearHistory = viewModel::clearHistory,
                            onSelectFilter = viewModel::setHistoryFilter,
                            onToggleLanguage = viewModel::toggleLanguage
                        )
                    }

                    composable<ReaderRoute> {
                        ReaderScreen(
                            readerState = readerState,
                            preferences = preferences,
                            onBack = { navController.popBackStack() },
                            onRefresh = { targetUrl ->
                                viewModel.loadArticle(targetUrl, forceRefresh = true)
                            },
                            onToggleBookmark = viewModel::toggleBookmark,
                            onUpdateFontSize = viewModel::updateFontSize,
                            onUpdateLineHeight = viewModel::updateLineHeight,
                            onUpdateSurfaceTheme = viewModel::updateSurfaceTheme,
                            onUpdateFontStyle = viewModel::updateFontStyle,
                            onUpdateShowImages = viewModel::updateShowImages
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractIncomingUrl(intent)?.let { url ->
            incomingSharedUrl = url
        }
    }

    private fun extractIncomingUrl(intent: Intent?): String? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                UrlCleaner.extractFirstUrl(sharedText)
            }
            Intent.ACTION_VIEW -> {
                UrlCleaner.extractFirstUrl(intent.dataString)
            }
            else -> null
        }
    }
}
