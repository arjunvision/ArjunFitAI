package com.arjunvision.arjunfitai.data.samsung

import android.content.Context
import com.arjunvision.arjunfitai.data.health.HealthDataSource

/** "samsung" build: Samsung Health is available as a source. (The "open" build's version returns null.) */
fun createSamsungSource(context: Context): HealthDataSource? = SamsungHealthSource(context)
