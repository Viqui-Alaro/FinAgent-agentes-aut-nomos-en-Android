package dev.devfest.agent.domain.model

/**
 * Salida "cruda" de un turno del LLM, en streaming.
 * Un turno puede traer texto, llamadas a herramientas, o ambas cosas.
 */
sealed interface LlmOutput {
    data class TextDelta(val text: String) : LlmOutput
    data class ToolCallRequest(val call: ToolCall) : LlmOutput
}

/**
 * Ciclo de vida observable del agente (Razonamiento -> Tool Calling -> Respuesta).
 * Es lo que emite [dev.devfest.agent.domain.usecase.SendMessageUseCase] y consume el ViewModel.
 */
sealed interface AgentEvent {
    /** Un mensaje pasa a formar parte del historial definitivo. */
    data class MessageAppended(val message: AgentMessage) : AgentEvent

    /** El modelo está razonando (paso N del bucle ReAct). */
    data class Thinking(val step: Int) : AgentEvent

    /** Texto parcial mientras el modelo responde (streaming). */
    data class Streaming(val partialText: String) : AgentEvent

    /** Comienza la ejecución de una herramienta. */
    data class ToolStarted(val call: ToolCall, val definition: ToolDefinition?) : AgentEvent

    /** Terminó una herramienta (éxito, error o rechazada por el usuario). */
    data class ToolFinished(val result: ToolResult) : AgentEvent

    /** El agente terminó la tarea. */
    data object Completed : AgentEvent
}
