// data/model/Code.kt
package com.example.codemanager.data.model

import com.google.firebase.firestore.PropertyName

data class Code(
    val id: String = "",
    val code: String = "",
    val prefix: String = "",
    val sequence: Int = 0,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)