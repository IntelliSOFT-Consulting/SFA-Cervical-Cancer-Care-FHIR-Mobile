package com.icl.cervicalcancercare.auth

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.icl.cervicalcancercare.MainActivity
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.network.FormatterClass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        applySystemBarAppearance()
        val root = findViewById<android.view.View>(R.id.main)
        val initialPaddingLeft = root.paddingLeft
        val initialPaddingTop = root.paddingTop
        val initialPaddingRight = root.paddingRight
        val initialPaddingBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                initialPaddingLeft + systemBars.left,
                initialPaddingTop + systemBars.top,
                initialPaddingRight + systemBars.right,
                initialPaddingBottom + systemBars.bottom
            )
            insets
        }

        lifecycleScope.launch {
            delay(3000) // 3 seconds
            val formatter = FormatterClass()
            val loggedIn = formatter.getSharedPref("isLoggedIn", this@SplashActivity)
            if (loggedIn != null) {
                val isInitialLocationSyncDone =
                    formatter.getSharedPref(
                        LocationDownloaderActivity.PREF_KEY_INITIAL_LOCATION_SYNC_COMPLETED,
                        this@SplashActivity
                    )
                        .toBoolean()

                val intent = if (isInitialLocationSyncDone) {
                    Intent(this@SplashActivity, MainActivity::class.java)
                } else {
                    Intent(this@SplashActivity, LocationDownloaderActivity::class.java)
                }
                startActivity(intent)
                this@SplashActivity.finish()
            } else {
                val intent = Intent(this@SplashActivity, LoginActivity::class.java)
                startActivity(intent)
                this@SplashActivity.finish()
            }
        }

    }

    private fun applySystemBarAppearance() {
        val isNightMode =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView)?.apply {
            isAppearanceLightStatusBars = !isNightMode
            isAppearanceLightNavigationBars = !isNightMode
        }
    }
}
