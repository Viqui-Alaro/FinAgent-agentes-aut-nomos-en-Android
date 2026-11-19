package dev.devfest.agent.data.tools

import dev.devfest.agent.domain.model.ToolCall
import dev.devfest.agent.domain.model.ToolDefinition
import dev.devfest.agent.domain.model.ToolResult
import dev.devfest.agent.domain.model.ToolStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registro de herramientas. Hilt inyecta TODAS las tools vía multibinding (@IntoSet),
 * así el registro no conoce clases concretas (Open/Closed).
 */
@Singleton
class ToolRegistry @Inject constructor(
    tools: Set<@JvmSuppressWildcards AgentTool>,
) {
    private val toolsByName: Map<String, AgentTool> = tools.associateBy { it.definition.name }

    val definitions: List<ToolDefinition>
        get() = toolsByName.values.map { it.definition }

    suspend fun execute(call: ToolCall): ToolResult {
        val tool = toolsByName[call.name]
            ?: return ToolResult.error(call, "Herramienta no registrada: ${call.name}")
        val output = tool.execute(call.arguments)
        return ToolResult(
            callId = call.id,
            toolName = call.name,
            output = output.toString(),
            status = ToolStatus.SUCCESS,
        )
    }
}
