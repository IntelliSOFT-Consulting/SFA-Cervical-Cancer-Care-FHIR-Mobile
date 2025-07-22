package com.icl.cervicalcancercare.auth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.databinding.ActivityLoginBinding
import com.icl.cervicalcancercare.models.Login
import com.icl.cervicalcancercare.network.RetrofitCallsAuthentication

class LoginActivity : AppCompatActivity() {

    private var retrofitCallsAuthentication = RetrofitCallsAuthentication()

    //let's use binding
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.apply {
            loginButton.apply {
                setOnClickListener {
                    val email = binding.usernameEditText.text.toString()
                    val password = binding.passwordEditText.text.toString()

                    if (email.isEmpty()) {
                        binding.usernameInputLayout.error = "Please enter username"
                        return@setOnClickListener
                    }
                    // check password
                    if (password.isEmpty()) {
                        binding.passwordInputLayout.error = "Please enter password"
                        return@setOnClickListener
                    }

                    val dbSignIn = Login(username = email, password = password)
                    retrofitCallsAuthentication.loginUser(this@LoginActivity, dbSignIn)

                }
            }
        }
    }
}