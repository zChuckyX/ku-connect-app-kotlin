package com.example.ku_connect.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.ku_connect.databinding.ActivityLoginBinding
import com.example.ku_connect.ui.MainActivity
import com.example.ku_connect.util.Extensions.showToast
import com.example.ku_connect.viewmodel.AuthViewModel
import com.example.ku_connect.viewmodel.UiState

class LoginFragment : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (viewModel.isLoggedIn) {
            goToMain()
            return
        }

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            clearErrors()

            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            viewModel.login(email, password)
        }

        binding.tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterFragment::class.java))
        }
    }

    private fun observeState() {
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        viewModel.loginState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnLogin.isEnabled = false
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    goToMain()
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true

                    if (state.message.contains("อีเมล")) {
                        val text = "เมลต้องเป็น @ku.th เท่านั้น"

                        binding.tilEmail.error = text
                        showToast(text)
                    } else {
                        val text = "รหัสผ่านหรืออีเมลไม่ถูกต้อง"

                        binding.tilPassword.error = text
                        showToast(text)

                    }
                }
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun clearErrors() {
        binding.tilEmail.error = null
        binding.tilPassword.error = null
    }
}