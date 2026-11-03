package com.arjunvision.arjunfitai.data.samsung

import android.content.Context
import com.arjunvision.arjunfitai.data.health.HealthDataSource

/**
 * "open" build: built without the Samsung Health Data SDK (it can't be redistributed), so there is
 * no Samsung source. Health Connect and Demo mode cover this build.
 */
@Suppress("UNUSED_PARAMETER")
fun createSamsungSource(context: Context): HealthDataSource? = null
