package com.isaka.munavigation.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import kotlin.math.roundToInt

object Mapbox {
    fun locationDistanceConverter(meters: Double): String {
        return if (meters >= 1000) {
            val distance = ((meters / 1000) * 10.0).roundToInt() / 10.0
            "$distance km"
        } else {
            val distance = meters.roundToInt()
            "$distance m"
        }
    }

    fun locationDurationConverter(seconds: Double): String {
        var n = seconds
        val day = n / (24 * 3600)

        n %= (24 * 3600)
        val hour = n / 3600

        n %= 3600
        val minute = n / 60

        return "${day.toInt()}:${hour.toInt()}:${minute.toInt()}"
    }

    // convert given drawable id to bitmap
    fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
        if (sourceDrawable == null) {
            return null
        }
        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else {
            // copying drawable object to not manipulate on the same reference
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }
}