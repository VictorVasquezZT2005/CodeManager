package com.example.codemanager.data.repository

import com.example.codemanager.data.model.Warehouse // Asegúrate que importa el del Models.kt
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class WarehouseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val warehousesCollection = db.collection("warehouses")

    // --- LECTURA ---
    suspend fun getAllWarehouses(): Result<List<Warehouse>> {
        return try {
            val snapshot = warehousesCollection
                .get() // Traemos todo sin ordenar primero para evitar errores de índices faltantes
                .await()

            val warehouses = snapshot.documents.mapNotNull { doc ->
                doc.toWarehouse() // Usamos la función robusta de abajo
            }

            // Ordenamos en memoria
            Result.success(warehouses.sortedBy { it.name })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWarehousesByType(type: String): Result<List<Warehouse>> {
        return try {
            val snapshot = warehousesCollection
                .whereEqualTo("type", type)
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

    // --- ESCRITURA ---
    suspend fun createWarehouse(warehouse: Warehouse): Result<String> {
        return try {
            // Verificación simple de duplicados
            val existing = warehousesCollection
                .whereEqualTo("code", warehouse.code)
                .whereEqualTo("type", warehouse.type)
                .get().await()

            if (!existing.isEmpty) {
                return Result.failure(Exception("El código ${warehouse.code} ya existe."))
            }

            // Guardamos usando el mapa del modelo
            warehousesCollection.document(warehouse.id).set(warehouse.toMap()).await()
            Result.success(warehouse.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
    // Esta función intenta rescatar los datos aunque estén incompletos
    private fun com.google.firebase.firestore.DocumentSnapshot.toWarehouse(): Warehouse? {
        return try {
            // 1. ID: Si no hay campo 'id', usamos el ID del documento
            val id = getString("id") ?: this.id

            // 2. Nombre: Si es nulo, ponemos un texto por defecto
            val name = getString("name") ?: "Sin Nombre"

            // 3. Tipo: Si es nulo, asumimos 'estante' para que aparezca en la lista
            val type = getString("type") ?: "estante"

            // 4. Código: Si no tiene, ponemos S/C
            val code = getString("code") ?: "S/C"

            // 5. Números: Si fallan, ponemos 1
            val levelNumber = getLong("levelNumber")?.toInt() ?: 1
            val itemNumber = getLong("itemNumber")?.toInt() ?: 1

            val createdBy = getString("createdBy") ?: "Desconocido"

            // 6. Fecha: Soporta formato viejo (Timestamp) y nuevo (Long)
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