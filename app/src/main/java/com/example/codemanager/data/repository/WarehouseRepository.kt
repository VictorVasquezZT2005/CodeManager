package com.example.codemanager.data.repository

import com.example.codemanager.data.model.Warehouse
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class WarehouseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val warehousesCollection = db.collection("warehouses")

    fun generateWarehouseId(): String {
        // Generar ID único usando timestamp + random
        return "WH_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    suspend fun getAllWarehouses(): Result<List<Warehouse>> {
        return try {
            val snapshot = warehousesCollection
                .orderBy("id")
                .get()
                .await()

            val warehouses = snapshot.documents.mapNotNull { doc ->
                doc.toWarehouse()
            }

            Result.success(warehouses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWarehousesByType(type: String): Result<List<Warehouse>> {
        return try {
            val snapshot = warehousesCollection
                .whereEqualTo("type", type)
                .orderBy("levelNumber")
                .orderBy("itemNumber")
                .get()
                .await()

            val warehouses = snapshot.documents.mapNotNull { doc ->
                doc.toWarehouse()
            }
            Result.success(warehouses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createWarehouse(warehouse: Warehouse): Result<String> {
        return try {
            // Verificar si el código ya existe
            val existingWarehouse = warehousesCollection
                .whereEqualTo("code", warehouse.code)
                .whereEqualTo("type", warehouse.type)
                .get()
                .await()

            if (!existingWarehouse.isEmpty) {
                return Result.failure(Exception("El código ${warehouse.code} ya existe para ${Warehouse.getTypeDisplayName(warehouse.type)}"))
            }

            val warehouseData = warehouse.toMap().toMutableMap().apply {
                put("createdAt", Timestamp.now())
            }

            // Usar el ID del warehouse como ID del documento
            warehousesCollection.document(warehouse.id).set(warehouseData).await()
            Result.success(warehouse.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWarehouse(warehouseId: String, warehouse: Warehouse): Result<Unit> {
        return try {
            val warehouseData = warehouse.toMap().toMutableMap().apply {
                put("createdAt", Timestamp.now())
            }

            warehousesCollection.document(warehouseId).set(warehouseData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteWarehouse(warehouseId: String): Result<Unit> {
        return try {
            warehousesCollection.document(warehouseId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toWarehouse(): Warehouse? {
        return try {
            val id = getString("id") ?: return null
            val code = getString("code") ?: return null
            val name = getString("name") ?: return null
            val type = getString("type") ?: return null
            val levelNumber = getLong("levelNumber")?.toInt() ?: 1
            val itemNumber = getLong("itemNumber")?.toInt() ?: 1
            val createdBy = getString("createdBy") ?: ""
            val createdAt = getLong("createdAt") ?: System.currentTimeMillis()

            Warehouse(
                id = id,
                code = code,
                name = name,
                type = type,
                levelNumber = levelNumber,
                itemNumber = itemNumber,
                createdBy = createdBy,
                createdAt = createdAt
            )
        } catch (e: Exception) {
            null
        }
    }
}