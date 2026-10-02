package com.myday.dailyfocus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.myday.dailyfocus.ui.theme.DailyFocusTheme

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gatherConsentAndStartAds()
        setContent {
            DailyFocusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    LaunchedEffect(Unit) {
                        requestNotificationPermissionIfNeeded()
                    }
                    DailyFocusApp()
                }
            }
        }
    }

    /**
     * Shows the GDPR consent form where required (EEA, UK, Switzerland), then starts ads once
     * they are allowed. Users who already answered on a previous launch start ads immediately.
     */
    private fun gatherConsentAndStartAds() {
        val app = application as DailyFocusApplication
        val consent = app.consentManager
        consent.gatherConsent(this) { error ->
            if (error != null) {
                Log.w("Consent", "Consent gathering failed (${error.errorCode}): ${error.message}")
            }
            if (consent.canRequestAds) {
                app.adManager.initializeIfNeeded()
            }
        }
        if (consent.canRequestAds) {
            app.adManager.initializeIfNeeded()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
