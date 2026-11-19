package dev.devfest.agent.presentation.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.devfest.agent.domain.model.AgentMessage
import dev.devfest.agent.domain.model.ToolCall
import dev.devfest.agent.domain.model.ToolResult
import dev.devfest.agent.domain.model.ToolStatus
import dev.devfest.agent.presentation.chat.components.AgentActivityBubble
import dev.devfest.agent.presentation.chat.components.AgentStatusIndicator
import dev.devfest.agent.presentation.chat.components.ChatInputBar
import dev.devfest.agent.presentation.chat.components.EmptyChatState
import dev.devfest.agent.presentation.chat.components.MessageItem
import dev.devfest.agent.presentation.chat.components.ToolActivityCard
import dev.devfest.agent.presentation.chat.components.ToolConfirmationDialog
import dev.devfest.agent.presentation.theme.FinAgentTheme

/** Punto de entrada con estado: conecta el ViewModel (Hilt) con la UI sin estado. */
@Composable
fun AgentChatScreen(viewModel: AgentChatViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AgentChatContent(state = state, onAction = viewModel::onAction)
}

/** UI sin estado: fácil de previsualizar y de testear con Compose UI tests. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentChatContent(
    state: AgentUiState,
    onAction: (AgentChatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val itemCount = state.messages.size + if (state.showAgentActivityRow) 1 else 0

    // Autoscroll: al llegar mensajes, cambiar de fase o crecer el streaming
    LaunchedEffect(state.messages.size, state.phase, state.streamingText.length / 60) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            onAction(AgentChatAction.DismissError)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("FinAgent", fontWeight = FontWeight.SemiBold)
                        AgentStatusIndicator(phase = state.phase)
                    }
                },
            )
        },
        bottomBar = {
            ChatInputBar(
                value = state.input,
                isBusy = state.isBusy,
                onValueChange = { onAction(AgentChatAction.InputChanged(it)) },
                onSend = { onAction(AgentChatAction.Send) },
                onStop = { onAction(AgentChatAction.Stop) },
                modifier = Modifier.navigationBarsPadding().imePadding(),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.messages.isEmpty()) {
                EmptyChatState(
                    onSuggestion = { onAction(AgentChatAction.SendSuggestion(it)) },
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    state = listState,
                    // espacio superior para que la tarjeta flotante no tape el primer mensaje
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 88.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        MessageItem(message = message, modifier = Modifier.animateItem())
                    }
                    if (state.showAgentActivityRow) {
                        item(key = "agent-activity") {
                            AgentActivityBubble(
                                phase = state.phase,
                                streamingText = state.streamingText,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }

            // Tarjeta flotante NO modal: la UI sigue siendo usable mientras la tool corre
            ToolActivityCard(
                activeTool = state.activeTool,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }

    state.pendingConfirmation?.let { call ->
        ToolConfirmationDialog(
            call = call,
            onConfirm = { onAction(AgentChatAction.ResolveConfirmation(approved = true)) },
            onReject = { onAction(AgentChatAction.ResolveConfirmation(approved = false)) },
        )
    }
}

// ---------------------------- Previews ----------------------------

private val previewCall = ToolCall(
    id = "c1",
    name = "executeBankTransfer",
    arguments = mapOf("recipient" to "María", "amount" to 150.0),
)

private val previewMessages = listOf(
    AgentMessage.user("Transfiere 150 a María"),
    AgentMessage.assistant(
        "Antes de transferir, verifico tu saldo disponible.",
        listOf(ToolCall(id = "c0", name = "getAccountBalance")),
    ),
    AgentMessage.tool(
        ToolResult("c0", "getAccountBalance", """{"balance":2500.0,"currency":"BOB"}""", ToolStatus.SUCCESS)
    ),
    AgentMessage.assistant("Tienes Bs 2500. Preparo la transferencia.", listOf(previewCall)),
)

@Preview(showBackground = true)
@Composable
private fun AgentChatEmptyPreview() {
    FinAgentTheme { AgentChatContent(state = AgentUiState(), onAction = {}) }
}

@Preview(showBackground = true)
@Composable
private fun AgentChatExecutingToolPreview() {
    FinAgentTheme {
        AgentChatContent(
            state = AgentUiState(
                messages = previewMessages,
                phase = AgentPhase.ExecutingTool("executeBankTransfer"),
                activeTool = ActiveTool(previewCall, "Ejecuta una transferencia bancaria", ToolRunStatus.RUNNING),
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AgentChatThinkingPreview() {
    FinAgentTheme {
        AgentChatContent(
            state = AgentUiState(messages = previewMessages.take(1), phase = AgentPhase.Thinking(1)),
            onAction = {},
        )
    }
}
