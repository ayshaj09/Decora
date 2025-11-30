package com.example.decora

data class Board(
    val id: Int,
    val title: String,
    val pinCount: String = "0 pins" // Default for now
)