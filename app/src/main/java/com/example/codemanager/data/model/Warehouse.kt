// data/model/Warehouse.kt
package com.example.codemanager.data.model

data class Warehouse(
    val id: String = "",
    val code: String = "",
    val shelfNumber: Int = 0,
    val levelNumber: Int = 0,
    val name: String = "",
    val description: String = "",
    val type: String = "",
    val temperature: String? = null,
    val humidity: String? = null,
    val capacity: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)