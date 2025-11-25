package com.example.codemanager.data.repository

import com.example.codemanager.data.model.Category
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CategoryRepository {

    private val db: FirebaseFirestore = Firebase.firestore
    private val categoriesCollection = db.collection("groups")
    private val sequencesCollection = db.collection("sequences")

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    // Cargar grupos filtrando por tipo ("MED" o "DESC")
    suspend fun loadCategoriesByType(type: String) {
        try {
            // --- CORRECCIÓN IMPORTANTE ---
            // 1. Quitamos .orderBy("sequence") de la consulta para evitar error de "Falta Índice" en Firestore.
            // 2. Hacemos lectura robusta manual por si faltan campos en documentos viejos.

            val snapshot = categoriesCollection
                .whereEqualTo("type", type)
                .get()
                .await()

            val categoriesList = snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("name") ?: "Sin Nombre"
                    val code = doc.getString("code") ?: "00"
                    val sequence = doc.getLong("sequence")?.toInt() ?: 0
                    // Si el documento no tiene type, asumimos que es del tipo que estamos buscando
                    val docType = doc.getString("type") ?: type
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                    Category(
                        id = id,
                        name = name,
                        code = code,
                        sequence = sequence,
                        type = docType,
                        createdAt = createdAt
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // 3. Ordenamos la lista en memoria (Kotlin) en lugar de en la base de datos
            _categories.value = categoriesList.sortedBy { it.sequence }

        } catch (e: Exception) {
            e.printStackTrace() // Revisa el Logcat si sigue fallando
            _categories.value = emptyList()
        }
    }

    suspend fun createCategory(name: String, type: String): Result<Category> {
        return try {
            val sequenceKey = "groups_$type"
            val nextSequence = getNextSequence(sequenceKey)
            val fullCode = nextSequence.toString().padStart(2, '0')

            val newCategory = Category(
                id = UUID.randomUUID().toString(),
                code = fullCode,
                sequence = nextSequence,
                name = name,
                type = type
            )

            categoriesCollection.document(newCategory.id).set(newCategory).await()
            loadCategoriesByType(type)
            Result.success(newCategory)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateCategory(id: String, name: String, currentType: String): Result<Boolean> {
        return try {
            categoriesCollection.document(id).update("name", name).await()
            loadCategoriesByType(currentType)
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteCategory(id: String, currentType: String): Result<Boolean> {
        return try {
            categoriesCollection.document(id).delete().await()
            loadCategoriesByType(currentType)
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun getNextSequence(prefix: String): Int {
        val docRef = sequencesCollection.document(prefix)
        return db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val newSequence = if (snapshot.exists()) (snapshot.getLong("lastSequence") ?: 0) + 1 else 1
            transaction.set(docRef, mapOf("lastSequence" to newSequence))
            newSequence
        }.await().toInt()
    }
}