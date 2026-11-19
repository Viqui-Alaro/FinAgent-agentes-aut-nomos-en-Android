package dev.devfest.agent.domain.usecase

import dev.devfest.agent.domain.model.AgentEvent
import dev.devfest.agent.domain.model.AgentMessage
import dev.devfest.agent.domain.model.LlmOutput
import dev.devfest.agent.domain.model.Role
import dev.devfest.agent.domain.model.ToolCall
import dev.devfest.agent.domain.model.ToolDefinition
import dev.devfest.agent.domain.model.ToolResult
import dev.devfest.agent.domain.model.ToolStatus
import dev.devfest.agent.domain.repository.AIAgentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** El bucle del agente se prueba sin Android, sin red y sin LLM real. */
class SendMessageUseCaseTest {

    private val transferDef = ToolDefinition("executeBankTransfer", "transfer", requiresConfirmation = true)
    private val weatherDef = ToolDefinition("getCurrentWeather", "weather")

    private class ScriptedRepo(
        private val tools: List<ToolDefinition>,
        private val turns: ArrayDeque<List<LlmOutput>>,
    ) : AIAgentRepository {
        var executed = 0
        override suspend fun availableTools() = tools
        override fun streamCompletion(history: List<AgentMessage>, tools: List<ToolDefinition>): Flow<LlmOutput> =
            flowOf(*turns.removeFirst().toTypedArray())
        override suspend fun executeTool(call: ToolCall): ToolResult {
            executed++
            return ToolResult(call.id, call.name, """{"ok":true}""", ToolStatus.SUCCESS)
        }
    }

    @Test
    fun `ejecuta la tool y luego responde`() = runTest {
        val call = ToolCall(name = "getCurrentWeather", arguments = mapOf("location" to "La Paz"))
        val repo = ScriptedRepo(
            listOf(weatherDef),
            ArrayDeque(listOf(listOf(LlmOutput.ToolCallRequest(call)), listOf(LlmOutput.TextDelta("Hace sol")))),
        )
        val useCase = SendMessageUseCase(repo, ExecuteToolUseCase(repo))

        val events = useCase(emptyList(), "clima?", approver = { _, _ -> true }).toList()
        val roles = events.filterIsInstance<AgentEvent.MessageAppended>().map { it.message.role }

        assertEquals(listOf(Role.USER, Role.ASSISTANT, Role.TOOL, Role.ASSISTANT), roles)
        assertEquals(1, repo.executed)
        assertTrue(events.last() is AgentEvent.Completed)
    }

    @Test
    fun `una tool sensible rechazada no se ejecuta`() = runTest {
        val call = ToolCall(name = "executeBankTransfer", arguments = mapOf("amount" to 100.0))
        val repo = ScriptedRepo(
            listOf(transferDef),
            ArrayDeque(listOf(listOf(LlmOutput.ToolCallRequest(call)), listOf(LlmOutput.TextDelta("Cancelado")))),
        )
        val useCase = SendMessageUseCase(repo, ExecuteToolUseCase(repo))

        val events = useCase(emptyList(), "transfiere", approver = { _, _ -> false }).toList()
        val finished = events.filterIsInstance<AgentEvent.ToolFinished>().single()

        assertEquals(ToolStatus.REJECTED, finished.result.status)
        assertEquals(0, repo.executed)
    }
}
