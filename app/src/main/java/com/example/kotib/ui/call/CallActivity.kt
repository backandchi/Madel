package com.example.kotib.ui.call

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.KotibApp
import com.example.ui.theme.KotibTheme

class CallActivity : ComponentActivity() {

    companion object {
        const val EXTRA_IS_INCOMING = "extra_is_incoming"
        const val EXTRA_SUMMARY = "extra_summary"
        const val EXTRA_URGENCY = "extra_urgency"
        const val EXTRA_ATTEMPT = "extra_attempt"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreenFlags()

        val app = application as KotibApp
        val container = app.container

        val isIncoming = intent.getBooleanExtra(EXTRA_IS_INCOMING, false)
        val summary = intent.getStringExtra(EXTRA_SUMMARY) ?: ""
        val urgency = intent.getIntExtra(EXTRA_URGENCY, 5)
        val attempt = intent.getIntExtra(EXTRA_ATTEMPT, 1)

        setContent {
            KotibTheme {
                val callViewModel: CallViewModel = viewModel(
                    factory = CallViewModel.Factory(container)
                )

                // Boshlang'ich holatni o'rnatish
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    callViewModel.initCall(isIncoming, summary, urgency, attempt)
                }

                CallScreen(
                    viewModel = callViewModel,
                    onFinish = { finish() }
                )
            }
        }
    }

    private fun configureLockScreenFlags() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                keyguardManager?.requestDismissKeyguard(this, null)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            }
        } catch (_: Throwable) {}
    }
}
