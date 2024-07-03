package com.quantactions.sdk

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Keep
interface DataCollectionNotification {

    val qa: QA
    @Keep
    fun updateNotification()
    @Keep
    fun createNotification(context: Context, channelID: String): Notification
}
@Keep
open class UpdateTaps(override val qa: QA) : DataCollectionNotification {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private var lastTaps = 0
    private var lastSpeed = 0.0

    @Keep
    override fun updateNotification() {
        scope.launch {
            val last = qa.getLastTaps(QA.Flag.DAY)
            lastTaps = last.totalTaps
            lastSpeed = (last.speed.sum() / last.speed.size).toDouble()
        }
    }
    @Keep
    override fun createNotification(
        context: Context,
        channelID: String,
    ): Notification {

        val pauseIntent = Intent(context, QABroadcastReceiver::class.java)
        pauseIntent.action = "pauseCollection"

        val pausePendingIntent = PendingIntent.getBroadcast(context, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val resumeIntent = Intent(context, QABroadcastReceiver::class.java)
        resumeIntent.action = "resumeCollection"

        val resumePendingIntent = PendingIntent.getBroadcast(context, 0, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val mBuilder = NotificationCompat.Builder(context, channelID)
        mBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        mBuilder.setSmallIcon(R.drawable.ic_equalizer_black_24dp)
        mBuilder.color = ContextCompat.getColor(
            context,
            R.color.brand_background_icon_color
        )
        mBuilder.setWhen(0)
        mBuilder.setOngoing(true)
        mBuilder.setContentText("Taps last 24h: $lastTaps\nSpeed last 24h: ${"%.2f".format(lastSpeed * 60)} taps/m")

        return mBuilder.build()
    }

}