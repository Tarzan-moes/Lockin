package com.lockin.app.presentation.coach

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lockin.app.domain.model.CoachMessage
import com.lockin.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═══════════════════════════════════════════════════════════════════════════════
// Public entry point
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun CoachScreen(viewModel: CoachViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    CoachScreenContent(
        uiState = uiState,
        onInputChanged = viewModel::onInputChanged,
        onSendMessage = viewModel::sendMessage,
        onRequestMotivation = viewModel::requestMotivation,
        onClearConversation = viewModel::clearConversation,
        onTabSelected = viewModel::onTabChanged
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// Main content
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun CoachScreenContent(
    uiState: CoachUiState,
    onInputChanged: (String) -> Unit = {},
    onSendMessage: () -> Unit = {},
    onRequestMotivation: () -> Unit = {},
    onClearConversation: () -> Unit = {},
    onTabSelected: (CoachTab) -> Unit = {}
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new messages or when thinking starts
    LaunchedEffect(uiState.messages.size, uiState.isThinking) {
        val target = uiState.messages.size - 1 + (if (uiState.isThinking) 1 else 0)
        if (target >= 0) {
            listState.animateScrollToItem(target.coerceAtLeast(0))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LockinBackground)
    ) {
        // ── Top bar ──────────────────────────────────────────────
        CoachTopBar(
            isModelReady = uiState.isModelReady,
            isInitializing = uiState.isInitializing,
            isThinking = uiState.isThinking,
            onClear = onClearConversation
        )

        // ── Tabs ────────────────────────────────────────────────
        TabRow(
            selectedTabIndex = uiState.currentTab.ordinal,
            containerColor = LockinSurface,
            contentColor = LockinPrimaryAccent,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.currentTab.ordinal]),
                    color = LockinPrimaryAccent
                )
            }
        ) {
            Tab(
                selected = uiState.currentTab == CoachTab.WORKOUT,
                onClick = { onTabSelected(CoachTab.WORKOUT) },
                text = { Text("Workout Coach") },
                icon = { Icon(Icons.Outlined.FitnessCenter, null) } // Provided by material-icons-extended usually
            )
            Tab(
                selected = uiState.currentTab == CoachTab.NUTRITION,
                onClick = { onTabSelected(CoachTab.NUTRITION) },
                text = { Text("Nutrition Coach") },
                icon = { Icon(Icons.Outlined.Restaurant, null) }
            )
        }

        // ── Model not ready banner ──────────────────────────────
        AnimatedVisibility(visible = !uiState.isModelReady && !uiState.isInitializing) {
            ModelStatusBanner()
        }

        // ── Error banner ────────────────────────────────────────
        uiState.error?.let { error ->
            ErrorBanner(error)
        }

        // ── Messages ────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Empty-state welcome
            if (uiState.messages.isEmpty() && !uiState.isInitializing) {
                item { EmptyCoachState() }
            }

            items(uiState.messages, key = { it.id }) { message ->
                ChatBubble(message)
            }

            if (uiState.isThinking) {
                item { TypingIndicator() }
            }
        }

        // ── Suggestion chips ────────────────────────────────────
        SuggestionChips(
            enabled = !uiState.isThinking && !uiState.isInitializing,
            onChipClick = { text ->
                onInputChanged(text)
                onSendMessage()
            },
            onMotivation = onRequestMotivation
        )

        // ── Input bar ───────────────────────────────────────────
        ChatInputBar(
            text = uiState.inputText,
            onTextChanged = onInputChanged,
            onSend = onSendMessage,
            enabled = !uiState.isThinking && !uiState.isInitializing
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Top bar
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CoachTopBar(
    isModelReady: Boolean,
    isInitializing: Boolean,
    isThinking: Boolean,
    onClear: () -> Unit
) {
    Surface(
        color = LockinSurface,
        shadowElevation = 2.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AI icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    LockinPrimaryAccent,
                                    LockinSecondaryAccent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Psychology,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "AI Coach",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = LockinTextPrimary
                    )
                    Text(
                        text = when {
                            isInitializing -> "Starting up…"
                            isThinking -> "Thinking…"
                            isModelReady -> "On-device · Offline"
                            else -> "Basic mode"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isThinking -> LockinPrimaryAccent
                            isModelReady -> LockinSecondaryAccent
                            else -> LockinTextSecondary
                        }
                    )
                }

                // Clear button
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Clear chat",
                        tint = LockinTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Progress bar during init
            if (isInitializing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = LockinPrimaryAccent,
                    trackColor = LockinBackground
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Banners
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ModelStatusBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = LockinWarning.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, LockinWarning.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = LockinWarning,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "AI model not loaded. Responses may be limited. " +
                        "Push the model file via ADB for full coaching.",
                style = MaterialTheme.typography.bodySmall,
                color = LockinWarning,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun ErrorBanner(error: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = LockinError.copy(alpha = 0.10f)
    ) {
        Text(
            error,
            style = MaterialTheme.typography.bodySmall,
            color = LockinError,
            modifier = Modifier.padding(12.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Empty state
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyCoachState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(LockinPrimaryAccent.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = LockinPrimaryAccent,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Ask me anything",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = LockinTextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Workouts · Recovery · Form · Nutrition",
            style = MaterialTheme.typography.bodyMedium,
            color = LockinTextSecondary
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Chat bubble
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChatBubble(message: CoachMessage) {
    val isUser = message.isFromUser

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Bubble
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = if (isUser) LockinPrimaryAccent else LockinSurface,
            border = if (!isUser) {
                androidx.compose.foundation.BorderStroke(
                    0.5.dp, LockinSubtleBorder
                )
            } else null
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = if (isUser) LockinOnPrimary else LockinTextPrimary
            )
        }

        // Timestamp
        if (message.timestamp > 0L) {
            Text(
                text = formatTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = LockinTextDisabled,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

private fun formatTime(ts: Long): String {
    return try {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))
    } catch (_: Exception) {
        ""
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Typing indicator
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
            color = LockinSurface,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, LockinSubtleBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Three animated dots
                repeat(3) { index ->
                    val infiniteTransition = rememberInfiniteTransition(label = "dot$index")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(LockinPrimaryAccent.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Suggestion chips
// ═══════════════════════════════════════════════════════════════════════════════

private data class Suggestion(val label: String, val text: String)

private val suggestions = listOf(
    Suggestion("What should I train?", "What should I train today?"),
    Suggestion("How's my recovery?", "How is my recovery looking?"),
    Suggestion("30-min full body", "Give me a 30-minute full body workout.")
)

@Composable
private fun SuggestionChips(
    enabled: Boolean,
    onChipClick: (String) -> Unit,
    onMotivation: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Motivation chip
        item {
            SuggestionChip(
                onClick = { if (enabled) onMotivation() },
                label = {
                    Text("💪 Motivate me", style = MaterialTheme.typography.labelMedium)
                },
                enabled = enabled,
                shape = RoundedCornerShape(20.dp),
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = LockinSurface,
                    labelColor = LockinTextPrimary,
                    disabledContainerColor = LockinSurface.copy(alpha = 0.5f),
                    disabledLabelColor = LockinTextDisabled
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = enabled,
                    borderColor = LockinSubtleBorder,
                    disabledBorderColor = LockinSubtleBorder.copy(alpha = 0.3f)
                )
            )
        }

        items(suggestions) { suggestion ->
            SuggestionChip(
                onClick = { if (enabled) onChipClick(suggestion.text) },
                label = {
                    Text(suggestion.label, style = MaterialTheme.typography.labelMedium)
                },
                enabled = enabled,
                shape = RoundedCornerShape(20.dp),
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = LockinSurface,
                    labelColor = LockinTextPrimary,
                    disabledContainerColor = LockinSurface.copy(alpha = 0.5f),
                    disabledLabelColor = LockinTextDisabled
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = enabled,
                    borderColor = LockinSubtleBorder,
                    disabledBorderColor = LockinSubtleBorder.copy(alpha = 0.3f)
                )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Input bar
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChatInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    val canSend = text.isNotBlank() && enabled

    Surface(
        color = LockinSurface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Ask about workouts, recovery, form…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LockinTextDisabled
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LockinPrimaryAccent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = LockinSurfaceElevated,
                    unfocusedContainerColor = LockinSurfaceElevated,
                    disabledContainerColor = LockinSurfaceElevated.copy(alpha = 0.5f),
                    focusedTextColor = LockinTextPrimary,
                    unfocusedTextColor = LockinTextPrimary,
                    disabledTextColor = LockinTextDisabled,
                    cursorColor = LockinPrimaryAccent
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
                singleLine = false,
                maxLines = 4,
                enabled = enabled
            )

            // Send button
            FilledIconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = LockinPrimaryAccent,
                    contentColor = LockinOnPrimary,
                    disabledContainerColor = LockinSurfaceElevated,
                    disabledContentColor = LockinTextDisabled
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
