package com.example.codemanager.data.repository

import com.example.codemanager.data.model.Warehouse
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class WarehouseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val warehousesCollection = db.collection("warehouses")

    suspend fun getAllWarehouses(): Result<List<Warehouse>> {
        return try {
            val snapshot = warehousesCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val warehouses = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Warehouse::class.java)?.copy(id = doc.id)
            }
            Result.success(warehouses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWarehouseById(warehouseId: String): Result<Warehouse> {
        return try {
            val doc = warehousesCollection.document(warehouseId).get().await()
            val warehouse = doc.toObject(Warehouse::class.java)?.copy(id = doc.id)
            if (warehouse != null) {
                Result.success(warehouse)
            } else {
                Result.failure(Exception("Almacén no encontrado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWarehousesByType(type: String): Result<List<Warehouse>> {
        return try {
            val snapshot = warehousesCollection
                .whereEqualTo("type", type)
                .orderBy("code", Query.Direction.ASCENDING)
                .get()
                .await()

            val warehouses = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Warehouse::class.java)?.copy(id = doc.id)
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
                .get()
                .await()

            if (!existingWarehouse.isEmpty) {
                return Result.failure(Exception("El código ${warehouse.code} ya existe"))
            }

            val docRef = warehousesCollection.add(warehouse.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWarehouse(warehouseId: String, warehouse: Warehouse): Result<Unit> {
        return try {
            // Verificar si el código ya existe en otro almacén
            val existingWarehouse = warehousesCollection
                .whereEqualTo("code", warehouse.code)
                .get()
                .await()

            if (!existingWarehouse.isEmpty && existingWarehouse.documents[0].id != warehouseId) {
                return Result.failure(Exception("El código ${warehouse.code} ya existe"))
            }

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

    suspend fun updateEnvironmentalConditions(
        warehouseId: String,
        temperature: Double?,
        humidity: Double?
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any?>()
            temperature?.let { updates["temperature"] = it }
            humidity?.let { updates["humidity"] = it }

            warehousesCollection.document(warehouseId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchWarehouses(query: String): Result<List<Warehouse>> {
        return try {
            val snapshot = warehousesCollection.get().await()
            val warehouses = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Warehouse::class.java)?.copy(id = doc.id)
            }.filter { warehouse ->
                warehouse.code.contains(query, ignoreCase = true) ||
                        warehouse.name.contains(query, ignoreCase = true) ||
                        warehouse.type.contains(query, ignoreCase = true)
            }
            Result.success(warehouses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWarehousesByLevel(levelNumber: Int): Result<List<Warehouse>> {
        return try {
            val snapshot = warehousesCollection
                .whereEqualTo("levelNumber", levelNumber)
                .orderBy("shelfNumber", Query.Direction.ASCENDING)
                .get()
                .await()

            val warehouses = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Warehouse::class.java)?.copy(id = doc.id)
            }
            Result.success(warehouses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}