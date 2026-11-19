package dev.devfest.agent.domain.model

import java.util.UUID

/** Roles del protocolo de chat con Function Calling. */
enum class Role { USER, ASSISTANT, TOOL }

/**
 * Unidad de la conversación. Es agnóstica del proveedor (Gemini, OpenAI, local):
 * los mappers de :data la traducen al formato de cada API.
 *
 * - USER:       texto del usuario.
 * - ASSISTANT:  respuesta final del modelo, o un "paso de razonamiento" que pide
 *               ejecutar herramientas ([toolCalls] no vacío).
 * - TOOL:       resultado de una herramienta, que se reinyecta al modelo.
 */
data class AgentMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val content: String,
    val toolCalls: List<ToolCall> = emptyList(),
    val toolResult: ToolResult? = null,
    val timestampMillis: Long = System.currentTimeMillis(),
) {
    val isToolCallRequest: Boolean get() = role == Role.ASSISTANT && toolCalls.isNotEmpty()

    companion object {
        fun user(text: String) = AgentMessage(role = Role.USER, content = text)

        fun assistant(text: String, toolCalls: List<ToolCall> = emptyList()) =
            AgentMessage(role = Role.ASSISTANT, content = text, toolCalls = toolCalls)

        fun tool(result: ToolResult) =
            AgentMessage(role = Role.TOOL, content = result.output, toolResult = result)
    }
}
