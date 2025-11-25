package com.example.codemanager.data.repository

import com.example.codemanager.data.model.Code
import com.example.codemanager.data.model.Category // <-- Usamos Category
import com.example.codemanager.data.model.Warehouse
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
    private val groupsCollection = db.collection("groups") // Aquí están las categorías
    private val warehousesCollection = db.collection("warehouses")

    private val _codes = MutableStateFlow<List<Code>>(emptyList())
    val codes: StateFlow<List<Code>> = _codes.asStateFlow()

    suspend fun loadCodes() {
        try {
            val snapshot = codesCollection
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
            _codes.value = snapshot.toObjects(Code::class.java)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // --- ACTUALIZADO: Lectura de Categorías (Antes Grupos) ---
    suspend fun getCategories(): List<Category> {
        return try {
            val snapshot = groupsCollection.orderBy("code").get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("name") ?: "Sin Nombre"
                    val code = doc.getString("code") ?: "00"
                    val sequence = doc.getLong("sequence")?.toInt() ?: 0
                    val type = doc.getString("type") ?: "MED" // Importante para el filtro

                    Category(id = id, name = name, code = code, sequence = sequence, type = type)
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    // --- Lectura robusta de Almacenes ---
    suspend fun getWarehouses(): List<Warehouse> {
        return try {
            val snapshot = warehousesCollection.orderBy("name").get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("name") ?: "Sin Nombre"
                    val code = doc.getString("code") ?: "S/C"
                    val type = doc.getString("type") ?: "estante"
                    val levelNumber = doc.getLong("levelNumber")?.toInt() ?: 1
                    val itemNumber = doc.getLong("itemNumber")?.toInt() ?: 1
                    val createdBy = doc.getString("createdBy") ?: ""

                    val createdAt = try {
                        doc.getLong("createdAt")
                            ?: doc.getTimestamp("createdAt")?.toDate()?.time
                            ?: System.currentTimeMillis()
                    } catch (e: Exception) { System.currentTimeMillis() }

                    Warehouse(
                        id = id, code = code, name = name, type = type,
                        levelNumber = levelNumber, itemNumber = itemNumber,
                        createdBy = createdBy, createdAt = createdAt
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun deleteCode(codeId: String): Result<Boolean> {
        return try {
            codesCollection.document(codeId).delete().await()
            loadCodes()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    // Generar código simple (62 / 70)
    suspend fun generateStandardCode(prefix: String, description: String, createdBy: String): Result<Code> {
        return try {
            val nextSequence = getNextSequence(prefix)
            val formattedSequence = nextSequence.toString().padStart(5, '0')
            val fullCode = "$prefix-$formattedSequence"
            saveCode(fullCode, prefix, nextSequence, description, createdBy)
        } catch (e: Exception) { Result.failure(e) }
    }

    // Generar código compuesto (00 para Med, 01 para Desc)
    suspend fun generateCompositeCode(
        rootPrefix: String,
        category: Category, // Actualizado a Category
        warehouse: Warehouse,
        description: String,
        createdBy: String,
        internalPrefix: String
    ): Result<Code> {
        return try {
            // Secuencia única por combinación: "00-01-0102"
            val sequenceKey = "$rootPrefix-${category.code}-${warehouse.code}"
            val nextSequence = getNextSequence(sequenceKey)

            val formattedCategory = category.code.padStart(2, '0')
            val formattedWarehouse = warehouse.code
            val formattedSequence = nextSequence.toString().padStart(4, '0')

            // Armamos: 00-01-0102-0001
            val fullCode = "$rootPrefix-$formattedCategory-$formattedWarehouse-$formattedSequence"

            saveCode(fullCode, internalPrefix, nextSequence, description, createdBy)
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun saveCode(fullCode: String, prefix: String, sequence: Int, description: String, createdBy: String): Result<Code> {
        val newCode = Code(
            id = UUID.randomUUID().toString(),
            code = fullCode,
            prefix = prefix,
            sequence = sequence,
            description = description,
            createdBy = createdBy
        )
        codesCollection.document(newCode.id).set(newCode).await()
        loadCodes()
        return Result.success(newCode)
    }

    private suspend fun getNextSequence(docId: String): Int {
        val docRef = sequencesCollection.document(docId)
        return db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val newSequence = if (snapshot.exists()) {
                (snapshot.getLong("lastSequence") ?: 0) + 1
            } else {
                1
            }
            transaction.set(docRef, mapOf("lastSequence" to newSequence))
            newSequence
        }.await().toInt()
    }
}