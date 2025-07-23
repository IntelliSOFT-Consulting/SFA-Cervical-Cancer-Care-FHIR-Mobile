package com.icl.cervicalcancercare.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.auth.LoginActivity
import com.icl.cervicalcancercare.network.FormatterClass
import java.util.UUID

class Functions {

      fun generateUuid(): String {
        return UUID.randomUUID().toString()
    }
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun showConnectionAlertDialog(
        context: Context,
        title: String = "No Internet Connection",
        message: String = "An active internet connection is required.\nPlease check your internet connection and try again.",
        confirmText: String = "Confirm",
        cancelText: String = "Cancel",
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {

        val view: View = LayoutInflater.from(context).inflate(R.layout.confirm_dialog, null)
        val titleTextView = view.findViewById<TextView>(R.id.dialogTitle)
        val messageTextView = view.findViewById<TextView>(R.id.dialogMessage)
        val confirmButton = view.findViewById<MaterialButton>(R.id.confirmButton)
        val cancelButton = view.findViewById<MaterialButton>(R.id.cancelButton)

        titleTextView.text = title
        messageTextView.text = message
        confirmButton.text = confirmText
        cancelButton.text = cancelText

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()

        confirmButton.setOnClickListener {
            onConfirm()
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            onCancel?.invoke()
            dialog.dismiss()
        }

        dialog.show()

    }

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