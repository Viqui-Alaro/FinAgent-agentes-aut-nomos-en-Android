package dev.devfest.agent.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// DTOs de la Gemini API (REST v1beta, models/{model}:generateContent).
// Viven SOLO en :data. El dominio jamás ve estos tipos.

@Serializable
data class GenerateContentRequest(
    val contents: List<ContentDto>,
    val tools: List<ToolDto>? = null,
    val systemInstruction: ContentDto? = null,
)

@Serializable
data class ContentDto(
    val role: String? = null, // "user" | "model"
    val parts: List<PartDto>,
)

@Serializable
data class PartDto(
    val text: String? = null,
    val functionCall: FunctionCallDto? = null,
    val functionResponse: FunctionResponseDto? = null,
    /** Algunos modelos exigen devolverla en el siguiente turno junto a su functionCall. */
    val thoughtSignature: String? = null,
)

@Serializable
data class FunctionCallDto(
    val name: String,
    val args: JsonObject? = null,
    val id: String? = null,
)

@Serializable
data class FunctionResponseDto(
    val name: String,
    val response: JsonObject,
    val id: String? = null,
)

@Serializable
data class ToolDto(val functionDeclarations: List<FunctionDeclarationDto>)

@Serializable
data class FunctionDeclarationDto(
    val name: String,
    val description: String,
    val parameters: SchemaDto? = null,
)

@Serializable
data class SchemaDto(
    val type: String,
    val properties: Map<String, PropertySchemaDto>? = null,
    val required: List<String>? = null,
)

@Serializable
data class PropertySchemaDto(
    val type: String,
    val description: String? = null,
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<CandidateDto> = emptyList(),
)

@Serializable
data class CandidateDto(
    val content: ContentDto? = null,
    val finishReason: String? = null,
)
