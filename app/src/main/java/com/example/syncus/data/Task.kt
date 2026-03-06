package com.example.syncus.data

import com.google.firebase.Timestamp

data class Task(
    val id: String = "",
    val title: String = "",
    val done: Boolean = false,
    val createdAt: Timestamp? = null
)