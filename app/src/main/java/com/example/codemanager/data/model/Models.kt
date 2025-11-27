package com.example.codemanager.data.model

// --- CODE (Actualizado con tus nuevos campos) ---
data class Code(
    val id: String = "",
    val code: String = "",
    val prefix: String = "",        // "MED", "DESC", "62", "70"
    val rootPrefix: String = "",    // "00", "01"
    val categoryCode: String = "",  // "01"
    val warehouseCode: String = "", // "0101"
    val sequence: Int = 0,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)

// --- WAREHOUSE ---
data class Warehouse(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val type: String = "",
    val levelNumber: Int = 1,
    val itemNumber: Int = 1,
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

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id, "code" to code, "name" to name, "type" to type,
        "levelNumber" to levelNumber, "itemNumber" to itemNumber,
        "createdBy" to createdBy, "createdAt" to createdAt
    )

    fun getFormattedLocation(): String = "${getTypeDisplayName(type)} ${itemNumber.toString().padStart(2, '0')} - Nivel ${levelNumber.toString().padStart(2, '0')}"
    fun getDisplayCode(): String = "${getTypeDisplayName(type)} $code"
}

// --- CATEGORY ---
data class Category(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val sequence: Int = 0,
    val type: String = "MED",
    val createdAt: Long = System.currentTimeMillis()
)