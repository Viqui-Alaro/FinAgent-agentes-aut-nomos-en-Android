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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.devfest.agent.domain.model.AgentMessage
import dev.devfest.agent.domain.model.Role
import dev.devfest.agent.domain.model.ToolResult
import dev.devfest.agent.domain.model.ToolStatus

/** Renderiza cada tipo de mensaje del protocolo de Function Calling. */
@Composable
fun MessageItem(message: AgentMessage, modifier: Modifier = Modifier) {
    when (message.role) {
        Role.USER -> UserBubble(message.content, modifier)
        Role.ASSISTANT ->
            if (message.isToolCallRequest) ReasoningStep(message, modifier)
            else AssistantBubble(message.content, modifier)
        Role.TOOL -> message.toolResult?.let { ToolResultCard(it, modifier) }
    }
}

@Composable
private fun UserBubble(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(text, Modifier.padding(horizontal = 14.dp, vertical = 10.dp), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
internal fun AssistantBubble(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        AgentAvatar()
        Spacer(Modifier.width(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(text, Modifier.padding(horizontal = 14.dp, vertical = 10.dp), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
internal fun AgentAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(32.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Paso intermedio: el "pensamiento" del modelo + las tools que decidió invocar. */
@Composable
private fun ReasoningStep(message: AgentMessage, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 40.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (message.content.isNotBlank()) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Outlined.Psychology,
                    contentDescription = "Razonamiento",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    message.content,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        message.toolCalls.forEach { call ->
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        call.signature(),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = {
                    Icon(toolVisual(call.name).icon, contentDescription = null, Modifier.size(AssistChipDefaults.IconSize))
                },
            )
        }
    }
}

/** Resultado de una tool tal como se reinyecta al LLM (transparencia para la audiencia). */
@Composable
private fun ToolResultCard(result: ToolResult, modifier: Modifier = Modifier) {
    val visual = toolVisual(result.toolName)
    val colors = MaterialTheme.colorScheme
    val (statusLabel, statusColor) = when (result.status) {
        ToolStatus.SUCCESS -> "OK" to colors.tertiary
        ToolStatus.ERROR -> "Error" to colors.error
        ToolStatus.REJECTED -> "Rechazada" to colors.outline
    }

    OutlinedCard(
        modifier
            .fillMaxWidth()
            .padding(start = 40.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(visual.icon, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Resultado · ${visual.label}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(statusLabel, style = MaterialTheme.typography.labelMedium, color = statusColor)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                result.output.asDisplayJson(),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = colors.onSurfaceVariant,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
