package dev.devfest.agent.presentation.chat.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.devfest.agent.presentation.chat.AgentPhase

internal fun AgentPhase.label(): String = when (this) {
    AgentPhase.Idle -> "Listo"
    is AgentPhase.Thinking -> if (step == 1) "Pensando…" else "Pensando… (paso $step)"
    is AgentPhase.ExecutingTool -> "Ejecutando herramienta…"
    AgentPhase.AwaitingConfirmation -> "Esperando tu autorización…"
    AgentPhase.Responding -> "Respondiendo…"
}

/** Indicador compacto para la TopAppBar: punto que late + etiqueta de la fase. */
@Composable
fun AgentStatusIndicator(phase: AgentPhase, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val dotColor by animateColorAsState(
        targetValue = when (phase) {
            AgentPhase.Idle -> colors.tertiary
            is AgentPhase.Thinking, AgentPhase.Responding -> colors.primary
            is AgentPhase.ExecutingTool, AgentPhase.AwaitingConfirmation -> colors.secondary
        },
        label = "statusColor",
    )
    val busy = phase != AgentPhase.Idle
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (busy) 1.6f else 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseScale",
    )

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .background(dotColor, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        AnimatedContent(
            targetState = phase.label(),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "statusLabel",
        ) { text ->
            Text(text, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

/** Tres puntos animados ("el agente está pensando"). */
@Composable
fun ThinkingDots(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(450),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 150),
                ),
                label = "dot$index",
            )
            Box(
                Modifier
                    .size(7.dp)
                    .graphicsLayer { this.alpha = alpha }
                    .background(color, CircleShape)
            )
        }
    }
}

/**
 * Última fila de la lista mientras el agente trabaja:
 * - con texto en streaming -> burbuja que crece con cursor
 * - sin texto -> puntos + fase ("Pensando…", "Ejecutando herramienta…")
 */
@Composable
fun AgentActivityBubble(
    phase: AgentPhase,
    streamingText: String,
    modifier: Modifier = Modifier,
) {
    if (streamingText.isNotEmpty()) {
        AssistantBubble(text = streamingText.trimEnd() + " ▍", modifier = modifier)
        return
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AgentAvatar()
        Spacer(Modifier.width(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp),
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ThinkingDots()
                Spacer(Modifier.width(10.dp))
                Text(
                    text = phase.label(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
