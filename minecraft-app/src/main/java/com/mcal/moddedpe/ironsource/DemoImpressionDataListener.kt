package com.mcal.moddedpe.ironsource

import android.util.Log
import com.ironsource.mediationsdk.impressionData.ImpressionData
import com.ironsource.mediationsdk.impressionData.ImpressionDataListener

class DemoImpressionDataListener : ImpressionDataListener {
    private val TAG = DemoImpressionDataListener::class.java.name

    /**
    Called when the ad was displayed successfully and the impression data was recorded
    @param impressionData The recorded impression data
     */
    override fun onImpressionSuccess(impressionData: ImpressionData) {
        Log.e(TAG, "")
    }
}
