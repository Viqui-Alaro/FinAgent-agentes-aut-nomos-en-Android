package dev.devfest.agent.data.local

import dev.devfest.agent.data.tools.money
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

sealed interface TransferOutcome {
    data class Completed(val transactionId: String, val newBalance: Double) : TransferOutcome
    data class InsufficientFunds(val available: Double) : TransferOutcome
}

/**
 * "Core bancario" simulado en memoria. El Mutex garantiza consistencia si el
 * agente pidiera varias operaciones concurrentes.
 *
 * Producción: usa BigDecimal (o centavos en Long) para dinero, persistencia en
 * Room y, sobre todo, el backend del banco como fuente de verdad.
 */
@Singleton
class FakeBankDataSource @Inject constructor() {

    private val mutex = Mutex()
    private var balance = INITIAL_BALANCE

    val currency = "BOB"
    val accountAlias = "Caja de ahorro ••4821"

    suspend fun currentBalance(): Double {
        delay(700)
        return mutex.withLock { balance.money() }
    }

    suspend fun transfer(recipient: String, amount: Double): TransferOutcome {
        require(amount > 0) { "El monto debe ser mayor a 0" }
        require(recipient.isNotBlank()) { "El destinatario es obligatorio" }
        delay(1_600) // simula autorización en el core

        return mutex.withLock {
            if (amount > balance) {
                TransferOutcome.InsufficientFunds(balance.money())
            } else {
                balance -= amount
                TransferOutcome.Completed(
                    transactionId = "TRX-" + System.currentTimeMillis().toString().takeLast(8),
                    newBalance = balance.money(),
                )
            }
        }
    }

    private companion object { const val INITIAL_BALANCE = 2_500.0 }
}
