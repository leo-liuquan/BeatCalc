/*
 * Copyright 2022 Erfan Sn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ir.erfansn.siliconecalculator

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SiliconeCalculator : Application(), Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private lateinit var appOpenAdManager: AppOpenAdManager
    private var currentActivity: Activity? = null

    override fun onCreate() {
        super<Application>.onCreate()

        // Initialize the Google Mobile Ads SDK once at app start.
        MobileAds.initialize(this) {}

        registerActivityLifecycleCallbacks(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdManager = AppOpenAdManager()
        appOpenAdManager.loadAd(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        currentActivity?.let { activity ->
            appOpenAdManager.showAdIfAvailable(activity)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        // Do not update currentActivity when an ad is showing (AdActivity).
        if (!appOpenAdManager.isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (!appOpenAdManager.isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    private inner class AppOpenAdManager {
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd: Boolean = false
            private set

        // Keep track of the time an app open ad is loaded to ensure you don't show an expired ad.
        private var loadTime: Long = 0

        fun loadAd(context: Context) {
            if (isLoadingAd || isAdAvailable()) return

            isLoadingAd = true
            val adRequest = AdRequest.Builder().build()
            AppOpenAd.load(
                context,
                context.getString(R.string.admob_app_open_ad_unit_id),
                adRequest,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        isLoadingAd = false
                        loadTime = Date().time
                        Log.d(TAG, "App open ad loaded.")
                    }

                    override fun onAdFailedToLoad(loadAdError: com.google.android.gms.ads.LoadAdError) {
                        isLoadingAd = false
                        Log.w(TAG, "App open ad failed to load: ${loadAdError.message}")
                    }
                }
            )
        }

        fun showAdIfAvailable(activity: Activity) {
            // If the app open ad is already showing, do not show the ad again.
            if (isShowingAd) return

            // If the app open ad is not available yet, load the ad.
            if (!isAdAvailable()) {
                loadAd(activity)
                return
            }

            val ad = appOpenAd ?: run {
                loadAd(activity)
                return
            }

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAd = false
                    Log.d(TAG, "App open ad dismissed.")
                    loadAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isShowingAd = false
                    Log.w(TAG, "App open ad failed to show: ${adError.message}")
                    loadAd(activity)
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "App open ad showed.")
                }
            }

            isShowingAd = true
            ad.show(activity)
        }

        private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
            val dateDifference: Long = Date().time - loadTime
            val numMilliSecondsPerHour: Long = 3_600_000
            return dateDifference < numMilliSecondsPerHour * numHours
        }

        private fun isAdAvailable(): Boolean {
            // App open ads expire after 4 hours.
            return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
        }
    }

    private companion object {
        private const val TAG = "AppOpenAdManager"
    }
}
