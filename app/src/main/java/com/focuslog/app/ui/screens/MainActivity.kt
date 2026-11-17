package com.focuslog.app.ui.screens

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.focuslog.app.data.local.FocusLogDatabase
import com.focuslog.app.data.repository.FocusLogRepository
import com.focuslog.app.service.AlarmService
import com.focuslog.app.ui.theme.FocusLogTheme
import com.focuslog.app.ui.viewmodel.FocusLogViewModel
import com.focuslog.app.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Single Activity hosting the Jetpack Compose NavHost with two destinations:
 * 1. HomeScreen (start destination)
 * 2. AlarmEntryScreen
 */
class MainActivity : ComponentActivity() {

    private val viewModel: FocusLogViewModel by viewModels {
        FocusLogViewModel.provideFactory(application)
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureLockscreenDisplay()
        checkPermissions()

        val incomingStartTime = intent.getLongExtra(AlarmService.EXTRA_START_TIME, -1L)
        val incomingDuration = intent.getLongExtra(AlarmService.EXTRA_DURATION_MS, -1L)

        val startDestination = if (incomingStartTime > 0 && incomingDuration > 0) {
            "alarm_entry/$incomingStartTime/$incomingDuration"
        } else {
            "home"
        }

        setContent {
            FocusLogTheme(darkTheme = true) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable("home") {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToAlarmEntry = { startTime, duration ->
                                navController.navigate("alarm_entry/$startTime/$duration")
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
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
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
            // Activity re-launched from alarm notification
            recreate()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
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

        CoroutineScope(Dispatchers.IO).launch {
            repo.logActivity(
                taskDescription = description.trim(),
                startTimeMs = startTimeMs,
                endTimeMs = now,
                durationMs = durationMs,
                dateString = dateString
            )
        }

        // Stop the alarm audio and release wake lock
        AlarmService.stop(this)
    }
}
