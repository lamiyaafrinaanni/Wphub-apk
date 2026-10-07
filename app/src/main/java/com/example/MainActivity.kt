package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notifications.NotificationHelper
import com.example.ui.WPHubApp
import com.example.ui.WPHubViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {
    private val viewModel: WPHubViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            MyApplicationTheme(themeMode = themeMode) {
                WPHubApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val destinationTab = intent.getStringExtra(NotificationHelper.EXTRA_DESTINATION_TAB)
            ?: intent.getStringExtra("destination_tab")
        val targetType = intent.getStringExtra(NotificationHelper.EXTRA_TARGET_TYPE)
            ?: intent.getStringExtra("target_type")
        val targetId = intent.getStringExtra(NotificationHelper.EXTRA_TARGET_ID)
            ?: intent.getStringExtra("target_id")
        val siteId = intent.getStringExtra(NotificationHelper.EXTRA_SITE_ID)
            ?: intent.getStringExtra("site_id")

        if (destinationTab != null || targetType != null) {
            val sessionToken = intent.getStringExtra("session_token")
            val isTrusted = sessionToken == NotificationHelper.sessionToken

            if (isTrusted) {
                viewModel.handleNotificationNavigation(
                    destinationTab = destinationTab,
                    targetType = targetType,
                    targetId = targetId,
                    siteId = siteId
                )
            } else {
                viewModel.requestExternalAction(
                    destinationTab = destinationTab,
                    targetType = targetType,
                    targetId = targetId,
                    siteId = siteId
                )
            }
        }
    }
}

