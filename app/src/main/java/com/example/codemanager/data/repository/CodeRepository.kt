// data/repository/CodeRepository.kt
package com.example.codemanager.data.repository

import com.example.codemanager.data.model.Code
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CodeRepository {

    private val db: FirebaseFirestore = Firebase.firestore
    private val codesCollection = db.collection("codes")
    private val sequencesCollection = db.collection("sequences")

    private val _codes = MutableStateFlow<List<Code>>(emptyList())
    val codes: StateFlow<List<Code>> = _codes.asStateFlow()

    suspend fun generateCode(prefix: String, description: String = "", createdBy: String = ""): Result<Code> {
        return try {
            // Obtener el próximo número de secuencia para este prefijo desde Firestore
            val nextSequence = getNextSequence(prefix)

            // Formatear el código: "62-00001"
            val formattedSequence = nextSequence.toString().padStart(5, '0')
            val fullCode = "$prefix-$formattedSequence"

            val newCode = Code(
                id = UUID.randomUUID().toString(),
                code = fullCode,
                prefix = prefix,
                sequence = nextSequence,
                description = description,
                createdBy = createdBy
            )

            // Guardar en Firestore
            codesCollection.document(newCode.id)
                .set(newCode)
                .await()

            // Actualizar la lista local
            loadCodes()

            Result.success(newCode)
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

                // Actualizar la secuencia en Firestore
                sequencesCollection.document(prefix)
                    .set(mapOf("lastSequence" to nextSequence))
                    .await()

                nextSequence
            } else {
                // Crear nuevo documento de secuencia
                sequencesCollection.document(prefix)
                    .set(mapOf("lastSequence" to 1))
                    .await()
                1
            }
        } catch (e: Exception) {
            throw Exception("Error al obtener secuencia: ${e.message}")
        }
    }

    suspend fun loadCodes() {
        try {
            val snapshot = codesCollection
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val codesList = snapshot.documents.map { document ->
                document.toObject(Code::class.java) ?: Code()
            }

            _codes.value = codesList
        } catch (e: Exception) {
            // Manejar error, puedes loggearlo o mostrar un mensaje
            println("Error cargando códigos: ${e.message}")
        }
    }

    suspend fun getCodesByPrefix(prefix: String): List<Code> {
        return try {
            val snapshot = codesCollection
                .whereEqualTo("prefix", prefix)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.map { document ->
                document.toObject(Code::class.java) ?: Code()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteCode(codeId: String): Result<Boolean> {
        return try {
            codesCollection.document(codeId).delete().await()
            loadCodes() // Recargar la lista después de eliminar
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCodeById(codeId: String): Code? {
        return try {
            val document = codesCollection.document(codeId).get().await()
            document.toObject(Code::class.java)
        } catch (e: Exception) {
            null
        }
    }
}