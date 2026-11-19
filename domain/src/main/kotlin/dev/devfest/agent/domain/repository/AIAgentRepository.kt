package dev.devfest.agent.domain.repository

import dev.devfest.agent.domain.model.AgentMessage
import dev.devfest.agent.domain.model.LlmOutput
import dev.devfest.agent.domain.model.ToolCall
import dev.devfest.agent.domain.model.ToolDefinition
import dev.devfest.agent.domain.model.ToolResult
import kotlinx.coroutines.flow.Flow

/**
 * Puerto (port) del agente. El dominio define QUÉ necesita; :data decide CÓMO
 * (Gemini por Retrofit, OpenAI, un modelo on-device o un fake para demos).
 */
interface AIAgentRepository {

    /** Herramientas que el agente puede usar (registro de tools). */
    suspend fun availableTools(): List<ToolDefinition>

    /**
     * Un turno del LLM: recibe el historial + herramientas y emite texto y/o
     * solicitudes de herramientas en streaming. Debe ser main-safe.
     */
    fun streamCompletion(
        history: List<AgentMessage>,
        tools: List<ToolDefinition>,
    ): Flow<LlmOutput>

    /** Resuelve (ejecuta) una herramienta nativa. Debe ser main-safe. */
    suspend fun executeTool(call: ToolCall): ToolResult
}
