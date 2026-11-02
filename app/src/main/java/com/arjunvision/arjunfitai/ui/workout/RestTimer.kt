package com.arjunvision.arjunfitai.ui.workout

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.arjunvision.arjunfitai.MainActivity
import com.arjunvision.arjunfitai.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil

/**
 * Rest countdown between sets. Counts against a wall-clock end time, so it stays right if the screen
 * pauses drawing. When it reaches zero it posts a vibrating, high-priority notification — Galaxy
 * Wearable mirrors phone notifications to the Fit3, which is what makes the band buzz.
 */
class RestTimer(private val context: Context, private val scope: CoroutineScope) {
    var running by mutableStateOf(false); private set
    /** Rest is over ("GO!") until the next set or Skip. */
    var finished by mutableStateOf(false); private set
    var remaining by mutableIntStateOf(0); private set
    var total by mutableIntStateOf(DEFAULT_SECONDS); private set

    private var endAt = 0L
    private var nextLabel = ""
    private var job: Job? = null

    fun start(seconds: Int, next: String) {
        total = seconds
        endAt = System.currentTimeMillis() + seconds * 1000L
        nextLabel = next
        finished = false
        running = true
        cancelNotification()
        tick()
    }

    /** −15 / +15 seconds. */
    fun adjust(deltaSeconds: Int) {
        if (!running) return
        endAt = maxOf(System.currentTimeMillis(), endAt + deltaSeconds * 1000L)
        total = maxOf(total, remainingNow())
        tick()
    }

    fun stop() {
        job?.cancel()
        running = false
        finished = false
        cancelNotification()
    }

    private fun remainingNow() = ceil((endAt - System.currentTimeMillis()) / 1000.0).toInt().coerceAtLeast(0)

    private fun tick() {
        job?.cancel()
        job = scope.launch {
            while (true) {
                remaining = remainingNow()
                if (remaining <= 0) {
                    running = false
                    finished = true
                    notifyRestOver()
                    break
                }
                delay(200)
            }
        }
    }

    private fun notifyRestOver() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_arjunfit)
            .setContentTitle("Rest over — go!")
            .setContentText(nextLabel)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVibrate(VIBRATION)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    private fun cancelNotification() = NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)

    companion object {
        const val DEFAULT_SECONDS = 90
        private const val CHANNEL_ID = "rest_timer"
        private const val NOTIFICATION_ID = 4201
        private val VIBRATION = longArrayOf(0, 400, 200, 400)

        fun ensureChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Rest timer", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Buzzes when your rest between sets is over"
                    enableVibration(true)
                    vibrationPattern = VIBRATION
                },
            )
        }
    }
}
