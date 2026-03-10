package com.example.ku_connect.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.ku_connect.databinding.ActivityRegisterBinding
import com.example.ku_connect.ui.MainActivity
import com.example.ku_connect.util.Extensions.showToast
import com.example.ku_connect.viewmodel.AuthViewModel
import com.example.ku_connect.viewmodel.UiState

class RegisterFragment : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            clearErrors()

            val username = binding.etUsername.text.toString()
            val email    = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val confirm  = binding.etConfirmPassword.text.toString()

            viewModel.register(username, email, password, confirm)
        }

        binding.tvGoLogin.setOnClickListener { finish() }
    }

    private fun observeState() {
        viewModel.registerState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnRegister.isEnabled = false
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegister.isEnabled = true

                    showToast("สมัครสมาชิกสำเร็จ!")

                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegister.isEnabled = true

                    when {
                        state.message.contains("email") -> {
                            val text = "อีเมลนี้ถูกใช้งานแล้ว"

                            binding.tilEmail.error = text
                            showToast(text)
                        }
                        state.message.contains("password") -> {
                            val text = "รหัสผ่านไม่ตรงกัน"

                            binding.tilConfirmPassword.error = text
                            showToast(text)
                        } else -> {
                            showToast(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun clearErrors() {
        binding.tilUsername.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
    }
}