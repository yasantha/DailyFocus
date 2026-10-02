package com.myday.dailyfocus.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.myday.dailyfocus.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wraps Google's User Messaging Platform (UMP) SDK. In the EEA, UK and Switzerland it shows the
 * consent message configured in AdMob (Privacy & messaging -> European regulations) before any
 * ad is requested. Everywhere else the form is not required and ads start straight away.
 */
class ConsentManager(context: Context) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context.applicationContext)

    private val _privacyOptionsRequired = MutableStateFlow(isPrivacyOptionsRequired())

    /** True when the user must be offered a way to change their choice (shown in Settings). */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /** True once consent has been gathered, or when consent is not required for this user. */
    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    /**
     * Refreshes the consent status and shows the consent form if one is required. Call once per
     * app launch from the Activity. [onComplete] runs when gathering is finished, successfully or
     * not; check [canRequestAds] afterwards before loading ads.
     */
    fun gatherConsent(activity: Activity, onComplete: (FormError?) -> Unit) {
        val params = ConsentRequestParameters.Builder().apply {
            if (BuildConfig.DEBUG && TEST_DEVICE_HASHED_ID.isNotEmpty()) {
                setConsentDebugSettings(
                    ConsentDebugSettings.Builder(activity)
                        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                        .addTestDeviceHashedId(TEST_DEVICE_HASHED_ID)
                        .build()
                )
            }
        }.build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    _privacyOptionsRequired.value = isPrivacyOptionsRequired()
                    onComplete(formError)
                }
            },
            { requestError ->
                onComplete(requestError)
            }
        )
    }

    /** Lets the user review or change their consent choice (Settings -> Privacy settings). */
    fun showPrivacyOptionsForm(activity: Activity, onDismissed: (FormError?) -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            _privacyOptionsRequired.value = isPrivacyOptionsRequired()
            onDismissed(formError)
        }
    }

    private fun isPrivacyOptionsRequired(): Boolean =
        consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    companion object {
        /**
         * To see the consent form from outside Europe in a debug build: run the app once, copy the
         * hashed device ID the UMP SDK prints in Logcat (search for "addTestDeviceHashedId"), and
         * paste it here. Leave empty for release builds.
         */
        private const val TEST_DEVICE_HASHED_ID = ""
    }
}
