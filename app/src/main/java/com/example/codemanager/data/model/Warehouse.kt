package com.example.codemanager.data.model

data class Warehouse(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val type: String = "", // "estante", "bodega", "refrigerado", etc.
    val description: String = "",
    val capacity: String = "",
    val levelNumber: Int = 0,
    val shelfNumber: Int = 0,
    val temperature: Double? = null,
    val humidity: Double? = null,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "code" to code,
            "name" to name,
            "type" to type,
            "description" to description,
            "capacity" to capacity,
            "levelNumber" to levelNumber,
            "shelfNumber" to shelfNumber,
            "temperature" to temperature,
            "humidity" to humidity,
            "createdBy" to createdBy,
            "createdAt" to createdAt
        )
    }

    fun getFormattedLocation(): String {
        return "Nivel $levelNumber - Estante $shelfNumber"
    }

    fun hasEnvironmentalControl(): Boolean {
        return temperature != null || humidity != null
    }
}