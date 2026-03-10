package com.example.ku_connect.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Comment(
    @DocumentId
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorColor: String = "#4CAF50",
    val content: String = "",
    val imageUrl: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null
)