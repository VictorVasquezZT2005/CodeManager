// ui/groups/GroupsViewModelFactory.kt
package com.example.codemanager.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.codemanager.data.repository.GroupRepository

class GroupsViewModelFactory(
    private val groupRepository: GroupRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupsViewModel::class.java)) {
            return GroupsViewModel(groupRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}