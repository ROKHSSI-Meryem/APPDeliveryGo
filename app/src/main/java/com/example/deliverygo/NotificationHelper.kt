package com.example.deliverygo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificationHelper {

    private const val CHANNEL_ID = "delivery_orders_v2"
    private const val OLD_CHANNEL_ID = "delivery"
    private const val ORDER_NOTIFICATION_ID = 2001
    const val REQUEST_CODE = 101

    private val ORANGE = Color.rgb(255, 152, 0)
    private val GREEN = Color.rgb(46, 125, 50)
    private val RED = Color.rgb(211, 47, 47)

    const val ACTION_OPEN_ORDER = "com.example.deliverygo.ACTION_OPEN_ORDER"
    const val ACTION_ACCEPT_ORDER = "com.example.deliverygo.ACTION_ACCEPT_ORDER"
    const val ACTION_REFUSE_ORDER = "com.example.deliverygo.ACTION_REFUSE_ORDER"
    const val EXTRA_ORDER_ID = "order_id"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.deleteNotificationChannel(OLD_CHANNEL_ID)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Delivery",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Delivery notifications"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                enableLights(true)
                lightColor = ORANGE
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun requestPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                activity, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE
            )
        }
    }

    fun canNotify(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun show(context: Context, title: String, message: String) {
        if (!canNotify(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_delivery_notification)
            .setColor(ORANGE)
            .setLargeIcon(emojiBitmap("🛵"))
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun orderIntent(context: Context, order: Order, action: String, code: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            this.action = action
            putExtra(EXTRA_ORDER_ID, order.id)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Notification "nouvelle commande" avec les boutons Accepter / Refuser. */
    @SuppressLint("MissingPermission")
    fun showOrder(context: Context, order: Order) {
        if (!canNotify(context)) return

        val title = "🛵 " + context.getString(R.string.order_notification_title)
        val shortText = context.getString(
            R.string.order_notification_text, order.clientName, order.pickupName
        )
        val details = context.getString(
            R.string.order_card_format, order.pickupName, order.clientName, order.clientAddress
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_delivery_notification)
            .setColor(ORANGE)
            .setLargeIcon(emojiBitmap("🛵"))
            .setContentTitle(title)
            .setContentText(shortText)
            .setSubText("DeliveryGo")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(details)
            )
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000L)
            .setContentIntent(orderIntent(context, order, ACTION_OPEN_ORDER, order.id * 10))
            .addAction(
                0,
                coloredLabel(context.getString(R.string.order_accept), GREEN),
                orderIntent(context, order, ACTION_ACCEPT_ORDER, order.id * 10 + 1)
            )
            .addAction(
                0,
                coloredLabel(context.getString(R.string.order_refuse), RED),
                orderIntent(context, order, ACTION_REFUSE_ORDER, order.id * 10 + 2)
            )
            .build()

        NotificationManagerCompat.from(context).notify(ORDER_NOTIFICATION_ID, notification)
    }

    fun cancelOrder(context: Context) {
        NotificationManagerCompat.from(context).cancel(ORDER_NOTIFICATION_ID)
    }

    private fun coloredLabel(text: String, color: Int): CharSequence =
        SpannableString(text).apply {
            setSpan(ForegroundColorSpan(color), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

    private fun emojiBitmap(emoji: String, sizePx: Int = 128): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sizePx * 0.8f
            textAlign = Paint.Align.CENTER
        }
        val y = sizePx / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(emoji, sizePx / 2f, y, paint)
        return bitmap
    }
}