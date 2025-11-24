package com.example.codemanager.data.repository

import com.example.codemanager.data.model.Group
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GroupRepository {

    private val db: FirebaseFirestore = Firebase.firestore
    private val groupsCollection = db.collection("groups")
    private val sequencesCollection = db.collection("sequences")

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    // Parámetro createdBy eliminado
    suspend fun createGroup(name: String): Result<Group> {
        return try {
            val nextSequence = getNextSequence("groups")
            val fullCode = nextSequence.toString().padStart(2, '0')

            val newGroup = Group(
                id = UUID.randomUUID().toString(),
                code = fullCode,
                sequence = nextSequence,
                name = name
                // createdBy eliminado
            )

            groupsCollection.document(newGroup.id)
                .set(newGroup)
                .await()

            loadGroups()

            Result.success(newGroup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getNextSequence(prefix: String): Int {
        return try {
            val sequenceDoc = sequencesCollection.document(prefix).get().await()
            if (sequenceDoc.exists()) {
                val currentSequence = sequenceDoc.getLong("lastSequence")?.toInt() ?: 0
                val nextSequence = currentSequence + 1

                sequencesCollection.document(prefix)
                    .set(mapOf("lastSequence" to nextSequence))
                    .await()

                nextSequence
            } else {
                sequencesCollection.document(prefix)
                    .set(mapOf("lastSequence" to 1))
                    .await()
                1
            }
        } catch (e: Exception) {
            throw Exception("Error al obtener secuencia: ${e.message}")
        }
    }

    suspend fun loadGroups() {
        try {
            val snapshot = groupsCollection
                .orderBy("sequence", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .await()

            val groupsList = snapshot.documents.map { document ->
                document.toObject(Group::class.java) ?: Group()
            }

            _groups.value = groupsList
        } catch (e: Exception) {
            println("Error cargando grupos: ${e.message}")
        }
    }

    suspend fun deleteGroup(groupId: String): Result<Boolean> {
        return try {
            groupsCollection.document(groupId).delete().await()
            loadGroups()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGroup(groupId: String, name: String): Result<Boolean> {
        return try {
            groupsCollection.document(groupId)
                .update(
                    mapOf("name" to name)
                )
                .await()
            loadGroups()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}