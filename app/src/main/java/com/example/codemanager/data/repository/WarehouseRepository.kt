// data/repository/WarehouseRepository.kt
package com.example.codemanager.data.repository

import com.example.codemanager.data.model.Warehouse
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class WarehouseRepository {

    private val db: FirebaseFirestore = Firebase.firestore
    private val warehousesCollection = db.collection("warehouses")

    private val _warehouses = MutableStateFlow<List<Warehouse>>(emptyList())
    val warehouses: StateFlow<List<Warehouse>> = _warehouses.asStateFlow()

    suspend fun loadWarehouses(): Result<Boolean> {
        return try {
            println("DEBUG: Iniciando carga desde Firestore...")

            val snapshot = warehousesCollection
                .orderBy("shelfNumber")
                .orderBy("levelNumber")
                .get()
                .await()

            println("DEBUG: Snapshots recibidos: ${snapshot.documents.size}")

            val warehousesList = snapshot.documents.mapNotNull { document ->
                try {
                    val warehouse = document.toObject(Warehouse::class.java)
                    if (warehouse != null) {
                        println("DEBUG: Almacén cargado - ID: ${warehouse.id}, Código: ${warehouse.code}")
                    }
                    warehouse
                } catch (e: Exception) {
                    println("ERROR convirtiendo documento ${document.id}: ${e.message}")
                    null
                }
            }

            println("DEBUG: Total de almacenes convertidos: ${warehousesList.size}")
            _warehouses.value = warehousesList
            Result.success(true)

        } catch (e: Exception) {
            println("ERROR crítico cargando almacenes: ${e.message}")
            e.printStackTrace()
            _warehouses.value = emptyList()
            Result.failure(e)
        }
    }

    suspend fun createWarehouse(
        shelfNumber: Int,
        levelNumber: Int,
        name: String,
        description: String = "",
        type: String,
        temperature: String? = null,
        humidity: String? = null,
        capacity: String? = null,
        createdBy: String = ""
    ): Result<Warehouse> {
        return try {
            // Verificar que no exista ya un almacén con el mismo estante y nivel
            val existingWarehouse = warehousesCollection
                .whereEqualTo("shelfNumber", shelfNumber)
                .whereEqualTo("levelNumber", levelNumber)
                .get()
                .await()

            if (!existingWarehouse.isEmpty) {
                return Result.failure(Exception("Ya existe un almacén en el estante $shelfNumber, nivel $levelNumber"))
            }

            // Generar código automático
            val shelfCode = shelfNumber.toString().padStart(2, '0')
            val levelCode = levelNumber.toString().padStart(2, '0')
            val fullCode = "$shelfCode$levelCode"

            val newWarehouse = Warehouse(
                id = UUID.randomUUID().toString(),
                code = fullCode,
                shelfNumber = shelfNumber,
                levelNumber = levelNumber,
                name = name,
                description = description,
                type = type,
                temperature = temperature,
                humidity = humidity,
                capacity = capacity,
                createdBy = createdBy,
                createdAt = System.currentTimeMillis()
            )

            println("DEBUG: Creando nuevo almacén: ${newWarehouse.code}")

            // Guardar en Firestore
            warehousesCollection.document(newWarehouse.id)
                .set(newWarehouse)
                .await()

            println("DEBUG: Almacén creado exitosamente")

            // Recargar la lista completa
            loadWarehouses()

            Result.success(newWarehouse)

        } catch (e: Exception) {
            println("ERROR creando almacén: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteWarehouse(warehouseId: String): Result<Boolean> {
        return try {
            warehousesCollection.document(warehouseId).delete().await()
            loadWarehouses()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWarehouseById(warehouseId: String): Warehouse? {
        return try {
            val document = warehousesCollection.document(warehouseId).get().await()
            document.toObject(Warehouse::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun testConnection(): Boolean {
        return try {
            warehousesCollection.limit(1).get().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}