package com.svobnick.thisorthat.model

data class Comment(
    val commentId: Long,
    val userId: Long,
    val parentId: Long,
    val text: String
)