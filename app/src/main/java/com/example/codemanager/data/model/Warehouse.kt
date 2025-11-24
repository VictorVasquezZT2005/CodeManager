package com.example.codemanager.data.model

data class Warehouse(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val type: String = "",
    val levelNumber: Int = 1, // 1-10
    val itemNumber: Int = 1, // 1-30 (Esto es el número de Estante)
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

            // --- CORRECCIÓN AQUÍ ---
            // Antes: return "$levelStr$itemStr"
            // Ahora: Estante (item) va primero, luego el Nivel
            return "$itemStr$levelStr"
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
        // También invertí el texto aquí para que coincida con tu lógica visual: Estante X - Nivel Y
        return "${getTypeDisplayName(type)} ${itemNumber.toString().padStart(2, '0')} - Nivel ${levelNumber.toString().padStart(2, '0')}"
    }

    fun getDisplayCode(): String {
        return "${getTypeDisplayName(type)} $code" // Ej: "Estante 0110"
    }
}