package dev.devfest.agent.domain.model

import java.util.UUID

enum class ParameterType { STRING, NUMBER, BOOLEAN }

data class ToolParameter(
    val name: String,
    val type: ParameterType,
    val description: String,
    val required: Boolean = true,
)

/**
 * Descripción de una herramienta que el LLM puede invocar.
 * Es el "contrato" que se envía al modelo como function declaration.
 *
 * @param requiresConfirmation si es true, el agente NO la ejecuta sin aprobación
 *        explícita del usuario (human-in-the-loop). Imprescindible en finanzas.
 */
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter> = emptyList(),
    val requiresConfirmation: Boolean = false,
)

/**
 * Solicitud del modelo para ejecutar una herramienta.
 *
 * @param arguments valores primitivos (String, Double, Boolean) ya decodificados.
 * @param providerMetadata datos opacos que algunos proveedores exigen devolver
 *        en el siguiente turno (p. ej. `thoughtSignature` en Gemini). El dominio
 *        no los interpreta, solo los conserva.
 */
data class ToolCall(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val arguments: Map<String, Any?> = emptyMap(),
    val providerMetadata: Map<String, String> = emptyMap(),
)

enum class ToolStatus { SUCCESS, ERROR, REJECTED }

/** Resultado de ejecutar una herramienta. [output] es JSON para que el LLM lo lea. */
data class ToolResult(
    val callId: String,
    val toolName: String,
    val output: String,
    val status: ToolStatus,
) {
    companion object {
        fun error(call: ToolCall, message: String) = ToolResult(
            callId = call.id,
            toolName = call.name,
            output = """{"error":"${message.replace("\"", "'")}"}""",
            status = ToolStatus.ERROR,
        )

        fun rejected(call: ToolCall) = ToolResult(
            callId = call.id,
            toolName = call.name,
            output = """{"error":"El usuario rechazó la ejecución de ${call.name}"}""",
            status = ToolStatus.REJECTED,
        )
    }
}
