package dev.devfest.agent.data.llm

import dev.devfest.agent.domain.model.AgentMessage
import dev.devfest.agent.domain.model.LlmOutput
import dev.devfest.agent.domain.model.ToolDefinition
import kotlinx.coroutines.flow.Flow

/**
 * Fuente de datos del "cerebro". Implementaciones intercambiables:
 * - [FakeLlmDataSource]: guionizado, offline, determinista (demo en vivo).
 * - [GeminiLlmDataSource]: Gemini API con Function Calling vía Retrofit.
 * - (tuya) OpenAiLlmDataSource, GeminiNanoLlmDataSource on-device, etc.
 */
interface LlmDataSource {
    fun stream(history: List<AgentMessage>, tools: List<ToolDefinition>): Flow<LlmOutput>
}
