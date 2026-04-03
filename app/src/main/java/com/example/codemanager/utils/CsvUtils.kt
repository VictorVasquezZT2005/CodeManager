package com.example.codemanager.utils

import android.content.Context
import android.net.Uri
import com.example.codemanager.data.model.Category
import com.example.codemanager.data.model.Code
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object CsvUtils {

    // --- CATEGORÍAS (Exportar) ---
    fun exportCategoriesToCsv(categories: List<Category>): String {
        val header = "CODIGO,NOMBRE,TIPO,SECUENCIA,FECHA\n"
        val sb = StringBuilder(header)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        categories.forEach {
            val date = if (it.createdAt > 0) dateFormat.format(Date(it.createdAt)) else "-"
            val nameSanitized = it.name.replace(",", " ")
            sb.append("${it.code},${nameSanitized},${it.type},${it.sequence},$date\n")
        }
        return sb.toString()
    }

    // --- CATEGORÍAS (Importar) ---
    fun parseCategoriesFromCsv(context: Context, uri: Uri): List<String> {
        val namesToCreate = mutableListOf<String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String? = reader.readLine() // Saltar cabecera

                while (reader.readLine().also { line = it } != null) {
                    val tokens = line!!.split(",")
                    if (tokens.size >= 2) {
                        val name = tokens[1].trim()
                        if (name.isNotEmpty()) namesToCreate.add(name)
                    } else if (tokens.isNotEmpty()) {
                        val name = tokens[0].trim()
                        if (name.isNotEmpty()) namesToCreate.add(name)
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return namesToCreate
    }

    // --- CÓDIGOS (Exportar) ---
    fun exportCodesToCsv(codes: List<Code>): String {
        val header = "CODIGO,DESCRIPCION,PREFIJO,CREADO_POR,FECHA\n"
        val sb = StringBuilder(header)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        codes.forEach {
            val date = if (it.createdAt > 0) dateFormat.format(Date(it.createdAt)) else "-"
            val descSanitized = it.description.replace(",", " ").replace("\n", " ")
            sb.append("${it.code},${descSanitized},${it.prefix},${it.createdBy},$date\n")
        }
        return sb.toString()
    }

    // --- CÓDIGOS (Importar - NUEVO) ---
    fun parseCodesFromCsv(context: Context, uri: Uri): List<Code> {
        val codesToImport = mutableListOf<Code>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String? = reader.readLine() // Saltar cabecera

                while (reader.readLine().also { line = it } != null) {
                    val tokens = line!!.split(",")
                    // Esperamos formato: CODIGO, DESCRIPCION, PREFIJO, CREADO_POR, ...
                    if (tokens.size >= 2) {
                        val fullCode = tokens[0].trim()
                        val description = tokens[1].trim().uppercase() // Regla de mayúsculas
                        val prefixHint = if (tokens.size > 2) tokens[2].trim() else ""
                        val createdBy = if (tokens.size > 3) tokens[3].trim() else "Importado"

                        // Lógica para reconstruir los campos internos según el formato del código
                        val parts = fullCode.split("-")
                        val newCode = if (parts.size == 4) {
                            // Es compuesto: 00-01-0101-0005 (Root-Cat-Ware-Seq)
                            Code(
                                id = UUID.randomUUID().toString(),
                                code = fullCode,
                                description = description,
                                prefix = prefixHint, // MED o DESC
                                rootPrefix = parts[0],
                                categoryCode = parts[1],
                                warehouseCode = parts[2],
                                sequence = parts[3].toIntOrNull() ?: 0,
                                createdBy = createdBy
                            )
                        } else if (parts.size == 2) {
                            // Es simple: 62-00001 (Prefix-Seq)
                            Code(
                                id = UUID.randomUUID().toString(),
                                code = fullCode,
                                description = description,
                                prefix = parts[0], // 62 o 70
                                rootPrefix = parts[0],
                                sequence = parts[1].toIntOrNull() ?: 0,
                                createdBy = createdBy
                            )
                        } else {
                            null // Formato desconocido, saltar
                        }

                        if (newCode != null) {
                            codesToImport.add(newCode)
                        }
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return codesToImport
    }
}