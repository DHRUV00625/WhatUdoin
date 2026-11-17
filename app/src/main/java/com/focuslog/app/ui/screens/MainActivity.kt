package com.focuslog.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.focuslog.app.ui.theme.FocusLogTheme
import com.focuslog.app.ui.viewmodel.FocusLogViewModel

/**
 * Main Activity for FocusLog.
 * Checks and requests runtime permissions required for alarm notifications.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: FocusLogViewModel by viewModels {
        FocusLogViewModel.provideFactory(application)
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions()

        setContent {
            FocusLogTheme(darkTheme = true) {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    private fun checkPermissions() {
        // Notification permission for Android 13+ (API 33+)
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
}
