package dev.devfest.agent.data.remote

import dev.devfest.agent.data.remote.dto.ContentDto
import dev.devfest.agent.data.remote.dto.FunctionCallDto
import dev.devfest.agent.data.remote.dto.FunctionDeclarationDto
import dev.devfest.agent.data.remote.dto.FunctionResponseDto
import dev.devfest.agent.data.remote.dto.PartDto
import dev.devfest.agent.data.remote.dto.PropertySchemaDto
import dev.devfest.agent.data.remote.dto.SchemaDto
import dev.devfest.agent.domain.model.AgentMessage
import dev.devfest.agent.domain.model.LlmOutput
import dev.devfest.agent.domain.model.ParameterType
import dev.devfest.agent.domain.model.Role
import dev.devfest.agent.domain.model.ToolCall
import dev.devfest.agent.domain.model.ToolDefinition
import dev.devfest.agent.domain.model.ToolResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.util.UUID

private const val THOUGHT_SIGNATURE = "thoughtSignature"

// ---------- Dominio -> Gemini ----------

/** Historial del dominio -> `contents` de Gemini. Agrupa resultados de tools consecutivos. */
internal fun List<AgentMessage>.toGeminiContents(): List<ContentDto> {
    val contents = mutableListOf<ContentDto>()
    val pendingToolParts = mutableListOf<PartDto>()

    fun flushToolResponses() {
        if (pendingToolParts.isNotEmpty()) {
            contents += ContentDto(role = "user", parts = pendingToolParts.toList())
            pendingToolParts.clear()
        }
    }

    for (message in this) {
        when (message.role) {
            Role.TOOL -> message.toolResult?.let { pendingToolParts += PartDto(functionResponse = it.toFunctionResponse()) }

            Role.USER -> {
                flushToolResponses()
                contents += ContentDto(role = "user", parts = listOf(PartDto(text = message.content)))
            }

            Role.ASSISTANT -> {
                flushToolResponses()
                val parts = buildList {
                    if (message.content.isNotBlank()) add(PartDto(text = message.content))
                    message.toolCalls.forEach { call ->
                        add(
                            PartDto(
                                functionCall = FunctionCallDto(name = call.name, args = call.arguments.toJsonObject()),
                                thoughtSignature = call.providerMetadata[THOUGHT_SIGNATURE],
                            )
                        )
                    }
                }
                if (parts.isNotEmpty()) contents += ContentDto(role = "model", parts = parts)
            }
        }
    }
    flushToolResponses()
    return contents
}

internal fun ToolDefinition.toFunctionDeclaration() = FunctionDeclarationDto(
    name = name,
    description = description,
    parameters = parameters.takeIf { it.isNotEmpty() }?.let { params ->
        SchemaDto(
            type = "OBJECT",
            properties = params.associate { it.name to PropertySchemaDto(it.type.toGeminiType(), it.description) },
            required = params.filter { it.required }.map { it.name },
        )
    },
)

private fun ParameterType.toGeminiType() = when (this) {
    ParameterType.STRING -> "STRING"
    ParameterType.NUMBER -> "NUMBER"
    ParameterType.BOOLEAN -> "BOOLEAN"
}

private fun ToolResult.toFunctionResponse() = FunctionResponseDto(
    name = toolName,
    response = runCatching { Json.parseToJsonElement(output).jsonObject }
        .getOrElse { buildJsonObject { put("result", output) } },
)

// ---------- Gemini -> Dominio ----------

internal fun PartDto.toLlmOutput(): LlmOutput? {
    functionCall?.let { fc ->
        return LlmOutput.ToolCallRequest(
            ToolCall(
                id = fc.id ?: UUID.randomUUID().toString(),
                name = fc.name,
                arguments = fc.args?.toArgumentMap().orEmpty(),
                providerMetadata = thoughtSignature?.let { mapOf(THOUGHT_SIGNATURE to it) }.orEmpty(),
            )
        )
    }
    return text?.takeIf { it.isNotEmpty() }?.let { LlmOutput.TextDelta(it) }
}

// ---------- JSON <-> primitivos ----------

internal fun JsonObject.toArgumentMap(): Map<String, Any?> = mapValues { (_, value) -> value.toPlainValue() }

private fun JsonElement.toPlainValue(): Any? = when (this) {
    is JsonNull -> null
    is JsonPrimitive -> if (isString) content else booleanOrNull ?: doubleOrNull ?: content
    else -> toString() // objetos/arrays anidados: se pasan como JSON
}

internal fun Map<String, Any?>.toJsonObject(): JsonObject = buildJsonObject {
    forEach { (key, value) ->
        when (value) {
            null -> put(key, JsonNull)
            is Number -> put(key, value)
            is Boolean -> put(key, value)
            else -> put(key, value.toString())
        }
    }
}
