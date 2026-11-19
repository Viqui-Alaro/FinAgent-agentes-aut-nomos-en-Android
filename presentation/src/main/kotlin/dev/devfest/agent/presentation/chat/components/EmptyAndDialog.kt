package dev.devfest.agent.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.devfest.agent.domain.model.ToolCall
import java.util.Locale
import androidx.compose.foundation.clickable

private val demoSuggestions = listOf(
    "¿Qué clima hace en La Paz?",
    "¿Cuál es mi saldo?",
    "Transfiere 150 a María",
    "Transfiere 9000 a Juan Pérez",
)

@Composable
fun EmptyChatState(onSuggestion: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Agente financiero autónomo", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Razona, elige herramientas nativas y actúa. Las acciones con dinero siempre piden tu autorización.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        demoSuggestions.forEach { suggestion ->
            SuggestionChip(onClick = { onSuggestion(suggestion) }, label = { Text(suggestion) })
        }
    }
}

/** Human-in-the-loop: el agente queda suspendido hasta que el usuario decida. */
@Composable
fun ToolConfirmationDialog(
    call: ToolCall,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
) {
    var selectedAccount by remember { mutableStateOf<FakeAccount?>(null) }
    val isTransfer = call.name == "executeBankTransfer"
    val visual = toolVisual(call.name)

    AlertDialog(
        onDismissRequest = onReject,
        icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        title = { 
            if (isTransfer && selectedAccount == null) {
                Text("Seleccionar cuenta de destino")
            } else {
                Text("¿Confirmar ${visual.label.lowercase()}?") 
            }
        },
        text = {
            if (isTransfer && selectedAccount == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Se encontraron varios contactos. Selecciona uno:", style = MaterialTheme.typography.bodyMedium)
                    fakeAccounts.forEach { account ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAccount = account }
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(account.name, fontWeight = FontWeight.Bold)
                                Text("${account.bank} - ${account.number}", style = MaterialTheme.typography.bodySmall)
                                Text(account.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "El agente quiere ejecutar una acción con efectos reales:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    call.arguments.forEach { (key, value) ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                key.humanize(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            // Si es una transferencia y ya elegimos cuenta, sobrescribimos el nombre
                            val displayValue = if (isTransfer && key == "recipient" && selectedAccount != null) {
                                selectedAccount!!.name
                            } else {
                                value.display(key)
                            }
                            Text(displayValue, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    
                    // Simulación de datos bancarios para transferencia
                    if (isTransfer && selectedAccount != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "Banco",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Text(selectedAccount!!.bank, fontWeight = FontWeight.SemiBold)
                        }
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "Cuenta",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Text(selectedAccount!!.number, fontWeight = FontWeight.SemiBold)
                        }
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "Tipo",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Text(selectedAccount!!.type, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        confirmButton = { 
            if (!isTransfer || selectedAccount != null) {
                Button(onClick = onConfirm) { Text("Confirmar") }
            }
        },
        dismissButton = { 
            TextButton(onClick = onReject) { Text("Cancelar") } 
        },
    )
}

data class FakeAccount(val name: String, val bank: String, val number: String, val type: String)

private val fakeAccounts = listOf(
    FakeAccount("María Elena Pérez", "Banco Unión", "100-12345678", "Cuenta Corriente"),
    FakeAccount("María Antonieta Gómez", "Banco BCP", "201-98765432-1", "Caja de Ahorro"),
    FakeAccount("María José Suárez", "Banco Bisa", "300-11223344", "Cuenta Corriente")
)

private fun String.humanize(): String = when (this) {
    "recipient" -> "Destinatario"
    "amount" -> "Monto"
    else -> replaceFirstChar { it.uppercase() }
}

private fun Any?.display(key: String): String =
    if (key == "amount" && this is Number) String.format(Locale.US, "Bs %.2f", toDouble())
    else toString()
