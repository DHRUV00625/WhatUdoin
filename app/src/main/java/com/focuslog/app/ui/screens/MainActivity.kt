package com.focuslog.app.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.focuslog.app.service.AlarmService
import com.focuslog.app.ui.theme.FocusLogTheme
import com.focuslog.app.ui.viewmodel.FocusLogViewModel

/**
 * Single Activity hosting the Jetpack Compose NavHost with two destinations:
 * 1. HomeScreen (start destination)
 * 2. AlarmEntryScreen (cannot be dismissed by back press or swipe until log submitted)
 *
 * Configured with lockscreen display flags:
 * - setShowWhenLocked(true)
 * - setTurnScreenOn(true)
 * - FLAG_KEEP_SCREEN_ON
 */
class MainActivity : ComponentActivity() {

    private val viewModel: FocusLogViewModel by viewModels {
        FocusLogViewModel.provideFactory(application)
    }

    private var currentDestination: String = "home"
    private var navController: NavHostController? = null

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureLockscreenDisplay()
        checkPermissions()
        setupBackPressInterceptor()

        val incomingStartTime = intent.getLongExtra(AlarmService.EXTRA_START_TIME, -1L)
        val incomingDuration = intent.getLongExtra(AlarmService.EXTRA_DURATION_MS, -1L)

        val startDestination = if (incomingStartTime > 0 && incomingDuration > 0) {
            "alarm_entry/$incomingStartTime/$incomingDuration"
        } else {
            "home"
        }
        currentDestination = startDestination

        setContent {
            FocusLogTheme(darkTheme = true) {
                val controller = rememberNavController()
                navController = controller

                val uiState by viewModel.uiState.collectAsState()

                // If timer expired or awaiting entry state is active, navigate to AlarmEntryScreen
                LaunchedEffect(uiState.isAwaitingEntry) {
                    if (uiState.isAwaitingEntry && !currentDestination.startsWith("alarm_entry")) {
                        currentDestination = "alarm_entry"
                        val startMs = if (uiState.currentSessionStartTimeMs > 0) {
                            uiState.currentSessionStartTimeMs
                        } else {
                            System.currentTimeMillis() - (uiState.totalSeconds * 1000L)
                        }
                        val durationMs = uiState.totalSeconds * 1000L
                        controller.navigate("alarm_entry/$startMs/$durationMs") {
                            popUpTo("home") { inclusive = false }
                        }
                    }
                }

                NavHost(
                    navController = controller,
                    startDestination = startDestination
                ) {
                    composable("home") {
                        currentDestination = "home"
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToAlarmEntry = { startTime, duration ->
                                currentDestination = "alarm_entry"
                                controller.navigate("alarm_entry/$startTime/$duration")
                            }
                        )
                    }

                    composable(
                        route = "alarm_entry/{startTimeMs}/{durationMs}",
                        arguments = listOf(
                            navArgument("startTimeMs") { type = NavType.LongType },
                            navArgument("durationMs") { type = NavType.LongType }
                        )
                    ) { backStackEntry ->
                        currentDestination = "alarm_entry"
                        val startTimeMs = backStackEntry.arguments?.getLong("startTimeMs")
                            ?: System.currentTimeMillis()
                        val durationMs = backStackEntry.arguments?.getLong("durationMs") ?: 0L

                        AlarmEntryScreen(
                            startTimeMs = startTimeMs,
                            durationMs = durationMs,
                            onSubmitAndDismiss = { activityDescription ->
                                handleTaskSubmission(
                                    description = activityDescription,
                                    startTimeMs = startTimeMs,
                                    durationMs = durationMs
                                )
                                currentDestination = "home"
                                controller.navigate("home") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val incomingStartTime = intent.getLongExtra(AlarmService.EXTRA_START_TIME, -1L)
        val incomingDuration = intent.getLongExtra(AlarmService.EXTRA_DURATION_MS, -1L)
        if (incomingStartTime > 0 && incomingDuration > 0) {
            currentDestination = "alarm_entry"
            navController?.navigate("alarm_entry/$incomingStartTime/$incomingDuration") {
                popUpTo("home") { inclusive = false }
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

    private fun checkPermissions() {
        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Exact Alarm permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }

    private fun setupBackPressInterceptor() {
        // Enforce rule: AlarmEntryScreen CANNOT be dismissed by back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentDestination.startsWith("alarm_entry")) {
                    // Intentionally blocked: user must type their log and submit
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    private fun handleTaskSubmission(
        description: String,
        startTimeMs: Long,
        durationMs: Long
    ) {
        val now = System.currentTimeMillis()
        val durationSeconds = durationMs / 1000L

        viewModel.insertLogEntry(
            text = description.trim(),
            startedAtMillis = startTimeMs,
            completedAtMillis = now,
            durationSeconds = durationSeconds
        )

        // Stop the alarm audio, vibration, and release wake lock
        AlarmService.stop(this)
    }
}
