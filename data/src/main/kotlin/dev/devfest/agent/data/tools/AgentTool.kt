package dev.devfest.agent.data.tools

import dev.devfest.agent.domain.model.ToolDefinition
import kotlinx.serialization.json.JsonObject

/**
 * Una herramienta nativa del dispositivo/app que el agente puede invocar.
 * Agregar una tool nueva = 1 clase + 1 línea @Binds @IntoSet en ToolsModule.
 */
interface AgentTool {
    val definition: ToolDefinition

    /** Devuelve JSON: es lo que el LLM "lee" para razonar el siguiente paso. */
    suspend fun execute(arguments: Map<String, Any?>): JsonObject
}

internal fun Map<String, Any?>.requireString(key: String): String =
    (this[key] as? String)?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Falta el argumento '$key'")

internal fun Map<String, Any?>.requireDouble(key: String): Double =
    when (val value = this[key]) {
        is Number -> value.toDouble()
        is String -> value.replace(',', '.').toDoubleOrNull()
        else -> null
    } ?: throw IllegalArgumentException("Argumento numérico inválido '$key'")

internal fun Double.money(): Double = Math.round(this * 100) / 100.0
