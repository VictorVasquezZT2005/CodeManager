package com.example.codemanager.data.repository

import com.example.codemanager.data.model.Warehouse
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class WarehouseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val warehousesCollection = db.collection("warehouses")

    // --- LECTURA ---
    suspend fun getAllWarehouses(): Result<List<Warehouse>> {
        return try {
            val snapshot = warehousesCollection.get().await()
            val warehouses = snapshot.documents.mapNotNull { doc -> doc.toWarehouse() }
            Result.success(warehouses.sortedBy { it.name })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWarehousesByType(type: String): Result<List<Warehouse>> {
        return try {
            val snapshot = warehousesCollection
                .whereEqualTo("type", type)
                .get().await()
            val warehouses = snapshot.documents.mapNotNull { doc -> doc.toWarehouse() }
            Result.success(warehouses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- ESCRITURA ---
    suspend fun createWarehouse(warehouse: Warehouse): Result<String> {
        return try {
            val existing = warehousesCollection
                .whereEqualTo("code", warehouse.code)
                .whereEqualTo("type", warehouse.type)
                .get().await()

            if (!existing.isEmpty) {
                return Result.failure(Exception("El código ${warehouse.code} ya existe."))
            }

            warehousesCollection.document(warehouse.id).set(warehouse.toMap()).await()
            Result.success(warehouse.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- NUEVO: ESCRITURA MASIVA (BATCH) ---
    // Permite guardar una lista de almacenes en una sola operación de red
    suspend fun createBatchWarehouses(warehouses: List<Warehouse>): Result<Unit> {
        return try {
            val batch = db.batch() // Iniciamos un lote

            warehouses.forEach { warehouse ->
                val docRef = warehousesCollection.document(warehouse.id)
                batch.set(docRef, warehouse.toMap())
            }

            // Ejecutamos todas las escrituras al mismo tiempo
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // ----------------------------------------

    suspend fun updateWarehouse(warehouseId: String, warehouse: Warehouse): Result<Unit> {
        return try {
            warehousesCollection.document(warehouseId).set(warehouse.toMap()).await()
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

    // --- FUNCIÓN DE CONVERSIÓN (ROBUSTA) ---
    private fun com.google.firebase.firestore.DocumentSnapshot.toWarehouse(): Warehouse? {
        return try {
            val id = getString("id") ?: this.id
            val name = getString("name") ?: "Sin Nombre"
            val type = getString("type") ?: "estante"
            val code = getString("code") ?: "S/C"
            val levelNumber = getLong("levelNumber")?.toInt() ?: 1
            val itemNumber = getLong("itemNumber")?.toInt() ?: 1
            val createdBy = getString("createdBy") ?: "Desconocido"

            val createdAt = try {
                val longTime = getLong("createdAt")
                if (longTime != null) {
                    longTime
                } else {
                    val timestamp = getTimestamp("createdAt")
                    timestamp?.toDate()?.time ?: System.currentTimeMillis()
                }
            } catch (e: Exception) {
                System.currentTimeMillis()
            }

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
            e.printStackTrace()
            null
        }
    }
}