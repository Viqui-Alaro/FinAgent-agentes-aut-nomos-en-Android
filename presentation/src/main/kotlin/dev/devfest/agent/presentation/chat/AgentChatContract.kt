package dev.devfest.agent.presentation.chat

import dev.devfest.agent.domain.model.AgentMessage
import dev.devfest.agent.domain.model.ToolCall

/** Fase visible del ciclo de vida del agente. */
sealed interface AgentPhase {
    data object Idle : AgentPhase
    data class Thinking(val step: Int) : AgentPhase
    data class ExecutingTool(val toolName: String) : AgentPhase
    data object AwaitingConfirmation : AgentPhase
    data object Responding : AgentPhase
}

enum class ToolRunStatus { AWAITING_CONFIRMATION, RUNNING, SUCCESS, ERROR, REJECTED }

/** Lo que muestra la tarjeta flotante de "tool en segundo plano". */
data class ActiveTool(
    val call: ToolCall,
    val description: String?,
    val status: ToolRunStatus,
)

/** Estado único e inmutable de la pantalla (UDF: una sola fuente de verdad). */
data class AgentUiState(
    val messages: List<AgentMessage> = emptyList(),
    val input: String = "",
    val phase: AgentPhase = AgentPhase.Idle,
    val streamingText: String = "",
    val activeTool: ActiveTool? = null,
    val pendingConfirmation: ToolCall? = null,
    val error: String? = null,
) {
    val isBusy: Boolean get() = phase != AgentPhase.Idle
    val showAgentActivityRow: Boolean get() = isBusy && pendingConfirmation == null
}

/** Intenciones del usuario (eventos que suben de la UI al ViewModel). */
sealed interface AgentChatAction {
    data class InputChanged(val text: String) : AgentChatAction
    data object Send : AgentChatAction
    data class SendSuggestion(val text: String) : AgentChatAction
    data object Stop : AgentChatAction
    data class ResolveConfirmation(val approved: Boolean) : AgentChatAction
    data object DismissError : AgentChatAction
}
