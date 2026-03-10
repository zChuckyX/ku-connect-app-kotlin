package com.example.ku_connect.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ku_connect.data.model.User
import com.example.ku_connect.data.repository.AuthRepository
import com.example.ku_connect.util.Validator
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repo = AuthRepository()
    private val _loginState = MutableLiveData<UiState<User>>()
    val loginState: LiveData<UiState<User>> = _loginState
    private val _registerState = MutableLiveData<UiState<User>>()
    val registerState: LiveData<UiState<User>> = _registerState
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser
    val isLoggedIn: Boolean get() = repo.currentUserId != null

    fun login(email: String, password: String) {
        if (!Validator.isValidKuEmail(email)) {
            _loginState.value = UiState.Error("อีเมลต้องเป็น @ku.th เท่านั้น")
            return
        }

        if (password.isBlank()) {
            _loginState.value = UiState.Error("กรุณากรอกรหัสผ่าน")
            return
        }

        _loginState.value = UiState.Loading

        viewModelScope.launch {
            val result = repo.login(email.trim(), password)

            _loginState.value = if (result.isSuccess) {
                UiState.Success(result.getOrThrow())
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "เกิดข้อผิดพลาด")
            }
        }
    }

    fun register(username: String, email: String, password: String, confirmPassword: String) {
        if (!Validator.isValidUsername(username)) {
            _registerState.value = UiState.Error("ชื่อผู้ใช้ต้องมี 3-30 ตัวอักษร")
            return
        }

        if (!Validator.isValidKuEmail(email)) {
            _registerState.value = UiState.Error("อีเมลต้องเป็น @ku.th เท่านั้น")
            return
        }

        val pwResult = Validator.isValidPassword(password)

        if (pwResult != Validator.PasswordResult.Valid) {
            _registerState.value = UiState.Error(pwResult.message())
            return
        }

        if (password != confirmPassword) {
            _registerState.value = UiState.Error("รหัสผ่านไม่ตรงกัน")
            return
        }

        _registerState.value = UiState.Loading

        val profileColor = listOf("#C8E6C9","#BBDEFB","#F8BBD9","#FFF9C4","#E1BEE7","#FFE0B2").random()

        viewModelScope.launch {
            val result = repo.register(username.trim(), email.trim(), password, profileColor)

            _registerState.value = if (result.isSuccess) {
                UiState.Success(result.getOrThrow())
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "เกิดข้อผิดพลาด")
            }
        }
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            _currentUser.value = repo.getCurrentUser()
        }
    }

    fun logout() {
        repo.logout()
    }
}

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}