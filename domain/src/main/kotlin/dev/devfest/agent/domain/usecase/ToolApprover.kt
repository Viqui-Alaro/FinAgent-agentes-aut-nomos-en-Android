package dev.devfest.agent.domain.usecase

import dev.devfest.agent.domain.model.ToolCall
import dev.devfest.agent.domain.model.ToolDefinition

/**
 * Human-in-the-loop. El caso de uso suspende aquí hasta que la UI responda.
 * La presentación lo implementa con un CompletableDeferred + diálogo.
 */
fun interface ToolApprover {
    suspend fun approve(call: ToolCall, definition: ToolDefinition): Boolean
}
