package dev.devfest.agent.data.llm

import dev.devfest.agent.data.tools.BankTransferTool
import dev.devfest.agent.data.tools.GetBalanceTool
import dev.devfest.agent.data.tools.WeatherTool
import dev.devfest.agent.domain.model.AgentMessage
import dev.devfest.agent.domain.model.LlmOutput
import dev.devfest.agent.domain.model.Role
import dev.devfest.agent.domain.model.ToolCall
import dev.devfest.agent.domain.model.ToolDefinition
import dev.devfest.agent.domain.model.ToolResult
import dev.devfest.agent.domain.model.ToolStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

/**
 * LLM simulado con reglas. Imita el comportamiento de un modelo con Function Calling
 * (incluido el razonamiento multi-paso: saldo -> transferencia -> respuesta)
 * para que la charla funcione sin red, sin cuota y siempre igual.
 *
 * Frases de demo:
 *  - "¿Qué clima hace en Cochabamba?"
 *  - "¿Cuál es mi saldo?"
 *  - "Transfiere 150 a María"          (2 tools encadenadas + confirmación)
 *  - "Transfiere 9000 a Juan Pérez"    (saldo insuficiente: el agente se detiene)
 */
class FakeLlmDataSource @Inject constructor() : LlmDataSource {

    private sealed interface Decision {
        data class CallTool(val reasoning: String, val call: ToolCall) : Decision
        data class Answer(val text: String) : Decision
    }

    override fun stream(history: List<AgentMessage>, tools: List<ToolDefinition>): Flow<LlmOutput> = flow {
        delay(THINKING_DELAY_MS) // "pensando..."

        val lastUserIndex = history.indexOfLast { it.role == Role.USER }
        val userText = history.getOrNull(lastUserIndex)?.content.orEmpty()
        val resultsThisTurn = history.drop(lastUserIndex + 1).mapNotNull { it.toolResult }

        when (val decision = decide(userText, resultsThisTurn)) {
            is Decision.CallTool -> {
                streamText(decision.reasoning)
                emit(LlmOutput.ToolCallRequest(decision.call))
            }
            is Decision.Answer -> streamText(decision.text)
        }
    }

    private suspend fun FlowCollector<LlmOutput>.streamText(text: String) {
        text.split(" ").forEach { word ->
            emit(LlmOutput.TextDelta("$word "))
            delay(TOKEN_DELAY_MS)
        }
    }

    private fun decide(userText: String, results: List<ToolResult>): Decision {
        val lower = userText.lowercase()
        val last = results.lastOrNull()

        return when {
            listOf("clima", "tiempo", "temperatura", "llover").any { it in lower } ->
                if (last == null) {
                    val city = extractLocation(userText)
                    Decision.CallTool(
                        reasoning = "Voy a consultar el servicio de clima para $city.",
                        call = ToolCall(name = WeatherTool.NAME, arguments = mapOf("location" to city)),
                    )
                } else {
                    weatherAnswer(last)
                }

            listOf("transfer", "transfier", "envía", "envia", "manda", "paga").any { it in lower } ->
                transferFlow(userText, last)

            "saldo" in lower || "cuánto tengo" in lower || "cuanto tengo" in lower ->
                if (last == null) {
                    Decision.CallTool("Consulto tu cuenta.", ToolCall(name = GetBalanceTool.NAME))
                } else {
                    val json = last.json()
                    Decision.Answer(
                        "Tu saldo disponible en ${json.str("account")} es de " +
                            "Bs ${json.num("balance")}."
                    )
                }

            else -> Decision.Answer(
                "Soy tu agente financiero de demostración. Puedo consultar el clima, " +
                    "revisar tu saldo y hacer transferencias. Prueba con: \"Transfiere 150 a María\"."
            )
        }
    }

    /** Razonamiento multi-paso: 1) saldo  2) transferencia (con confirmación)  3) respuesta. */
    private fun transferFlow(userText: String, last: ToolResult?): Decision {
        val amount = extractAmount(userText)
        val recipient = extractRecipient(userText)
        if (amount == null || recipient == null) {
            return Decision.Answer(
                "Para transferir necesito el destinatario y el monto. Ejemplo: \"Transfiere 150 a María\"."
            )
        }

        return when (last?.toolName) {
            null -> Decision.CallTool(
                reasoning = "Antes de transferir Bs $amount a $recipient, verifico tu saldo disponible.",
                call = ToolCall(name = GetBalanceTool.NAME),
            )

            GetBalanceTool.NAME -> {
                val balance = last.json().num("balance") ?: 0.0
                if (balance >= amount) {
                    Decision.CallTool(
                        reasoning = "Tienes Bs $balance, es suficiente. Preparo la transferencia; necesito tu autorización.",
                        call = ToolCall(
                            name = BankTransferTool.NAME,
                            arguments = mapOf("recipient" to recipient, "amount" to amount),
                        ),
                    )
                } else {
                    Decision.Answer(
                        "No puedo completar la transferencia: tienes Bs $balance y solicitaste Bs $amount. " +
                            "¿Quieres enviar un monto menor?"
                    )
                }
            }

            BankTransferTool.NAME -> when (last.status) {
                ToolStatus.REJECTED -> Decision.Answer("Entendido, cancelé la transferencia. No se movió dinero.")
                ToolStatus.ERROR -> Decision.Answer("Hubo un problema con el banco: ${last.json().str("error")}.")
                ToolStatus.SUCCESS -> {
                    val json = last.json()
                    if (json.str("status") == "COMPLETED") {
                        Decision.Answer(
                            "✅ Listo. Transferí Bs ${json.num("amount")} a ${json.str("recipient")}. " +
                                "Comprobante ${json.str("transactionId")}. Tu nuevo saldo es Bs ${json.num("newBalance")}."
                        )
                    } else {
                        Decision.Answer("El banco rechazó la operación por saldo insuficiente.")
                    }
                }
            }

            else -> Decision.Answer("Operación finalizada.")
        }
    }

    private fun weatherAnswer(result: ToolResult): Decision {
        if (result.status != ToolStatus.SUCCESS) return Decision.Answer("No pude obtener el clima ahora mismo.")
        val json = result.json()
        return Decision.Answer(
            "En ${json.str("location")} está ${json.str("condition")?.lowercase()} con " +
                "${json.num("temperatureC")?.toInt()} °C y ${json.num("humidity")?.toInt()}% de humedad."
        )
    }

    // ---- "NLU" mínimo con regex (en producción esto lo hace el LLM) ----

    private fun extractAmount(text: String): Double? =
        Regex("""(\d+(?:[.,]\d{1,2})?)""").find(text)?.value?.replace(',', '.')?.toDoubleOrNull()

    private fun extractRecipient(text: String): String? =
        Regex("""\ba\s+([a-zA-ZáéíóúÁÉÍÓÚñÑ]+(?:\s+[a-zA-ZáéíóúÁÉÍÓÚñÑ]+)?)""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)

    private fun extractLocation(text: String): String =
        Regex("""\ben\s+([a-zA-ZáéíóúÁÉÍÓÚñÑ]+(?:\s+[a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*)""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1) ?: "La Paz"

    private fun ToolResult.json(): JsonObject =
        runCatching { Json.parseToJsonElement(output).jsonObject }.getOrDefault(JsonObject(emptyMap()))

    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.content
    private fun JsonObject.num(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull

    private companion object {
        const val THINKING_DELAY_MS = 900L
        const val TOKEN_DELAY_MS = 35L
    }
}
