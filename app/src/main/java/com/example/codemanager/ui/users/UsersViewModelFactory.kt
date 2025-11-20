package com.example.codemanager.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.codemanager.data.repository.AuthRepository

class UsersViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UsersViewModel::class.java)) {
            return UsersViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}