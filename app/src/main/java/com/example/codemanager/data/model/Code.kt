// data/model/Code.kt
package com.example.codemanager.data.model

data class Code(
    val id: String,
    val code: String,
    val prefix: String,
    val sequence: Int,
    val description: String,
    val createdAt: Long = System.currentTimeMillis()
)