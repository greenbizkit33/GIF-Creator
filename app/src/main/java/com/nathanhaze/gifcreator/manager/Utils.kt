package com.nathanhaze.gifcreator.manager

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import com.google.android.gms.ads.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.nathanhaze.gifcreator.R

object Utils {

    fun getPurchased(activity: Activity): Boolean {
        val sharedPref = activity?.getSharedPreferences(
            activity.getString(R.string.preference_app), Context.MODE_PRIVATE
        )
        return sharedPref.getBoolean(activity.getString(R.string.preference_purchased), false)
    }

    @SuppressLint("MissingPermission")
    fun getAdmobAd(activity: Activity?): AdView? {
        val adView = AdView(activity)
        adView.adSize = activity?.let { getAdSize(it) }
        if (activity != null) {
            adView.adUnitId = activity.getString(R.string.banner_ad_unit_id)
        }
        val adRequest = AdRequest.Builder().build()
        adView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {}
            override fun onAdLoaded() {
                super.onAdLoaded()
            }
        }
        adView.loadAd(adRequest)
        return adView
    }

    fun getAdSize(activity: Activity): AdSize? {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()

        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    /***
     * Tracking screen view
     *
     * @param screenName screen name to be displayed on GA dashboard
     */
    @SuppressLint("MissingPermission")
    fun trackScreenView(act: Activity?, screenName: String?) {
        if (act != null) {
            FirebaseAnalytics.getInstance(act)
                .setCurrentScreen(act, screenName, null /* class override */)
        }
    }

    @SuppressLint("MissingPermission")
    fun trackEvent(bundle: Bundle?, name: String?, context: Context) {
        var name = name
        if (name != null && !name.isEmpty()) {
            name = name.replace(" ", "_")
        }
        FirebaseAnalytics.getInstance(context).logEvent(name!!, bundle)
    }
}