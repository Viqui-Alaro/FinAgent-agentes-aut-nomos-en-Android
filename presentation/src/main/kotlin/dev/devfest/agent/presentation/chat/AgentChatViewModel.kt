package dev.devfest.agent.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.devfest.agent.domain.model.AgentEvent
import dev.devfest.agent.domain.model.ToolCall
import dev.devfest.agent.domain.model.ToolDefinition
import dev.devfest.agent.domain.model.ToolStatus
import dev.devfest.agent.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UDF:  UI --(AgentChatAction)--> ViewModel --(AgentUiState)--> UI
 *
 * El ViewModel NO contiene lógica del agente: solo traduce el Flow<AgentEvent>
 * del caso de uso a estado de UI (función `reduce`) y atiende la confirmación.
 */
@HiltViewModel
class AgentChatViewModel @Inject constructor(
    private val sendMessage: SendMessageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    private var agentJob: Job? = null
    private var pendingApproval: CompletableDeferred<Boolean>? = null

    fun onAction(action: AgentChatAction) {
        when (action) {
            is AgentChatAction.InputChanged -> _uiState.update { it.copy(input = action.text) }
            AgentChatAction.Send -> submit(_uiState.value.input)
            is AgentChatAction.SendSuggestion -> submit(action.text)
            AgentChatAction.Stop -> agentJob?.cancel()
            is AgentChatAction.ResolveConfirmation -> pendingApproval?.complete(action.approved)
            AgentChatAction.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun submit(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || _uiState.value.isBusy) return

        val history = _uiState.value.messages
        _uiState.update { it.copy(input = "", error = null, phase = AgentPhase.Thinking(step = 1)) }

        agentJob = viewModelScope.launch {
            sendMessage(
                history = history,
                userInput = text,
                approver = { call, definition -> awaitUserApproval(call, definition) },
            )
                .catch { e -> _uiState.update { it.copy(error = e.message ?: "Error inesperado del agente") } }
                .onCompletion {
                    // Fin normal, error o cancelación (botón Detener): siempre volvemos a Idle
                    _uiState.update {
                        it.copy(phase = AgentPhase.Idle, activeTool = null, streamingText = "", pendingConfirmation = null)
                    }
                }
                .collect { event -> reduce(event) }
        }
    }

    /** Reducer puro: AgentEvent -> nuevo AgentUiState. */
    private fun reduce(event: AgentEvent) = _uiState.update { state ->
        when (event) {
            is AgentEvent.MessageAppended -> state.copy(
                messages = state.messages + event.message,
                streamingText = "",
            )

            is AgentEvent.Thinking -> state.copy(phase = AgentPhase.Thinking(event.step))

            is AgentEvent.Streaming -> state.copy(
                phase = AgentPhase.Responding,
                streamingText = event.partialText,
            )

            is AgentEvent.ToolStarted -> state.copy(
                phase = AgentPhase.ExecutingTool(event.call.name),
                streamingText = "",
                activeTool = ActiveTool(event.call, event.definition?.description, ToolRunStatus.RUNNING),
            )

            is AgentEvent.ToolFinished -> state.copy(
                activeTool = state.activeTool?.copy(
                    status = when (event.result.status) {
                        ToolStatus.SUCCESS -> ToolRunStatus.SUCCESS
                        ToolStatus.ERROR -> ToolRunStatus.ERROR
                        ToolStatus.REJECTED -> ToolRunStatus.REJECTED
                    }
                ),
            )

            AgentEvent.Completed -> state.copy(phase = AgentPhase.Idle, activeTool = null, streamingText = "")
        }
    }

    /**
     * Human-in-the-loop: el caso de uso SUSPENDE aquí (sin bloquear hilos)
     * hasta que el usuario toque "Autorizar" o "Rechazar" en el diálogo.
     */
    private suspend fun awaitUserApproval(call: ToolCall, definition: ToolDefinition): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        pendingApproval = deferred
        _uiState.update {
            it.copy(
                phase = AgentPhase.AwaitingConfirmation,
                pendingConfirmation = call,
                activeTool = ActiveTool(call, definition.description, ToolRunStatus.AWAITING_CONFIRMATION),
            )
        }
        return try {
            deferred.await().also { approved ->
                _uiState.update {
                    it.copy(
                        phase = if (approved) AgentPhase.ExecutingTool(call.name) else it.phase,
                        activeTool = if (approved) it.activeTool?.copy(status = ToolRunStatus.RUNNING) else it.activeTool,
                    )
                }
            }
        } finally {
            pendingApproval = null
            _uiState.update { it.copy(pendingConfirmation = null) }
        }
    }
}
