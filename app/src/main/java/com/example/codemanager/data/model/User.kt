package com.example.codemanager.data.model

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val rol: String = "user" // Asegúrate de que sea "rol" no "role"
)