package com.example.codemanager.data.repository

import com.example.codemanager.data.model.Code
import com.example.codemanager.data.model.Category
import com.example.codemanager.data.model.Warehouse
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
    private val groupsCollection = db.collection("groups")
    private val warehousesCollection = db.collection("warehouses")

    private val _codes = MutableStateFlow<List<Code>>(emptyList())
    val codes: StateFlow<List<Code>> = _codes.asStateFlow()

    suspend fun loadCodes() {
        try {
            val snapshot = codesCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            _codes.value = snapshot.toObjects(Code::class.java)
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun getCategories(): List<Category> {
        return try {
            val snapshot = groupsCollection.orderBy("code").get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("name") ?: "Sin Nombre"
                    val code = doc.getString("code") ?: "00"
                    val sequence = doc.getLong("sequence")?.toInt() ?: 0
                    val type = doc.getString("type") ?: "MED"
                    Category(id = id, name = name, code = code, sequence = sequence, type = type)
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

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
                        doc.getLong("createdAt") ?: doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
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

    // --- NUEVA FUNCIÓN: ACTUALIZAR CÓDIGO (Para editar descripción) ---
    suspend fun updateCode(code: Code): Result<Boolean> {
        return try {
            // Sobrescribe el documento con el objeto Code actualizado (mismo ID)
            codesCollection.document(code.id).set(code).await()
            loadCodes() // Recargamos la lista localmente
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- CÓDIGO SIMPLE (62 / 70) ---
    suspend fun generateStandardCode(prefix: String, description: String, createdBy: String): Result<Code> {
        return try {
            val nextSequence = getNextSequence(prefix)
            val formattedSequence = nextSequence.toString().padStart(5, '0')
            val fullCode = "$prefix-$formattedSequence"

            saveCode(
                fullCode = fullCode,
                rootPrefix = prefix,
                categoryCode = "",
                warehouseCode = "",
                internalPrefix = prefix,
                sequence = nextSequence,
                description = description,
                createdBy = createdBy
            )
        } catch (e: Exception) { Result.failure(e) }
    }

    // --- CÓDIGO COMPUESTO ---
    suspend fun generateCompositeCode(
        rootPrefix: String,
        category: Category,
        warehouse: Warehouse,
        description: String,
        createdBy: String,
        internalPrefix: String
    ): Result<Code> {
        return try {
            val sequenceKey = "$rootPrefix-${category.code}"
            val nextSequence = getNextSequence(sequenceKey)
            val formattedCategory = category.code.padStart(2, '0')
            val formattedWarehouse = warehouse.code
            val formattedSequence = nextSequence.toString().padStart(4, '0')
            val fullCode = "$rootPrefix-$formattedCategory-$formattedWarehouse-$formattedSequence"

            saveCode(
                fullCode = fullCode,
                rootPrefix = rootPrefix,
                categoryCode = formattedCategory,
                warehouseCode = formattedWarehouse,
                internalPrefix = internalPrefix,
                sequence = nextSequence,
                description = description,
                createdBy = createdBy
            )
        } catch (e: Exception) { Result.failure(e) }
    }

    // --- IMPORTAR CÓDIGO ---
    suspend fun importCode(code: Code): Result<Boolean> {
        return try {
            // Guardamos o sobrescribimos el código
            codesCollection.document(code.id).set(code).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- ACTUALIZAR SECUENCIA (CORRECCIÓN DE IMPORTACIÓN) ---
    suspend fun updateSequenceMax(sequenceKey: String, newMaxSequence: Int) {
        val docRef = sequencesCollection.document(sequenceKey)
        try {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentSequence = snapshot.getLong("lastSequence")?.toInt() ?: 0

                // Solo actualizamos si la nueva secuencia es MAYOR a la actual
                if (newMaxSequence > currentSequence) {
                    transaction.set(docRef, mapOf("lastSequence" to newMaxSequence))
                }
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun saveCode(
        fullCode: String,
        rootPrefix: String,
        categoryCode: String,
        warehouseCode: String,
        internalPrefix: String,
        sequence: Int,
        description: String,
        createdBy: String
    ): Result<Code> {
        val newCode = Code(
            id = UUID.randomUUID().toString(),
            code = fullCode,
            rootPrefix = rootPrefix,
            categoryCode = categoryCode,
            warehouseCode = warehouseCode,
            prefix = internalPrefix,
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