package com.focuslog.app.ui.screens

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuslog.app.R
import com.focuslog.app.data.local.FocusLogDatabase
import com.focuslog.app.data.repository.FocusLogRepository
import com.focuslog.app.service.AlarmService
import com.focuslog.app.ui.theme.AlertRed
import com.focuslog.app.ui.theme.FocusLogTheme
import com.focuslog.app.ui.theme.FocusPrimary
import com.focuslog.app.util.TimeUtils
import kotlinx.coroutines.launch

/**
 * Full-screen Lockscreen Alarm Activity.
 *
 * Rules:
 * - Wakes device, shows over lock screen.
 * - Looping alarm cannot be dismissed until user enters non-empty text describing what they did.
 * - Back button and dismissal gestures are intercepted and disabled.
 */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureLockscreenDisplay()
        blockBackPress()

        val startTimeMs = intent.getLongExtra(AlarmService.EXTRA_START_TIME, System.currentTimeMillis())
        val durationMs = intent.getLongExtra(AlarmService.EXTRA_DURATION_MS, 0L)

        setContent {
            FocusLogTheme(darkTheme = true) {
                AlarmDismissScreen(
                    startTimeMs = startTimeMs,
                    durationMs = durationMs,
                    onSubmitAndDismiss = { activityDescription ->
                        handleTaskSubmission(
                            description = activityDescription,
                            startTimeMs = startTimeMs,
                            durationMs = durationMs
                        )
                    }
                )
            }
        }
    }

    private fun configureLockscreenDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun blockBackPress() {
        // Enforce rule: alarm CANNOT be dismissed by back button!
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // No-op: user must type their log and submit!
            }
        })
    }

    private fun handleTaskSubmission(
        description: String,
        startTimeMs: Long,
        durationMs: Long
    ) {
        val now = System.currentTimeMillis()
        val dateString = TimeUtils.getCurrentDateIst()

        val db = FocusLogDatabase.getDatabase(applicationContext)
        val repo = FocusLogRepository(applicationContext, db.focusLogDao())

        // Use a detached coroutine on application context or runBlocking
        kotlinx.coroutines.GlobalScope.launch {
            repo.logActivity(
                taskDescription = description.trim(),
                startTimeMs = startTimeMs,
                endTimeMs = now,
                durationMs = durationMs,
                dateString = dateString
            )
        }

        // 1. Stop the looping alarm audio and release wake lock
        AlarmService.stop(this)

        // 2. Return to MainActivity
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(mainIntent)

        finish()
    }
}

@Composable
private fun AlarmDismissScreen(
    startTimeMs: Long,
    durationMs: Long,
    onSubmitAndDismiss: (String) -> Unit
) {
    var taskText by remember { mutableStateOf("") }
    val isSubmitEnabled = taskText.trim().isNotEmpty()

    // Pulsing animation for the alarm ring indicator
    val infiniteTransition = rememberInfiniteTransition(label = "AlarmPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F1115)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pulsing Alarm Icon Indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulseScale)
                    .background(AlertRed.copy(alpha = 0.2f), shape = CircleShape)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .background(AlertRed, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Alarm Title
            Text(
                text = stringResource(R.string.alarm_ringing),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = AlertRed
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Duration badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xFF1E232B), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = FocusPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Session Completed: ${TimeUtils.formatDuration(durationMs)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.LightGray,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Prompt
            Text(
                text = stringResource(R.string.alarm_prompt),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mandatory Text Input
            OutlinedTextField(
                value = taskText,
                onValueChange = { taskText = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.activity_input_hint),
                        color = Color.Gray
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FocusPrimary,
                    unfocusedBorderColor = Color(0xFF2C323D),
                    focusedContainerColor = Color(0xFF181B22),
                    unfocusedContainerColor = Color(0xFF181B22),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button (Cannot be clicked until user enters what they did)
            Button(
                onClick = {
                    if (isSubmitEnabled) {
                        onSubmitAndDismiss(taskText)
                    }
                },
                enabled = isSubmitEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FocusPrimary,
                    contentColor = Color(0xFF00363D),
                    disabledContainerColor = Color(0xFF232730),
                    disabledContentColor = Color.Gray
                )
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.submit_and_dismiss),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isSubmitEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Alarm will continue ringing until you log your task.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
