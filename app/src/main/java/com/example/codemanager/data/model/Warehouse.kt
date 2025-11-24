package com.example.codemanager.data.model

data class Warehouse(
    val id: String = "",
    val code: String = "", // Generado automáticamente: {nivel}{numero}
    val name: String = "",
    val type: String = "", // Solo "estante" o "refrigerador"
    val levelNumber: Int = 1, // 1-10
    val itemNumber: Int = 1, // 1-30
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val MAX_LEVELS = 10
        const val MAX_ITEMS_PER_LEVEL = 30
        val WAREHOUSE_TYPES = listOf("estante", "refrigerador")

        fun generateCode(level: Int, itemNumber: Int): String {
            val levelStr = level.toString().padStart(2, '0')
            val itemStr = itemNumber.toString().padStart(2, '0')
            return "$levelStr$itemStr" // Solo números: "0101", "0115", "1001", etc.
        }

        fun getTypeDisplayName(type: String): String {
            return when (type) {
                "estante" -> "Estante"
                "refrigerador" -> "Refrigerador"
                else -> type
            }
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "code" to code,
            "name" to name,
            "type" to type,
            "levelNumber" to levelNumber,
            "itemNumber" to itemNumber,
            "createdBy" to createdBy,
            "createdAt" to createdAt
        )
    }

    fun getFormattedLocation(): String {
        return "Nivel ${levelNumber.toString().padStart(2, '0')} - ${getTypeDisplayName(type)} ${itemNumber.toString().padStart(2, '0')}"
    }

    fun getDisplayCode(): String {
        return "${getTypeDisplayName(type)} $code" // Ej: "Estante 0101"
    }
}