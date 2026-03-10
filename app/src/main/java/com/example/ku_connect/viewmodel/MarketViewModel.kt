package com.example.ku_connect.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ku_connect.data.model.MarketItem
import com.example.ku_connect.data.repository.MarketRepository
import kotlinx.coroutines.launch

class MarketViewModel : ViewModel() {
    private val repo = MarketRepository()
    private val _items = MutableLiveData<UiState<List<MarketItem>>>()
    val items: LiveData<UiState<List<MarketItem>>> = _items
    private val _createState = MutableLiveData<UiState<String>>()
    val createState: LiveData<UiState<String>> = _createState

    fun loadItems() {
        _items.value = UiState.Loading

        viewModelScope.launch {
            val result = repo.getAllItems()

            _items.value = if (result.isSuccess) {
                UiState.Success(result.getOrThrow())
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "เกิดข้อผิดพลาด")
            }
        }
    }

    fun searchItems(query: String) {
        _items.value = UiState.Loading

        viewModelScope.launch {
            val result = repo.searchItems(query)

            _items.value = if (result.isSuccess) {

                UiState.Success(result.getOrThrow())
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "เกิดข้อผิดพลาด")
            }
        }
    }

    fun addItem(item: MarketItem, callback: (success: Boolean, msg: String) -> Unit) {
        _createState.value = UiState.Loading

        viewModelScope.launch {
            val result = repo.addItem(item)

            if (result.isSuccess) {
                _createState.value = UiState.Success(result.getOrThrow())

                callback(true, "เพิ่มร้านค้าสำเร็จ!")
            } else {
                _createState.value = UiState.Error(result.exceptionOrNull()?.message ?: "เกิดข้อผิดพลาด")
            }
        }
    }
}