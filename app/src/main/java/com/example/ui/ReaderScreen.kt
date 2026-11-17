package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ArticleBlock
import com.example.model.ParsedArticle
import com.example.model.ReaderFontStyle
import com.example.model.ReaderPreferences
import com.example.model.ReaderSurfaceTheme
import com.example.ui.theme.EditorialSansFamily
import com.example.ui.theme.EditorialSerifFamily
import com.example.ui.theme.ReaderPalette
import com.example.ui.theme.toPalette
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    readerState: ReaderUiState,
    preferences: ReaderPreferences,
    onBack: () -> Unit,
    onRefresh: (String) -> Unit,
    onToggleBookmark: (ParsedArticle) -> Unit,
    onUpdateFontSize: (Int) -> Unit,
    onUpdateLineHeight: (Float) -> Unit,
    onUpdateSurfaceTheme: (ReaderSurfaceTheme) -> Unit,
    onUpdateFontStyle: (ReaderFontStyle) -> Unit,
    onUpdateShowImages: (Boolean) -> Unit
) {
    val strings = remember(preferences.language) { stringsFor(preferences.language) }
    val palette = remember(preferences.surfaceTheme) { preferences.surfaceTheme.toPalette() }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCopySheet by remember { mutableStateOf(false) }
    var showAppearanceSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scrollProgress by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            if (total <= 1) 0f
            else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                ((lastVisible + 1).toFloat() / total.toFloat()).coerceIn(0f, 1f)
            }
        }
    }

    fun copyToClipboard(text: String) {
        clipboardManager.setText(AnnotatedString(text))
        scope.launch {
            snackbarHostState.showSnackbar(strings.copiedFeedback)
        }
    }

    fun shareText(text: String, subject: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, strings.shareText))
    }

    fun openExternalBrowser(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = palette.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("reader_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = palette.textPrimary
                            )
                        }

                        val headerLabel = when (readerState) {
                            is ReaderUiState.Success -> readerState.article.siteName
                            is ReaderUiState.Loading -> readerState.domain
                            is ReaderUiState.Error -> readerState.targetUrl
                            ReaderUiState.Idle -> ""
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = headerLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = strings.adFreeBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.accent
                                )
                            }
                        }
                    }

                    if (readerState is ReaderUiState.Success) {
                        val article = readerState.article
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onRefresh(article.cleanUrl) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("reader_refresh_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = strings.retry,
                                    tint = palette.textSecondary
                                )
                            }
                            IconButton(
                                onClick = {
                                    onToggleBookmark(article)
                                    scope.launch {
                                        val msg = if (!article.isBookmarked) strings.bookmarkAdded else strings.bookmarkRemoved
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("reader_bookmark_button")
                            ) {
                                Icon(
                                    imageVector = if (article.isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    contentDescription = strings.savedTab,
                                    tint = if (article.isBookmarked) palette.accent else palette.textSecondary
                                )
                            }
                            IconButton(
                                onClick = { showAppearanceSheet = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("reader_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FormatSize,
                                    contentDescription = strings.readerSettingsTitle,
                                    tint = palette.textPrimary
                                )
                            }
                        }
                    }
                }

                if (readerState is ReaderUiState.Success) {
                    LinearProgressIndicator(
                        progress = { scrollProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = palette.accent,
                        trackColor = palette.divider
                    )
                } else {
                    HorizontalDivider(color = palette.divider, thickness = 1.dp)
                }
            }
        },
        bottomBar = {
            if (readerState is ReaderUiState.Success) {
                val article = readerState.article
                ReaderBottomActionBar(
                    palette = palette,
                    strings = strings,
                    onCopyAll = { copyToClipboard(article.fullCopyText) },
                    onOpenCopyMenu = { showCopySheet = true },
                    onOpenAppearance = { showAppearanceSheet = true },
                    onShare = { shareText(article.fullCopyText, article.title) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (readerState) {
                ReaderUiState.Idle -> Unit

                is ReaderUiState.Loading -> {
                    ReaderLoadingView(
                        domain = readerState.domain,
                        palette = palette,
                        strings = strings
                    )
                }

                is ReaderUiState.Error -> {
                    ReaderErrorView(
                        targetUrl = readerState.targetUrl,
                        isInvalidUrl = readerState.isInvalidUrl,
                        palette = palette,
                        strings = strings,
                        onRetry = { onRefresh(readerState.targetUrl) },
                        onOpenBrowser = { openExternalBrowser(readerState.targetUrl) }
                    )
                }

                is ReaderUiState.Success -> {
                    val article = readerState.article
                    val readerFont = if (preferences.fontStyle == ReaderFontStyle.SERIF) {
                        EditorialSerifFamily
                    } else {
                        EditorialSansFamily
                    }
                    val bodyFontSize = preferences.fontSizeSp.sp
                    val bodyLineHeight = (preferences.fontSizeSp * preferences.lineHeightMultiplier).sp

                    SelectionContainer {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 720.dp)
                                .align(Alignment.TopCenter),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = palette.accent.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = article.domain,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = palette.accent,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Text(
                                            text = "${article.readingMinutes} ${strings.minReadSuffix}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.textSecondary
                                        )
                                    }

                                    Text(
                                        text = article.title,
                                        style = MaterialTheme.typography.displaySmall.copy(
                                            fontFamily = readerFont,
                                            fontSize = (preferences.fontSizeSp + 8).sp,
                                            lineHeight = ((preferences.fontSizeSp + 8) * 1.32f).sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = palette.textPrimary
                                    )

                                    if (!article.subtitle.isNullOrBlank()) {
                                        Text(
                                            text = article.subtitle,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = readerFont,
                                                fontSize = (preferences.fontSizeSp + 1).sp,
                                                lineHeight = ((preferences.fontSizeSp + 1) * 1.45f).sp
                                            ),
                                            color = palette.textSecondary
                                        )
                                    }

                                    val metaItems = listOfNotNull(
                                        article.author?.takeIf { it.isNotBlank() },
                                        article.publishedAt?.takeIf { it.isNotBlank() }
                                    )
                                    if (metaItems.isNotEmpty()) {
                                        Text(
                                            text = metaItems.joinToString("  •  "),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.textSecondary
                                        )
                                    }

                                    Text(
                                        text = strings.selectToCopyTip,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary.copy(alpha = 0.85f)
                                    )

                                    HorizontalDivider(color = palette.divider, thickness = 1.dp)
                                }
                            }

                            if (preferences.showImages && !article.heroImageUrl.isNullOrBlank()) {
                                item {
                                    AsyncImage(
                                        model = article.heroImageUrl,
                                        contentDescription = article.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(220.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(palette.surface)
                                    )
                                }
                            }

                            items(article.blocks) { block ->
                                when (block) {
                                    is ArticleBlock.Heading -> {
                                        Text(
                                            text = block.text,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontFamily = readerFont,
                                                fontSize = (preferences.fontSizeSp + 3).sp,
                                                lineHeight = ((preferences.fontSizeSp + 3) * 1.38f).sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = palette.textPrimary,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }

                                    is ArticleBlock.Paragraph -> {
                                        Text(
                                            text = block.text,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontFamily = readerFont,
                                                fontSize = bodyFontSize,
                                                lineHeight = bodyLineHeight
                                            ),
                                            color = palette.textPrimary
                                        )
                                    }

                                    is ArticleBlock.Quote -> {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = palette.quoteBackground,
                                            border = BorderStroke(1.dp, palette.divider),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(4.dp)
                                                        .height(48.dp)
                                                        .background(palette.accent, RoundedCornerShape(2.dp))
                                                )
                                                Text(
                                                    text = block.text,
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontFamily = readerFont,
                                                        fontSize = bodyFontSize,
                                                        lineHeight = bodyLineHeight,
                                                        fontStyle = FontStyle.Italic
                                                    ),
                                                    color = palette.textPrimary
                                                )
                                            }
                                        }
                                    }

                                    is ArticleBlock.ListItem -> {
                                        val bullet = block.orderedIndex?.let { "$it." } ?: "•"
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = bullet,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontFamily = readerFont,
                                                    fontSize = bodyFontSize,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = palette.accent
                                            )
                                            Text(
                                                text = block.text,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontFamily = readerFont,
                                                    fontSize = bodyFontSize,
                                                    lineHeight = bodyLineHeight
                                                ),
                                                color = palette.textPrimary,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    is ArticleBlock.InlineImage -> {
                                        if (preferences.showImages) {
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                AsyncImage(
                                                    model = block.url,
                                                    contentDescription = block.caption ?: article.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(200.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(palette.surface)
                                                )
                                                if (!block.caption.isNullOrBlank()) {
                                                    Text(
                                                        text = block.caption,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = palette.textSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = palette.surface,
                                    border = BorderStroke(1.dp, palette.divider),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = strings.cleanLinkLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.textSecondary
                                        )
                                        Text(
                                            text = article.cleanUrl,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.textPrimary
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { copyToClipboard(article.cleanUrl) },
                                                border = BorderStroke(1.dp, palette.divider)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Link,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(strings.copyCleanLink, style = MaterialTheme.typography.labelMedium)
                                            }
                                            OutlinedButton(
                                                onClick = { openExternalBrowser(article.cleanUrl) },
                                                border = BorderStroke(1.dp, palette.divider)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.OpenInBrowser,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(strings.openInBrowser, style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showCopySheet) {
                        ModalBottomSheet(
                            onDismissRequest = { showCopySheet = false },
                            containerColor = palette.surface,
                            contentColor = palette.textPrimary
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = 28.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = strings.copyOptions,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = palette.textPrimary
                                )
                                CopyOptionRow(
                                    icon = Icons.Filled.ContentCopy,
                                    title = strings.copyFullArticle,
                                    subtitle = strings.copyFullArticleDesc,
                                    palette = palette,
                                    testTag = "copy_full_article_button",
                                    onClick = {
                                        copyToClipboard(article.fullCopyText)
                                        showCopySheet = false
                                    }
                                )
                                CopyOptionRow(
                                    icon = Icons.Filled.ContentCopy,
                                    title = strings.copyBodyOnly,
                                    subtitle = strings.copyBodyOnlyDesc,
                                    palette = palette,
                                    testTag = "copy_body_only_button",
                                    onClick = {
                                        copyToClipboard(article.plainTextBody)
                                        showCopySheet = false
                                    }
                                )
                                CopyOptionRow(
                                    icon = Icons.Filled.ContentCopy,
                                    title = strings.copyHeadline,
                                    subtitle = article.title,
                                    palette = palette,
                                    testTag = "copy_headline_button",
                                    onClick = {
                                        copyToClipboard(article.title)
                                        showCopySheet = false
                                    }
                                )
                                CopyOptionRow(
                                    icon = Icons.Filled.Link,
                                    title = strings.copyCleanLink,
                                    subtitle = strings.copyCleanLinkDesc,
                                    palette = palette,
                                    testTag = "copy_clean_link_button",
                                    onClick = {
                                        copyToClipboard(article.cleanUrl)
                                        showCopySheet = false
                                    }
                                )
                                CopyOptionRow(
                                    icon = Icons.Filled.Share,
                                    title = strings.shareText,
                                    subtitle = article.siteName,
                                    palette = palette,
                                    testTag = "share_sheet_option_button",
                                    onClick = {
                                        showCopySheet = false
                                        shareText(article.fullCopyText, article.title)
                                    }
                                )
                            }
                        }
                    }

                    if (showAppearanceSheet) {
                        ModalBottomSheet(
                            onDismissRequest = { showAppearanceSheet = false },
                            containerColor = palette.surface,
                            contentColor = palette.textPrimary
                        ) {
                            ReaderAppearanceSheetContent(
                                preferences = preferences,
                                palette = palette,
                                strings = strings,
                                onUpdateFontSize = onUpdateFontSize,
                                onUpdateLineHeight = onUpdateLineHeight,
                                onUpdateSurfaceTheme = onUpdateSurfaceTheme,
                                onUpdateFontStyle = onUpdateFontStyle,
                                onUpdateShowImages = onUpdateShowImages
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderBottomActionBar(
    palette: ReaderPalette,
    strings: AppStrings,
    onCopyAll: () -> Unit,
    onOpenCopyMenu: () -> Unit,
    onOpenAppearance: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        color = palette.surface,
        border = BorderStroke(1.dp, palette.divider),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onCopyAll,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("copy_all_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.copyAll,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onOpenCopyMenu,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.divider),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("copy_menu_button")
            ) {
                Text(
                    text = strings.copyOptions,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textPrimary
                )
            }

            IconButton(
                onClick = onOpenAppearance,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("reader_settings_bottom_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.FormatSize,
                    contentDescription = strings.readerSettingsTitle,
                    tint = palette.textPrimary
                )
            }

            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("share_article_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = strings.shareText,
                    tint = palette.textPrimary
                )
            }
        }
    }
}

@Composable
private fun CopyOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    palette: ReaderPalette,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.background,
        border = BorderStroke(1.dp, palette.divider),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = palette.accent.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ReaderAppearanceSheetContent(
    preferences: ReaderPreferences,
    palette: ReaderPalette,
    strings: AppStrings,
    onUpdateFontSize: (Int) -> Unit,
    onUpdateLineHeight: (Float) -> Unit,
    onUpdateSurfaceTheme: (ReaderSurfaceTheme) -> Unit,
    onUpdateFontStyle: (ReaderFontStyle) -> Unit,
    onUpdateShowImages: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = strings.readerSettingsTitle,
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = strings.themeLabel,
                style = MaterialTheme.typography.labelLarge,
                color = palette.textSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val themes = listOf(
                    ReaderSurfaceTheme.PAPER to strings.themePaper,
                    ReaderSurfaceTheme.LIGHT to strings.themeLight,
                    ReaderSurfaceTheme.SEPIA to strings.themeSepia,
                    ReaderSurfaceTheme.DARK to strings.themeDark
                )
                themes.forEach { (theme, label) ->
                    val themePalette = theme.toPalette()
                    val selected = preferences.surfaceTheme == theme
                    Surface(
                        onClick = { onUpdateSurfaceTheme(theme) },
                        shape = RoundedCornerShape(12.dp),
                        color = themePalette.background,
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) palette.accent else palette.divider
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("theme_option_${theme.name.lowercase()}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = themePalette.textPrimary
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${strings.fontSizeLabel}: ${preferences.fontSizeSp}sp",
                style = MaterialTheme.typography.labelLarge,
                color = palette.textSecondary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onUpdateFontSize(preferences.fontSizeSp - 1) },
                    enabled = preferences.fontSizeSp > 14,
                    border = BorderStroke(1.dp, palette.divider),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease font size")
                }
                OutlinedButton(
                    onClick = { onUpdateFontSize(preferences.fontSizeSp + 1) },
                    enabled = preferences.fontSizeSp < 26,
                    border = BorderStroke(1.dp, palette.divider),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase font size")
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = strings.fontStyleLabel,
                style = MaterialTheme.typography.labelLarge,
                color = palette.textSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    ReaderFontStyle.SERIF to strings.fontSerif,
                    ReaderFontStyle.SANS to strings.fontSans
                ).forEach { (style, label) ->
                    val selected = preferences.fontStyle == style
                    Surface(
                        onClick = { onUpdateFontStyle(style) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) palette.accent.copy(alpha = 0.14f) else palette.background,
                        border = BorderStroke(1.dp, if (selected) palette.accent else palette.divider),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = if (style == ReaderFontStyle.SERIF) EditorialSerifFamily else EditorialSansFamily
                                ),
                                color = if (selected) palette.accent else palette.textPrimary
                            )
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = strings.lineSpacingLabel,
                style = MaterialTheme.typography.labelLarge,
                color = palette.textSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1.45f to "1.45×", 1.65f to "1.65×", 1.85f to "1.85×").forEach { (value, label) ->
                    val selected = kotlin.math.abs(preferences.lineHeightMultiplier - value) < 0.05f
                    Surface(
                        onClick = { onUpdateLineHeight(value) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) palette.accent.copy(alpha = 0.14f) else palette.background,
                        border = BorderStroke(1.dp, if (selected) palette.accent else palette.divider),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) palette.accent else palette.textPrimary
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = strings.showImagesLabel,
                style = MaterialTheme.typography.labelLarge,
                color = palette.textPrimary
            )
            Switch(
                checked = preferences.showImages,
                onCheckedChange = onUpdateShowImages
            )
        }
    }
}

@Composable
private fun ReaderLoadingView(
    domain: String,
    palette: ReaderPalette,
    strings: AppStrings
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = palette.accent,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = strings.loadingTitle,
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = domain,
            style = MaterialTheme.typography.labelLarge,
            color = palette.accent
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = strings.loadingSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary
        )
    }
}

@Composable
private fun ReaderErrorView(
    targetUrl: String,
    isInvalidUrl: Boolean,
    palette: ReaderPalette,
    strings: AppStrings,
    onRetry: () -> Unit,
    onOpenBrowser: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = palette.accent.copy(alpha = 0.14f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = strings.errorTitle,
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isInvalidUrl) strings.errorInvalidUrl else strings.errorNetwork,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!isInvalidUrl) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.retry)
                }
                OutlinedButton(
                    onClick = onOpenBrowser,
                    border = BorderStroke(1.dp, palette.divider),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.openInBrowser, color = palette.textPrimary)
                }
            }
        }
    }
}
