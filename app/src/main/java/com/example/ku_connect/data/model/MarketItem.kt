package com.example.ku_connect.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class MarketItem(
    @DocumentId
    val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val shopName: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val lineOpenChatUrl: String = "",
    @ServerTimestamp
    val createdAt: Date? = null
)