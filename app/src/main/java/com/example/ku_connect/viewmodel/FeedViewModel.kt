package com.example.ku_connect.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ku_connect.data.model.Comment
import com.example.ku_connect.data.model.Post
import com.example.ku_connect.data.repository.PostRepository
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {
    private val repo = PostRepository()
    private val _popularPosts = MutableLiveData<UiState<List<Post>>>()
    val popularPosts: LiveData<UiState<List<Post>>> = _popularPosts
    private val _allPosts = MutableLiveData<UiState<List<Post>>>()
    val allPosts: LiveData<UiState<List<Post>>> = _allPosts
    private val _comments = MutableLiveData<UiState<List<Comment>>>()
    val comments: LiveData<UiState<List<Comment>>> = _comments
    private val _createState = MutableLiveData<UiState<String>>()
    val createState: LiveData<UiState<String>> = _createState

    fun loadPopularPosts() {
        _popularPosts.value = UiState.Loading

        viewModelScope.launch {
            val result = repo.getPopularPosts()

            _popularPosts.value = if (result.isSuccess) {
                UiState.Success(result.getOrThrow())
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "เกิดข้อผิดพลาด")
            }
        }
    }

    fun loadAllPosts() {
        _allPosts.value = UiState.Loading

        viewModelScope.launch {
            val result = repo.getAllPosts()

            _allPosts.value = if (result.isSuccess) {
                UiState.Success(result.getOrThrow())
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "เกิดข้อผิดพลาด")
            }
        }
    }

    fun searchPosts(query: String) {
        _allPosts.value = UiState.Loading

        viewModelScope.launch {
            val result = repo.searchPosts(query)

            _allPosts.value = if (result.isSuccess) {
                UiState.Success(result.getOrThrow())
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "เกิดข้อผิดพลาด")
            }
        }
    }

    fun createPost(post: Post, callback: (Boolean, String) -> Unit) {
        _createState.value = UiState.Loading

        viewModelScope.launch {
            val result = repo.createPost(post)

            if (result.isSuccess) {
                _createState.value = UiState.Success(result.getOrThrow())

                callback(true, "สร้างกระทู้สำเร็จ!")
            } else {
                _createState.value = UiState.Error(result.exceptionOrNull()?.message ?: "เกิดข้อผิดพลาด")
            }
        }
    }

    fun toggleLike(postId: String, userId: String, liked: Boolean) {
        viewModelScope.launch {
            repo.toggleLike(postId, userId, liked)
        }
    }

    fun loadComments(postId: String) {
        _comments.value = UiState.Loading

        viewModelScope.launch {
            val result = repo.getComments(postId)

            _comments.value = if (result.isSuccess) {
                UiState.Success(result.getOrThrow())
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "เกิดข้อผิดพลาด")
            }
        }
    }

    fun addComment(postId: String, comment: Comment) {
        viewModelScope.launch {
            repo.addComment(postId, comment)

            loadComments(postId)
        }
    }
}