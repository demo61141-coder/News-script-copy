package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppLanguage
import com.example.model.ParsedArticle
import com.example.model.ReaderPreferences
import com.example.ui.theme.DisplayLatinSerifFamily
import com.example.ui.theme.EditorialSerifFamily
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    urlInput: String,
    clipboardUrl: String?,
    historyArticles: List<ParsedArticle>,
    bookmarkedArticles: List<ParsedArticle>,
    historyFilter: HistoryFilter,
    preferences: ReaderPreferences,
    onUrlInputChanged: (String) -> Unit,
    onCheckClipboard: (String?) -> Unit,
    onReadUrl: (String) -> Unit,
    onOpenCachedArticle: (ParsedArticle) -> Unit,
    onToggleBookmark: (ParsedArticle) -> Unit,
    onDeleteArticle: (String) -> Unit,
    onClearHistory: () -> Unit,
    onSelectFilter: (HistoryFilter) -> Unit,
    onToggleLanguage: () -> Unit
) {
    val strings = remember(preferences.language) { stringsFor(preferences.language) }
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val clipText = clipboardManager.getText()?.text
        onCheckClipboard(clipText)
    }

    val displayedArticles = if (historyFilter == HistoryFilter.RECENT) {
        historyArticles
    } else {
        bookmarkedArticles
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideLayout = maxWidth >= 840.dp

            if (isWideLayout) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.46f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MastheadHeader(
                            strings = strings,
                            language = preferences.language,
                            onToggleLanguage = onToggleLanguage
                        )
                        HeroEditorialBanner(strings = strings)
                        UrlInputCard(
                            urlInput = urlInput,
                            clipboardUrl = clipboardUrl,
                            strings = strings,
                            onUrlInputChanged = onUrlInputChanged,
                            onPasteFromClipboard = {
                                val text = clipboardManager.getText()?.text.orEmpty()
                                if (text.isNotBlank()) {
                                    onUrlInputChanged(text)
                                }
                            },
                            onSubmitUrl = { url ->
                                focusManager.clearFocus()
                                onReadUrl(url)
                            }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.54f)
                            .fillMaxHeight()
                    ) {
                        ArticleListSection(
                            articles = displayedArticles,
                            historyFilter = historyFilter,
                            strings = strings,
                            onSelectFilter = onSelectFilter,
                            onClearHistory = onClearHistory,
                            onOpenArticle = onOpenCachedArticle,
                            onCopyArticle = { article ->
                                clipboardManager.setText(AnnotatedString(article.fullCopyText))
                                scope.launch {
                                    snackbarHostState.showSnackbar(strings.copiedFeedback)
                                }
                            },
                            onToggleBookmark = onToggleBookmark,
                            onDeleteArticle = onDeleteArticle,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp)
                        .align(Alignment.TopCenter),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        MastheadHeader(
                            strings = strings,
                            language = preferences.language,
                            onToggleLanguage = onToggleLanguage
                        )
                    }

                    item {
                        HeroEditorialBanner(strings = strings)
                    }

                    item {
                        UrlInputCard(
                            urlInput = urlInput,
                            clipboardUrl = clipboardUrl,
                            strings = strings,
                            onUrlInputChanged = onUrlInputChanged,
                            onPasteFromClipboard = {
                                val text = clipboardManager.getText()?.text.orEmpty()
                                if (text.isNotBlank()) {
                                    onUrlInputChanged(text)
                                }
                            },
                            onSubmitUrl = { url ->
                                focusManager.clearFocus()
                                onReadUrl(url)
                            }
                        )
                    }

                    item {
                        HistoryHeaderRow(
                            historyFilter = historyFilter,
                            hasHistory = historyArticles.isNotEmpty(),
                            strings = strings,
                            onSelectFilter = onSelectFilter,
                            onClearHistory = onClearHistory
                        )
                    }

                    if (displayedArticles.isEmpty()) {
                        item {
                            EmptyArticlesCard(
                                isSavedTab = historyFilter == HistoryFilter.SAVED,
                                strings = strings
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = displayedArticles,
                            key = { _, item -> item.cleanUrl }
                        ) { index, article ->
                            ArticleHistoryCard(
                                index = index,
                                article = article,
                                strings = strings,
                                onOpen = { onOpenCachedArticle(article) },
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(article.fullCopyText))
                                    scope.launch {
                                        snackbarHostState.showSnackbar(strings.copiedFeedback)
                                    }
                                },
                                onToggleBookmark = { onToggleBookmark(article) },
                                onDelete = { onDeleteArticle(article.cleanUrl) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MastheadHeader(
    strings: AppStrings,
    language: AppLanguage,
    onToggleLanguage: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column {
                Text(
                    text = strings.appTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = DisplayLatinSerifFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = strings.tagline,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            onClick = onToggleLanguage,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("language_toggle_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = "Switch language",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (language == AppLanguage.BENGALI) "বাংলা • EN" else "EN • বাংলা",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun HeroEditorialBanner(strings: AppStrings) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_editorial_hero),
                contentDescription = strings.heroTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xEA112E3C),
                                Color(0xCC153E50),
                                Color(0x55153E50)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0x33FFFFFF)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFD5C6),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = strings.adFreeBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = strings.heroTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = EditorialSerifFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = strings.heroSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE3EDF2),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun UrlInputCard(
    urlInput: String,
    clipboardUrl: String?,
    strings: AppStrings,
    onUrlInputChanged: (String) -> Unit,
    onPasteFromClipboard: () -> Unit,
    onSubmitUrl: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = onUrlInputChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("url_input"),
                placeholder = {
                    Text(
                        text = strings.urlHint,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (urlInput.isNotBlank()) {
                        IconButton(
                            onClick = { onUrlInputChanged("") },
                            modifier = Modifier.testTag("clear_url_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = strings.clearLabel
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onPasteFromClipboard,
                            modifier = Modifier.testTag("paste_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentPaste,
                                contentDescription = strings.pasteLabel,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (urlInput.isNotBlank()) onSubmitUrl(urlInput)
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPasteFromClipboard,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("paste_action_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.pasteLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = { onSubmitUrl(urlInput) },
                    enabled = urlInput.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("read_article_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.readAdFree,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            AnimatedVisibility(visible = !clipboardUrl.isNullOrBlank() && clipboardUrl != urlInput.trim()) {
                AssistChip(
                    onClick = { clipboardUrl?.let(onSubmitUrl) },
                    label = {
                        Text(
                            text = "${strings.pasteAndRead}: ${clipboardUrl.orEmpty()}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("paste_read_chip")
                )
            }

            if (urlInput.isBlank()) {
                AssistChip(
                    onClick = { onSubmitUrl(MainViewModel.SAMPLE_ITTEFAQ_URL) },
                    label = {
                        Text(
                            text = strings.tryExampleLink,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Newspaper,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.testTag("sample_link_chip")
                )
            }
        }
    }
}

@Composable
private fun ArticleListSection(
    articles: List<ParsedArticle>,
    historyFilter: HistoryFilter,
    strings: AppStrings,
    onSelectFilter: (HistoryFilter) -> Unit,
    onClearHistory: () -> Unit,
    onOpenArticle: (ParsedArticle) -> Unit,
    onCopyArticle: (ParsedArticle) -> Unit,
    onToggleBookmark: (ParsedArticle) -> Unit,
    onDeleteArticle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HistoryHeaderRow(
            historyFilter = historyFilter,
            hasHistory = articles.isNotEmpty(),
            strings = strings,
            onSelectFilter = onSelectFilter,
            onClearHistory = onClearHistory
        )

        if (articles.isEmpty()) {
            EmptyArticlesCard(
                isSavedTab = historyFilter == HistoryFilter.SAVED,
                strings = strings
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    items = articles,
                    key = { _, item -> item.cleanUrl }
                ) { index, article ->
                    ArticleHistoryCard(
                        index = index,
                        article = article,
                        strings = strings,
                        onOpen = { onOpenArticle(article) },
                        onCopy = { onCopyArticle(article) },
                        onToggleBookmark = { onToggleBookmark(article) },
                        onDelete = { onDeleteArticle(article.cleanUrl) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryHeaderRow(
    historyFilter: HistoryFilter,
    hasHistory: Boolean,
    strings: AppStrings,
    onSelectFilter: (HistoryFilter) -> Unit,
    onClearHistory: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = historyFilter == HistoryFilter.RECENT,
                onClick = { onSelectFilter(HistoryFilter.RECENT) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                modifier = Modifier.testTag("tab_recent")
            ) {
                Text(strings.recentTab, style = MaterialTheme.typography.labelLarge)
            }
            SegmentedButton(
                selected = historyFilter == HistoryFilter.SAVED,
                onClick = { onSelectFilter(HistoryFilter.SAVED) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                modifier = Modifier.testTag("tab_saved")
            ) {
                Text(strings.savedTab, style = MaterialTheme.typography.labelLarge)
            }
        }

        if (hasHistory && historyFilter == HistoryFilter.RECENT) {
            TextButton(
                onClick = onClearHistory,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("clear_history_button")
            ) {
                Text(
                    text = strings.clearHistory,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyArticlesCard(
    isSavedTab: Boolean,
    strings: AppStrings
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSavedTab) Icons.Filled.BookmarkBorder else Icons.Filled.Newspaper,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Text(
                text = strings.emptyTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isSavedTab) strings.emptySavedSubtitle else strings.emptySubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ArticleHistoryCard(
    index: Int,
    article: ParsedArticle,
    strings: AppStrings,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onToggleBookmark: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .testTag("article_card_$index")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = article.siteName.ifBlank { article.domain },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${article.readingMinutes} ${strings.minReadSuffix}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = EditorialSerifFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val snippet = article.plainTextBody.lineSequence().firstOrNull().orEmpty()
            if (snippet.isNotBlank()) {
                Text(
                    text = snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("copy_card_button_$index")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = strings.copyFullArticle,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("bookmark_card_button_$index")
                ) {
                    Icon(
                        imageVector = if (article.isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = strings.savedTab,
                        tint = if (article.isBookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_card_button_$index")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = strings.clearLabel,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
