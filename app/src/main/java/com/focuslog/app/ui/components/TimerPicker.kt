package com.focuslog.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuslog.app.R
import com.focuslog.app.ui.theme.AlertRed
import com.focuslog.app.ui.theme.FocusPrimary
import java.util.Locale

/**
 * Timer Configuration and Live Countdown Display Component for HomeScreen.
 *
 * Supports:
 * - Big live countdown display continuously computed from persisted end time.
 * - Custom duration picker (minutes + seconds with +/- buttons and direct input).
 * - Quick-preset chips: 15m, 25m, 45m, 60m.
 * - Distinct Start and Cancel controls.
 */
@Composable
fun TimerSection(
    isRunning: Boolean,
    remainingSeconds: Long,
    totalSeconds: Long,
    minutesInput: String,
    secondsInput: String,
    onMinutesChange: (String) -> Unit,
    onSecondsChange: (String) -> Unit,
    onStartTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    onResetInputs: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isRunning) {
                // Live Countdown State (derived from persisted end time)
                ActiveCountdownDisplay(
                    remainingSeconds = remainingSeconds,
                    totalSeconds = totalSeconds,
                    onCancel = onCancelTimer
                )
            } else {
                // Custom Duration Setup State
                TimerSetupInput(
                    minutes = minutesInput,
                    seconds = secondsInput,
                    onMinutesChange = onMinutesChange,
                    onSecondsChange = onSecondsChange,
                    onStart = onStartTimer,
                    onReset = onResetInputs
                )
            }
        }
    }
}

/**
 * Big live countdown display with circular progress ring and accurate seconds remaining.
 */
@Composable
private fun ActiveCountdownDisplay(
    remainingSeconds: Long,
    totalSeconds: Long,
    onCancel: () -> Unit
) {
    val progress = if (totalSeconds > 0) {
        (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Status header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(FocusPrimary.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(FocusPrimary, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "FOCUS SESSION RUNNING",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = FocusPrimary,
                letterSpacing = 1.2.sp
            )
        }

        // Big countdown circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(210.dp)
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(200.dp),
                color = FocusPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 12.dp
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 52.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "REMAINING",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
        }

        // Cancel Timer Button
        OutlinedButton(
            onClick = onCancel,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
            border = BorderStroke(1.2.dp, AlertRed.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(46.dp)
        ) {
            Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.cancel_timer),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Custom Duration Picker with big preview, quick-chips, and minute/second adjustment fields.
 */
@Composable
private fun TimerSetupInput(
    minutes: String,
    seconds: String,
    onMinutesChange: (String) -> Unit,
    onSecondsChange: (String) -> Unit,
    onStart: () -> Unit,
    onReset: () -> Unit
) {
    val minutesVal = minutes.toLongOrNull() ?: 0L
    val secondsVal = seconds.toLongOrNull() ?: 0L
    val isValid = (minutesVal > 0 || secondsVal > 0)
    val previewFormatted = String.format(Locale.US, "%02d:%02d", minutesVal, secondsVal)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SET FOCUS DURATION",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp
        )

        // Big Duration Display
        Text(
            text = previewFormatted,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp),
            fontWeight = FontWeight.ExtraBold,
            color = if (isValid) FocusPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            letterSpacing = (-1).sp
        )

        // Quick Preset Chips (15, 25, 45, 60 min)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            listOf("15", "25", "45", "60").forEach { preset ->
                QuickPresetChip(
                    label = "${preset}m",
                    isSelected = (minutes == preset && (seconds.isEmpty() || seconds == "0" || seconds == "00")),
                    onClick = {
                        onMinutesChange(preset)
                        onSecondsChange("0")
                    }
                )
            }
        }

        // Custom Duration Number Fields with +/- controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Minutes Stepper & Field
            DurationStepper(
                value = minutes,
                label = stringResource(R.string.minutes_label),
                onValueChange = onMinutesChange,
                onIncrement = {
                    val current = minutes.toLongOrNull() ?: 0L
                    onMinutesChange((current + 1).coerceAtMost(180L).toString())
                },
                onDecrement = {
                    val current = minutes.toLongOrNull() ?: 0L
                    if (current > 0) onMinutesChange((current - 1).toString())
                }
            )

            Text(
                text = ":",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            // Seconds Stepper & Field
            DurationStepper(
                value = seconds,
                label = stringResource(R.string.seconds_label),
                placeholder = "00",
                onValueChange = onSecondsChange,
                onIncrement = {
                    val current = seconds.toLongOrNull() ?: 0L
                    onSecondsChange(((current + 5) % 60).toString())
                },
                onDecrement = {
                    val current = seconds.toLongOrNull() ?: 0L
                    val next = if (current >= 5) current - 5 else 55
                    onSecondsChange(next.toString())
                }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Start and Reset Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier
                    .weight(0.35f)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onStart,
                enabled = isValid,
                modifier = Modifier
                    .weight(0.65f)
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FocusPrimary,
                    contentColor = Color(0xFF00363D),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.start_timer),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * Numeric field with + and - increment buttons.
 */
@Composable
private fun DurationStepper(
    value: String,
    label: String,
    placeholder: String = "25",
    onValueChange: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onIncrement,
            modifier = Modifier.size(32.dp),
            colors = IconButtonDefaults.iconButtonColors(contentColor = FocusPrimary)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Increase $label", modifier = Modifier.size(18.dp))
        }

        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                if (input.all { it.isDigit() } && input.length <= 3) {
                    onValueChange(input)
                }
            },
            placeholder = { Text(placeholder, color = Color.Gray) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FocusPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background
            ),
            modifier = Modifier.width(88.dp),
            textStyle = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            shape = RoundedCornerShape(14.dp)
        )

        IconButton(
            onClick = onDecrement,
            modifier = Modifier.size(32.dp),
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
        ) {
            Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease $label", modifier = Modifier.size(18.dp))
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Selectable quick chip for duration presets (15m, 25m, 45m, 60m).
 */
@Composable
private fun QuickPresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) FocusPrimary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color(0xFF00363D) else MaterialTheme.colorScheme.onSurface
        )
    }
}
