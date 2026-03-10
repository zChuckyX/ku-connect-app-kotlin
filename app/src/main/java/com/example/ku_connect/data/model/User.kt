package com.example.ku_connect.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    @DocumentId
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val profileColor: String = "#4CAF50",
    val postCount: Int = 0,
    val marketCount: Int = 0,
    @ServerTimestamp
    val createdAt: Date? = null
)