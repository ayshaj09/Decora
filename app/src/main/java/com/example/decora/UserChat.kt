package com.example.decora

data class UserChat(
    val id: Int,
    val username: String,
    val profilePic: String?,
    val lastMessage: String?,
    val timestamp: String?
)