package com.icl.cervicalcancercare.auth

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.databinding.ActivityLoginBinding
import com.icl.cervicalcancercare.models.Login
import com.icl.cervicalcancercare.network.RetrofitCallsAuthentication
import com.icl.cervicalcancercare.utils.Functions

class LoginActivity : AppCompatActivity() {

    private var retrofitCallsAuthentication = RetrofitCallsAuthentication()

    //let's use binding
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarAppearance()
        val initialPaddingLeft = binding.main.paddingLeft
        val initialPaddingTop = binding.main.paddingTop
        val initialPaddingRight = binding.main.paddingRight
        val initialPaddingBottom = binding.main.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                initialPaddingLeft + systemBars.left,
                initialPaddingTop + systemBars.top,
                initialPaddingRight + systemBars.right,
                initialPaddingBottom + systemBars.bottom
            )
            insets
        }

        binding.apply {
            usernameEditText.doAfterTextChanged {
                usernameInputLayout.error = null
            }

            passwordEditText.doAfterTextChanged {
                passwordInputLayout.error = null
            }

            loginButton.apply {
                setOnClickListener {
                    val email = binding.usernameEditText.text.toString()
                    val password = binding.passwordEditText.text.toString()

                    if (email.isEmpty()) {
                        binding.usernameInputLayout.error =
                            getString(R.string.login_error_username_required)
                        return@setOnClickListener
                    }
                    // check password
                    if (password.isEmpty()) {
                        binding.passwordInputLayout.error =
                            getString(R.string.login_error_password_required)
                        return@setOnClickListener
                    }

                    // check if there's internet connection & display a connection alert dialog
                    if (!Functions().isInternetAvailable(this@LoginActivity)) {

                        Functions().showConnectionAlertDialog(
                            context = this@LoginActivity,
                            title = getString(R.string.login_no_internet_title),
                            message = getString(R.string.login_no_internet_message),
                            onConfirm = {
                                // Perform delete logic
                            },
                            onCancel = {
                                // Optional cancel logic
                            }
                        )

                        return@setOnClickListener
                    }


                    val dbSignIn = Login(username = email, password = password)
                    retrofitCallsAuthentication.loginUser(this@LoginActivity, dbSignIn)

                }
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
