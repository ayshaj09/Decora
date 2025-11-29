package com.example.decora
//data class
data class Message(
    val id: Int,
    val senderId: Int,
    val receiverId: Int,
    val messageText: String,
    val createdAt: String
)