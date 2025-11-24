package com.example.codemanager.data.model

// ---------------------------------------------------------------------------
// 1. CODE: El objeto principal que guardamos en la colección 'codes'
// ---------------------------------------------------------------------------
data class Code(
    val id: String = "",
    val code: String = "",
    val prefix: String = "", // "62", "70", "MED", "DESC"
    val sequence: Int = 0,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)

// ---------------------------------------------------------------------------
// 2. THERAPEUTIC GROUP: Mapeado a la colección 'groups' de Firestore
// ---------------------------------------------------------------------------
// Unificamos 'Group' y 'TherapeuticGroup' en esta clase.
// El ViewModel espera 'TherapeuticGroup' para los Dropdowns.
data class TherapeuticGroup(
    val id: String = "",
    val code: String = "",     // El código "01", "02"...
    val name: String = "",     // Nombre visible
    val sequence: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

// ---------------------------------------------------------------------------
// 3. WAREHOUSE: Mapeado a la colección 'warehouses'
// ---------------------------------------------------------------------------
// Esta es la versión COMPLETA con tu lógica de negocio (item primero, luego nivel)
data class Warehouse(
    val id: String = "",
    val code: String = "",      // Ej: "0102" (Estante 01, Nivel 02)
    val name: String = "",
    val type: String = "",      // "estante", "refrigerador"
    val levelNumber: Int = 1,   // 1-10
    val itemNumber: Int = 1,    // 1-30
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    // Lógica estática para generación y formateo
    companion object {
        const val MAX_LEVELS = 10
        const val MAX_ITEMS_PER_LEVEL = 30
        val WAREHOUSE_TYPES = listOf("estante", "refrigerador")

        // Genera el código interno: Estante (Item) + Nivel
        // Ej: Item 1, Nivel 2 -> "0102"
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

    // Convierte el objeto a Mapa para guardar en Firestore (si lo necesitas manualmente)
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

    // Devuelve un string legible: "Estante 01 - Nivel 02"
    fun getFormattedLocation(): String {
        return "${getTypeDisplayName(type)} ${itemNumber.toString().padStart(2, '0')} - Nivel ${levelNumber.toString().padStart(2, '0')}"
    }

    // Devuelve código display: "Estante 0102"
    fun getDisplayCode(): String {
        return "${getTypeDisplayName(type)} $code"
    }
}