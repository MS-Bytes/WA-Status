package com.msbytes.statussaver.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google User Messaging Platform (GDPR / US-state privacy).
 * Consent info is refreshed on every launch before any ad is requested.
 */
@Singleton
class ConsentManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    val canRequestAds: Boolean get() = consentInformation.canRequestAds()

    /** Calls [onCanRequestAds] (possibly more than once) as soon as ads may be requested. */
    fun gatherConsent(activity: Activity, onCanRequestAds: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    updatePrivacyOptions()
                    if (canRequestAds) onCanRequestAds()
                }
            },
            {
                // Offline or misconfigured: fall back to the previous session's consent.
                updatePrivacyOptions()
                if (canRequestAds) onCanRequestAds()
            },
        )
        // Consent obtained in a previous session lets ads start immediately.
        if (canRequestAds) onCanRequestAds()
    }

    fun showPrivacyOptionsForm(activity: Activity, onDismissed: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            updatePrivacyOptions()
            onDismissed()
        }
    }

    private fun updatePrivacyOptions() {
        _privacyOptionsRequired.value = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }
}
