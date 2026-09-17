package com.myday.dailyfocus.ads

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
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

    fun initialize() {
        MobileAds.initialize(context)
        loadInterstitial()
    }

    fun loadInterstitial() {
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
