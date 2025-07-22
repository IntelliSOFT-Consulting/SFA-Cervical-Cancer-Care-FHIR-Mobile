package com.icl.cervicalcancercare.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.icl.cervicalcancercare.R

class Functions {
    fun saveSharedPref(key: String, value: String, context: Context) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun getSharedPref(key: String, context: Context): String? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE)
        return sharedPreferences.getString(key, null)
    }

    fun deleteSharedPref(key: String, context: Context) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove(key)
        editor.apply()
    }

    fun getInitials(name: String): String {
        val parts = name.trim().split(" ")
        val first = parts.getOrNull(0)?.firstOrNull()?.toString()?.uppercase() ?: ""
        val last = parts.getOrNull(1)?.firstOrNull()?.toString()?.uppercase() ?: ""
        return first + last
    }

    fun createAvatar(initials: String): Bitmap {
        val size = 300
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background circle
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#3F51B5") // Blue
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Text (initials)
        paint.color = Color.WHITE
        paint.textSize = 100f
        paint.textAlign = Paint.Align.CENTER
        val textBounds = Rect()
        paint.getTextBounds(initials, 0, initials.length, textBounds)
        val x = size / 2f
        val y = size / 2f - textBounds.exactCenterY()
        canvas.drawText(initials, x, y, paint)

        return bitmap
    }


}