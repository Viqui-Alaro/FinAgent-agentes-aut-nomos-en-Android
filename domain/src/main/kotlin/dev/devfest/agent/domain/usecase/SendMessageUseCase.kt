package dev.devfest.agent.domain.usecase

import dev.devfest.agent.domain.model.AgentEvent
import dev.devfest.agent.domain.model.AgentMessage
import dev.devfest.agent.domain.model.LlmOutput
import dev.devfest.agent.domain.model.ToolCall
import dev.devfest.agent.domain.model.ToolResult
import dev.devfest.agent.domain.repository.AIAgentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Corazón del agente: el bucle ReAct (Reason + Act) vive en el DOMINIO,
 * no en el ViewModel ni en el repositorio. Así es testeable con JUnit puro
 * y no cambia si cambiamos de proveedor de LLM.
 *
 *   ┌──────────────► LLM (razona) ──────────────┐
 *   │                                           │
 *   │   ¿pidió tools? ── no ──► respuesta final ┘ (fin)
 *   │        │ sí
 *   │        ▼
 *   └── ejecutar tools (con aprobación si aplica) y reinyectar resultados
 *
 * Contrato:
 * - Entrada: historial previo, texto del usuario, un [ToolApprover] y un límite de pasos.
 * - Salida: un Flow frío de [AgentEvent]. Cancelar la colección cancela el agente.
 * - Errores del LLM (red, cuota) se propagan al colector; errores de tools NO:
 *   se convierten en resultados que el modelo puede explicar.
 */
class SendMessageUseCase @Inject constructor(
    private val repository: AIAgentRepository,
    private val executeTool: ExecuteToolUseCase,
) {

    operator fun invoke(
        history: List<AgentMessage>,
        userInput: String,
        approver: ToolApprover,
        maxSteps: Int = DEFAULT_MAX_STEPS,
    ): Flow<AgentEvent> = flow {
        val transcript = history.toMutableList()

        suspend fun append(message: AgentMessage) {
            transcript += message
            emit(AgentEvent.MessageAppended(message))
        }

        append(AgentMessage.user(userInput))

        val tools = repository.availableTools()
        val toolsByName = tools.associateBy { it.name }

        for (step in 1..maxSteps) {
            // 1) RAZONAMIENTO
            emit(AgentEvent.Thinking(step))

            val text = StringBuilder()
            val requestedCalls = mutableListOf<ToolCall>()

            repository.streamCompletion(transcript.toList(), tools).collect { output ->
                when (output) {
                    is LlmOutput.TextDelta -> {
                        text.append(output.text)
                        emit(AgentEvent.Streaming(text.toString()))
                    }
                    is LlmOutput.ToolCallRequest -> requestedCalls += output.call
                }
            }

            // 3) RESPUESTA FINAL: el modelo no pidió herramientas
            if (requestedCalls.isEmpty()) {
                append(AgentMessage.assistant(text.toString().trim().ifEmpty { "(sin respuesta)" }))
                emit(AgentEvent.Completed)
                return@flow
            }

            // 2) TOOL CALLING: guardamos el "pensamiento" + las llamadas pedidas
            append(AgentMessage.assistant(text.toString().trim(), requestedCalls.toList()))

            for (call in requestedCalls) {
                val definition = toolsByName[call.name]
                emit(AgentEvent.ToolStarted(call, definition))

                val result = when {
                    definition == null ->
                        ToolResult.error(call, "Herramienta desconocida: ${call.name}")
                    definition.requiresConfirmation && !approver.approve(call, definition) ->
                        ToolResult.rejected(call)
                    else ->
                        executeTool(call)
                }

                emit(AgentEvent.ToolFinished(result))
                append(AgentMessage.tool(result)) // se reinyecta al LLM en el siguiente paso
            }
        }

        // Guardarraíl: evita bucles infinitos (y facturas infinitas)
        append(
            AgentMessage.assistant(
                "Alcancé el límite de $maxSteps pasos sin terminar la tarea. ¿Puedes reformular tu solicitud?"
            )
        )
        emit(AgentEvent.Completed)
    }

    companion object {
        const val DEFAULT_MAX_STEPS = 6
    }
}
