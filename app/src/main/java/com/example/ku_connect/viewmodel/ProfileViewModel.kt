package com.example.ku_connect.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ku_connect.data.model.Post
import com.example.ku_connect.data.model.User
import com.example.ku_connect.data.repository.AuthRepository
import com.example.ku_connect.data.repository.PostRepository
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val authRepo   = AuthRepository()

    private val postRepo = PostRepository()
    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user
    private val _postCount = MutableLiveData<Int>(0)
    val postCount: LiveData<Int> = _postCount
    private val _marketCount = MutableLiveData<Int>(0)
    val marketCount: LiveData<Int> = _marketCount
    private val _myPosts = MutableLiveData<UiState<List<Post>>>()
    val myPosts: LiveData<UiState<List<Post>>> = _myPosts

    fun loadProfile() {
        viewModelScope.launch {
            _user.value = authRepo.getCurrentUser()
            _postCount.value = _user.value?.postCount
            _marketCount.value = _user.value?.marketCount
        }
    }

    fun refreshMyPosts() {
        val uid = authRepo.currentUserId ?: return

        viewModelScope.launch {
            _myPosts.value = UiState.Loading

            val result = postRepo.getMyPosts(uid)

            _myPosts.value = if (result.isSuccess) {
                UiState.Success(result.getOrThrow())
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "โหลดโพสต์ไม่สำเร็จ")
            }
        }
    }

    fun updateUsername(username: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val profileColor = listOf("#C8E6C9","#BBDEFB","#F8BBD9","#FFF9C4","#E1BEE7","#FFE0B2").random()
            val result = authRepo.updateUsername(username, profileColor)

            if (result.isSuccess) {
                _user.value = authRepo.getCurrentUser()

                callback(true, "อัปเดตชื่อผู้ใช้สำเร็จ")
            } else {
                callback(false, result.exceptionOrNull()?.message ?: "เกิดข้อผิดพลาด")
            }
        }
    }

    fun deletePost(postId: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = postRepo.deletePost(postId, authRepo.currentUserId!!)

            if (result.isSuccess) {
                callback(true, "ลบโพสต์สำเร็จ")

                refreshMyPosts()
            } else {
                callback(false, result.exceptionOrNull()?.message ?: "เกิดข้อผิดพลาด")
            }
        }
    }

    fun updatePost(postId: String, title: String, content: String, callback: (Boolean, String) -> Unit) {
        if (title.isBlank() || content.isBlank()) {
            callback(false, "กรุณากรอกหัวข้อและเนื้อหา")

            return
        }

        viewModelScope.launch {
            val result = postRepo.updatePost(postId, title, content)

            if (result.isSuccess) {
                callback(true, "แก้ไขโพสต์สำเร็จ")

                refreshMyPosts()
            } else {
                callback(false, result.exceptionOrNull()?.message ?: "เกิดข้อผิดพลาด")
            }
        }
    }
}