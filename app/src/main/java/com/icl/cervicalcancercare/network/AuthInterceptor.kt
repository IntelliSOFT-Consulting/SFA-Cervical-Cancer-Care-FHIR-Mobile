package com.icl.cervicalcancercare.network

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import com.icl.cervicalcancercare.auth.LoginActivity
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val context: Context
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code == 401 || response.code == 403) {
//            // Unauthorized or Forbidden
//            val intent = Intent(context, LoginActivity::class.java).apply {
//                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            }
//            context.startActivity(intent)

            android.os.Handler(Looper.getMainLooper()).post {
                showSessionExpiredDialog(context)
            }
        }

        return response
    }

    private fun showSessionExpiredDialog(context: Context) {
        val activity = context as? Activity ?: return

        AlertDialog.Builder(activity)
            .setTitle("Session Expired")
            .setMessage("Your session has expired. Please log in again.")
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                FormatterClass().deleteSharedPref("access_token",context)
                FormatterClass().deleteSharedPref("isLoggedIn",context)
                val intent = Intent(context, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
            }
            .show()
    }
}
