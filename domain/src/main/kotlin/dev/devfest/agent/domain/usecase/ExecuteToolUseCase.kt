package dev.devfest.agent.domain.usecase

import dev.devfest.agent.domain.model.ToolCall
import dev.devfest.agent.domain.model.ToolResult
import dev.devfest.agent.domain.repository.AIAgentRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Ejecuta una herramienta y NUNCA lanza excepción hacia el bucle del agente:
 * un fallo se convierte en un [ToolResult] de error que el LLM puede leer y
 * explicar al usuario ("no pude consultar el clima, ¿intento de nuevo?").
 */
class ExecuteToolUseCase @Inject constructor(
    private val repository: AIAgentRepository,
) {
    suspend operator fun invoke(call: ToolCall): ToolResult =
        try {
            repository.executeTool(call)
        } catch (e: CancellationException) {
            throw e // respetar la cancelación estructurada (botón "Detener")
        } catch (e: Exception) {
            ToolResult.error(call, e.message ?: e::class.simpleName.orEmpty())
        }
}
