/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * Interface for creating notifications to request activity permissions.
 * Implement this interface to customize the notification that will be shown to the user
 * when the app needs an activity permission.
 * @see ActivityPermissionNotificationImpl
 */
interface ActivityPermissionNotification {

    fun createNotification(context: Context, channelID: String): Notification
}

/**
 * Default implementation of ActivityPermissionNotification.
 */
open class ActivityPermissionNotificationImpl : ActivityPermissionNotification {

    /**
     *  It creates a notification that opens the main activity of the app when tapped.
     *  @param context Android context
     *  @param channelID Notification channel ID
     *  @return Notification
     */
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
                    .d("Open activity with package name ${context.packageName} / class name $mainActivity")
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.component = ComponentName(context.packageName, mainActivity)
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
        mBuilder.setContentTitle(context.getString(R.string.qa_sdk_notification_title_app_needs_a_permission))
        mBuilder.setContentText(context.getString(R.string.qa_sdk_notification_body_tap_to_open_and_grant_permission))
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