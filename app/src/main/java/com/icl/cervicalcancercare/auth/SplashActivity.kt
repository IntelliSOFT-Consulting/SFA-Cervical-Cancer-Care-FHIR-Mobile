package com.icl.cervicalcancercare.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        lifecycleScope.launch {
            delay(3000) // 3 seconds
            val loggedIn = FormatterClass().getSharedPref("isLoggedIn", this@SplashActivity)
            if (loggedIn != null) {
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                this@SplashActivity.finish()
            } else {
                val intent = Intent(this@SplashActivity, LoginActivity::class.java)
                startActivity(intent)
                this@SplashActivity.finish()
            }
        }

    }
}