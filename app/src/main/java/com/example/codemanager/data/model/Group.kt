// data/model/Group.kt
package com.example.codemanager.data.model

data class Group(
    val id: String = "",
    val code: String = "",
    val sequence: Int = 0,
    val name: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)