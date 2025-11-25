package com.example.codemanager.data.model

// --- CODE y WAREHOUSE se quedan igual ---
data class Code(
    val id: String = "",
    val code: String = "",
    val prefix: String = "",
    val sequence: Int = 0,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)

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
    // ... (El companion object de Warehouse se queda igual) ...
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

    // ... (Métodos de Warehouse siguen igual) ...
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id, "code" to code, "name" to name, "type" to type,
        "levelNumber" to levelNumber, "itemNumber" to itemNumber,
        "createdBy" to createdBy, "createdAt" to createdAt
    )
    fun getFormattedLocation(): String = "${getTypeDisplayName(type)} ${itemNumber.toString().padStart(2, '0')} - Nivel ${levelNumber.toString().padStart(2, '0')}"
    fun getDisplayCode(): String = "${getTypeDisplayName(type)} $code"
}

// --- CAMBIO PRINCIPAL AQUÍ ---
data class Category(
    val id: String = "",
    val code: String = "",      // Ej: "01", "02"
    val name: String = "",      // Ej: "Analgésicos" o "Jeringas"
    val sequence: Int = 0,
    val type: String = "MED",   // "MED" (Medicamentos) o "DESC" (Descartables)
    val createdAt: Long = System.currentTimeMillis()
)