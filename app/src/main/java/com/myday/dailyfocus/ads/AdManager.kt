package com.myday.dailyfocus.ads

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.myday.dailyfocus.BuildConfig
import com.myday.dailyfocus.DailyFocusApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Google's public sample ad units always serve test creatives, regardless of account -- used in
 * debug builds so local runs never hit the real units (which risks invalid-traffic flags).
 * Release builds use the real ad units from the AdMob console instead.
 */
object AdIds {
    private const val BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val INTERSTITIAL_TEST_ID = "ca-app-pub-3940256099942544/1033173712"

    private const val BANNER_RELEASE_ID = "ca-app-pub-6345809643366034/6929708105"
    private const val INTERSTITIAL_RELEASE_ID = "ca-app-pub-6345809643366034/9497785837"

    val BANNER_AD_UNIT_ID = if (BuildConfig.DEBUG) BANNER_TEST_ID else BANNER_RELEASE_ID
    val INTERSTITIAL_AD_UNIT_ID = if (BuildConfig.DEBUG) INTERSTITIAL_TEST_ID else INTERSTITIAL_RELEASE_ID
}

class AdManager(private val context: Context) {

    private var interstitialAd: InterstitialAd? = null
    private val initStarted = AtomicBoolean(false)
    private val _adsReady = MutableStateFlow(false)

    /** True once the Mobile Ads SDK has been initialized; no ad is requested before this. */
    val adsReady: StateFlow<Boolean> = _adsReady.asStateFlow()

    /**
     * Starts the Mobile Ads SDK. Only call this once [ConsentManager.canRequestAds] is true.
     * Safe to call more than once; only the first call does anything.
     */
    fun initializeIfNeeded() {
        if (!initStarted.compareAndSet(false, true)) return
        MobileAds.initialize(context) {
            _adsReady.value = true
            loadInterstitial()
        }
    }

    fun loadInterstitial() {
        if (!_adsReady.value) return
        InterstitialAd.load(
            context,
            AdIds.INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    android.util.Log.d("AdMob", "Interstitial loaded successfully")
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    android.util.Log.e("AdMob", "Interstitial failed to load: ${error.message}")
                    interstitialAd = null
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onDismissed: () -> Unit = {}) {
        val ad = interstitialAd
        if (ad == null) {
            onDismissed()
            loadInterstitial()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitial()
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                loadInterstitial()
                onDismissed()
            }
        }
        ad.show(activity)
    }
}

@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as DailyFocusApplication
    val adsReady by app.adManager.adsReady.collectAsStateWithLifecycle()
    // Nothing is shown (and no ad request is made) until consent is resolved and the SDK is ready.
    if (!adsReady) return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdIds.BANNER_AD_UNIT_ID
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        android.util.Log.e("AdMob", "Banner failed to load: ${error.message}")
                    }
                    override fun onAdLoaded() {
                        android.util.Log.d("AdMob", "Banner loaded successfully")
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
