package dev.devfest.agent.presentation.chat.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector
import dev.devfest.agent.domain.model.ToolCall

internal data class ToolVisual(val icon: ImageVector, val label: String)

/** Mapeo nombre de tool -> icono/etiqueta. Las tools desconocidas usan un icono genérico. */
internal fun toolVisual(toolName: String): ToolVisual = when (toolName) {
    "getCurrentWeather" -> ToolVisual(Icons.Outlined.Cloud, "Clima")
    "getAccountBalance" -> ToolVisual(Icons.Outlined.AccountBalance, "Saldo")
    "executeBankTransfer" -> ToolVisual(Icons.Outlined.SwapHoriz, "Transferencia")
    else -> ToolVisual(Icons.Outlined.Build, toolName)
}

/** Firma legible de la llamada: executeBankTransfer(recipient="María", amount=150.0) */
internal fun ToolCall.signature(): String =
    name + arguments.entries.joinToString(prefix = "(", postfix = ")") { (key, value) ->
        "$key=" + if (value is String) "\"$value\"" else value.toString()
    }

/** Formato simple de un JSON plano para mostrarlo en pantalla (no es un parser). */
internal fun String.asDisplayJson(): String =
    trim().removePrefix("{").removeSuffix("}")
        .split(",\"")
        .joinToString("\n") { it.removePrefix("\"").replaceFirst("\":", ": ") }
