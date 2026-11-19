package dev.devfest.agent.data.repository

import dev.devfest.agent.data.di.IoDispatcher
import dev.devfest.agent.data.llm.LlmDataSource
import dev.devfest.agent.data.tools.ToolRegistry
import dev.devfest.agent.domain.model.AgentMessage
import dev.devfest.agent.domain.model.LlmOutput
import dev.devfest.agent.domain.model.ToolCall
import dev.devfest.agent.domain.model.ToolDefinition
import dev.devfest.agent.domain.model.ToolResult
import dev.devfest.agent.domain.repository.AIAgentRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Adaptador: une el "cerebro" (LlmDataSource) con las "manos" (ToolRegistry). */
class AIAgentRepositoryImpl @Inject constructor(
    private val llm: LlmDataSource,
    private val toolRegistry: ToolRegistry,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AIAgentRepository {

    override suspend fun availableTools(): List<ToolDefinition> = toolRegistry.definitions

    override fun streamCompletion(
        history: List<AgentMessage>,
        tools: List<ToolDefinition>,
    ): Flow<LlmOutput> = llm.stream(history, tools).flowOn(ioDispatcher)

    override suspend fun executeTool(call: ToolCall): ToolResult =
        withContext(ioDispatcher) { toolRegistry.execute(call) }
}
