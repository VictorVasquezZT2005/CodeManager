package com.example.codemanager.data.repository

import com.example.codemanager.data.model.Warehouse
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
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
                .orderBy("id") // Puedes cambiar esto por "createdAt" si quieres orden cronológico
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
            // Verificar si el código ya existe para ese tipo
            val existingWarehouse = warehousesCollection
                .whereEqualTo("code", warehouse.code)
                .whereEqualTo("type", warehouse.type)
                .get()
                .await()

            if (!existingWarehouse.isEmpty) {
                return Result.failure(Exception("El código ${warehouse.code} ya existe para ${Warehouse.getTypeDisplayName(warehouse.type)}"))
            }

            val warehouseData = warehouse.toMap().toMutableMap().apply {
                put("createdAt", Timestamp.now()) // Se guarda como Timestamp
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
                // Al actualizar, mantenemos la fecha de creación original si es posible,
                // o actualizamos si esa es tu lógica. Aquí he dejado tu lógica original:
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

    // --- FUNCIÓN CORREGIDA ---
    private fun com.google.firebase.firestore.DocumentSnapshot.toWarehouse(): Warehouse? {
        return try {
            val id = getString("id") ?: return null
            val code = getString("code") ?: return null
            val name = getString("name") ?: return null
            val type = getString("type") ?: return null
            val levelNumber = getLong("levelNumber")?.toInt() ?: 1
            val itemNumber = getLong("itemNumber")?.toInt() ?: 1
            val createdBy = getString("createdBy") ?: ""

            // CORRECCIÓN: Leer como Timestamp de Firebase y convertir a Long (milisegundos)
            val timestamp = getTimestamp("createdAt")
            val createdAt = timestamp?.toDate()?.time ?: System.currentTimeMillis()

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
            e.printStackTrace() // Útil para ver errores en Logcat
            null
        }
    }
}