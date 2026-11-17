package com.focuslog.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.focuslog.app.R
import com.focuslog.app.data.local.FocusLogEntry
import com.focuslog.app.ui.theme.CardOlderMuted
import com.focuslog.app.ui.theme.CardTopAccent
import com.focuslog.app.ui.theme.CardTopVibrant
import com.focuslog.app.ui.theme.FocusPrimary
import com.focuslog.app.util.TimeUtils

/**
 * Visual Stack of Cards representing submitted focus entries.
 *
 * Rules:
 * - Newest card is on top: fully colored, full scale, high elevation.
 * - Older cards: greyed out, slightly smaller, offset downwards/backwards, and sent behind.
 * - Toggleable expanded list view for viewing older entries in full detail.
 */
@Composable
fun CardStackView(
    entries: List<FocusLogEntry>,
    modifier: Modifier = Modifier
) {
    var isExpandedView by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Section Header with Stack / List Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = FocusPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Today's Logs (${entries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (entries.isNotEmpty()) {
                IconButton(onClick = { isExpandedView = !isExpandedView }) {
                    Icon(
                        imageVector = if (isExpandedView) Icons.Default.Layers else Icons.Default.List,
                        contentDescription = if (isExpandedView) "Stack View" else "List View",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (entries.isEmpty()) {
            EmptyStackPlaceholder()
        } else if (isExpandedView) {
            ExpandedCardListView(entries = entries)
        } else {
            LayeredCardStack(entries = entries, onExpandRequested = { isExpandedView = true })
        }
    }
}

/**
 * 3D-styled Layered Stack showing up to top 4 cards with older ones recessed behind.
 */
@Composable
private fun LayeredCardStack(
    entries: List<FocusLogEntry>,
    onExpandRequested: () -> Unit
) {
    // Show top 4 cards in the visual physical stack
    val visibleEntries = entries.take(4)
    val maxIndex = visibleEntries.size - 1

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Render from oldest (bottom) to newest (top)
        for (i in maxIndex downTo 0) {
            val entry = visibleEntries[i]
            val isTopCard = (i == 0)

            // Visual layering parameters
            val scale = (1.0f - (i * 0.05f)).coerceAtLeast(0.85f)
            val offsetY = (i * 20).dp
            val alpha = if (isTopCard) 1.0f else (0.80f - (i * 0.15f)).coerceAtLeast(0.40f)
            val zIndex = (maxIndex - i + 1).toFloat()

            FocusLogCardItem(
                entry = entry,
                isTopCard = isTopCard,
                modifier = Modifier
                    .zIndex(zIndex)
                    .offset(y = offsetY)
                    .scale(scale)
                    .alpha(alpha)
                    .clickable {
                        if (!isTopCard) onExpandRequested()
                    }
            )
        }
    }
}

/**
 * Individual Card Item.
 * Top card is vibrant and colored; older cards are greyed out and muted.
 */
@Composable
fun FocusLogCardItem(
    entry: FocusLogEntry,
    isTopCard: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isTopCard) CardTopVibrant else CardOlderMuted
    val borderColor = if (isTopCard) CardTopAccent else Color(0xFF2A313C)
    val taskTextColor = if (isTopCard) Color.White else Color(0xFF8A93A0)
    val metaTextColor = if (isTopCard) CardTopAccent else Color(0xFF6B7280)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(if (isTopCard) 1.5.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isTopCard) 10.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Time and Duration Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isTopCard) Icons.Default.CheckCircle else Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = if (isTopCard) FocusPrimary else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = TimeUtils.formatEpochToTimeIst(entry.endTimeMs),
                        style = MaterialTheme.typography.labelLarge,
                        color = metaTextColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    color = if (isTopCard) FocusPrimary.copy(alpha = 0.15f) else Color(0xFF242C35),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = TimeUtils.formatDuration(entry.durationMs),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isTopCard) FocusPrimary else Color(0xFF9CA3AF),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Body: User's Logged Activity Description
            Text(
                text = entry.taskDescription,
                style = MaterialTheme.typography.bodyLarge,
                color = taskTextColor,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp,
                fontWeight = if (isTopCard) FontWeight.Medium else FontWeight.Normal
            )

            // Footer: Session IST Date
            Text(
                text = entry.dateString,
                style = MaterialTheme.typography.labelSmall,
                color = if (isTopCard) Color.LightGray.copy(alpha = 0.6f) else Color.DarkGray
            )
        }
    }
}

/**
 * Scrollable full list view when user toggles expanded view.
 */
@Composable
private fun ExpandedCardListView(entries: List<FocusLogEntry>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(entries) { index, entry ->
            FocusLogCardItem(
                entry = entry,
                isTopCard = (index == 0)
            )
        }
    }
}

/**
 * Placeholder when no sessions have been logged today.
 */
@Composable
private fun EmptyStackPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(20.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.empty_stack_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
