package com.example.codemanager.data.model

// --- CODE ACTUALIZADO ---
// Se agregaron los campos estructurales (rootPrefix, categoryCode, warehouseCode)
// para que coincidan con la lógica del CodeRepository.
data class Code(
    val id: String = "",
    val code: String = "",
    val prefix: String = "",        // "MED", "DESC", "62", "70" (Filtros UI)

    // --- NUEVOS CAMPOS REQUERIDOS POR EL REPOSITORIO ---
    val rootPrefix: String = "",    // "00", "01"
    val categoryCode: String = "",  // "01"
    val warehouseCode: String = "", // "0101"
    // ---------------------------------------------------

    val sequence: Int = 0,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)

// --- WAREHOUSE (Igual a tu versión, con toMap incluido) ---
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

// --- CATEGORY (Igual a tu versión) ---
data class Category(
    val id: String = "",
    val code: String = "",      // Ej: "01", "02"
    val name: String = "",      // Ej: "Analgésicos"
    val sequence: Int = 0,
    val type: String = "MED",   // "MED" o "DESC"
    val createdAt: Long = System.currentTimeMillis()
)