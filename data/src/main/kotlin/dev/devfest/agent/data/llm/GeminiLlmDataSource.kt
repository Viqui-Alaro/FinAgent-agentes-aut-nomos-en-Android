package dev.devfest.agent.data.llm

import dev.devfest.agent.data.remote.GeminiApi
import dev.devfest.agent.data.remote.dto.ContentDto
import dev.devfest.agent.data.remote.dto.GenerateContentRequest
import dev.devfest.agent.data.remote.dto.PartDto
import dev.devfest.agent.data.remote.dto.ToolDto
import dev.devfest.agent.data.remote.toFunctionDeclaration
import dev.devfest.agent.data.remote.toGeminiContents
import dev.devfest.agent.data.remote.toLlmOutput
import dev.devfest.agent.domain.model.AgentMessage
import dev.devfest.agent.domain.model.LlmOutput
import dev.devfest.agent.domain.model.ToolDefinition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Gemini API con Function Calling. Un turno = una llamada generateContent.
 * El bucle (llamar de nuevo con los resultados de las tools) lo hace el DOMINIO,
 * no esta clase: aquí solo traducimos formatos.
 */
class GeminiLlmDataSource(
    private val api: GeminiApi,
    private val model: String,
) : LlmDataSource {

    override fun stream(history: List<AgentMessage>, tools: List<ToolDefinition>): Flow<LlmOutput> = flow {
        val request = GenerateContentRequest(
            contents = history.toGeminiContents(),
            tools = tools.takeIf { it.isNotEmpty() }?.let { listOf(ToolDto(it.map(ToolDefinition::toFunctionDeclaration))) },
            systemInstruction = ContentDto(parts = listOf(PartDto(text = SYSTEM_PROMPT))),
        )

        val response = api.generateContent(model = model, body = request)
        val parts = response.candidates.firstOrNull()?.content?.parts.orEmpty()

        parts.forEach { part -> part.toLlmOutput()?.let { emit(it) } }
    }

    private companion object {
        val SYSTEM_PROMPT = """
            Eres FinAgent, un asistente financiero dentro de una app bancaria boliviana.
            Responde en español, breve y claro. Montos en bolivianos (Bs).
            Reglas:
            - Nunca inventes saldos, clima ni comprobantes: usa SIEMPRE las herramientas.
            - Antes de executeBankTransfer, consulta getAccountBalance.
            - Si una herramienta falla o el usuario la rechaza, explícalo y no reintentes sin permiso.
            - Antes de llamar herramientas, escribe en una frase corta qué vas a hacer.
        """.trimIndent()
    }
}
