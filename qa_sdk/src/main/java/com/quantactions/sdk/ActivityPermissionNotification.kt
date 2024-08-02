package com.quantactions.sdk

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

interface ActivityPermissionNotification {

    fun createNotification(context: Context, channelID: String): Notification
}

open class ActivityPermissionNotificationImpl : ActivityPermissionNotification {


    override fun createNotification(
        context: Context,
        channelID: String,
    ): Notification {

        val intent = Intent(Intent.ACTION_MAIN)
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (launchIntent == null) {
            Timber.tag("CREATE NOT").e("Launch intent is null")
        } else {
            val mainActivity = launchIntent.component?.className
            if (mainActivity == null) {
                Timber.tag("CREATE NOT").e("Main activity is null")
            } else {
                Timber.tag("CREATE NOT")
                    .d("Open activity with package name " + context.packageName + " / class name " + mainActivity)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.setComponent(ComponentName(context.packageName, mainActivity))
                // optional: intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
        }

        val mBuilder = NotificationCompat.Builder(context, channelID)
        mBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        mBuilder.setSmallIcon(R.drawable.ic_equalizer_black_24dp)
        mBuilder.color = ContextCompat.getColor(
            context,
            R.color.brand_background_icon_color
        )
        mBuilder.setWhen(0)
        mBuilder.setOngoing(true)
        mBuilder.setContentTitle(context.getString(R.string.action_required_app_needs_a_permission))
        mBuilder.setContentText(context.getString(R.string.tap_to_open_and_grant_permission))
        mBuilder.setAutoCancel(true)
        mBuilder.setContentIntent(
            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        return mBuilder.build()
    }

}