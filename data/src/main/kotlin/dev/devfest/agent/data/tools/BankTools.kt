package dev.devfest.agent.data.tools

import dev.devfest.agent.data.local.FakeBankDataSource
import dev.devfest.agent.data.local.TransferOutcome
import dev.devfest.agent.domain.model.ParameterType
import dev.devfest.agent.domain.model.ToolDefinition
import dev.devfest.agent.domain.model.ToolParameter
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

/** getAccountBalance() — consulta de saldo (solo lectura, sin confirmación). */
class GetBalanceTool @Inject constructor(
    private val bank: FakeBankDataSource,
) : AgentTool {

    override val definition = ToolDefinition(
        name = NAME,
        description = "Consulta el saldo disponible de la cuenta principal del usuario.",
    )

    override suspend fun execute(arguments: Map<String, Any?>): JsonObject =
        buildJsonObject {
            put("account", bank.accountAlias)
            put("balance", bank.currentBalance())
            put("currency", bank.currency)
        }

    companion object { const val NAME = "getAccountBalance" }
}

/**
 * executeBankTransfer(recipient: String, amount: Double)
 * Acción con efectos reales -> requiresConfirmation = true (human-in-the-loop).
 */
class BankTransferTool @Inject constructor(
    private val bank: FakeBankDataSource,
) : AgentTool {

    override val definition = ToolDefinition(
        name = NAME,
        description = "Ejecuta una transferencia bancaria desde la cuenta del usuario a un destinatario. " +
            "Verifica el saldo antes de llamarla.",
        parameters = listOf(
            ToolParameter("recipient", ParameterType.STRING, "Nombre del destinatario"),
            ToolParameter("amount", ParameterType.NUMBER, "Monto en bolivianos (BOB), mayor a 0"),
        ),
        requiresConfirmation = true,
    )

    override suspend fun execute(arguments: Map<String, Any?>): JsonObject {
        val recipient = arguments.requireString("recipient")
        val amount = arguments.requireDouble("amount")

        return when (val outcome = bank.transfer(recipient, amount)) {
            is TransferOutcome.Completed -> buildJsonObject {
                put("status", "COMPLETED")
                put("transactionId", outcome.transactionId)
                put("recipient", recipient)
                put("amount", amount.money())
                put("newBalance", outcome.newBalance)
                put("currency", bank.currency)
            }
            is TransferOutcome.InsufficientFunds -> buildJsonObject {
                put("status", "INSUFFICIENT_FUNDS")
                put("requested", amount.money())
                put("available", outcome.available)
                put("currency", bank.currency)
            }
        }
    }

    companion object { const val NAME = "executeBankTransfer" }
}
