package dev.devfest.agent.presentation.chat.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.devfest.agent.presentation.chat.ActiveTool
import dev.devfest.agent.presentation.chat.ToolRunStatus

/**
 * Tarjeta flotante que muestra en tiempo real qué Tool está invocando el agente.
 * No es modal: el usuario puede seguir haciendo scroll y escribiendo.
 * AnimatedContent conserva la tarjeta saliente durante la animación de salida.
 */
@Composable
fun ToolActivityCard(activeTool: ActiveTool?, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = activeTool,
        modifier = modifier,
        contentKey = { it?.call?.id }, // cambios de estado de la misma tool no re-animan
        transitionSpec = {
            (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
        },
        label = "toolActivityCard",
    ) { tool ->
        if (tool == null) Spacer(Modifier.fillMaxWidth()) else ToolCardBody(tool)
    }
}

@Composable
private fun ToolCardBody(tool: ActiveTool) {
    val colors = MaterialTheme.colorScheme
    val visual = toolVisual(tool.call.name)
    val (title, container) = when (tool.status) {
        ToolRunStatus.AWAITING_CONFIRMATION -> "Esperando tu autorización" to colors.secondaryContainer
        ToolRunStatus.RUNNING -> "Ejecutando en segundo plano" to colors.tertiaryContainer
        ToolRunStatus.SUCCESS -> "Herramienta completada" to colors.tertiaryContainer
        ToolRunStatus.ERROR -> "La herramienta falló" to colors.errorContainer
        ToolRunStatus.REJECTED -> "Rechazada por el usuario" to colors.surfaceVariant
    }
    val inProgress = tool.status == ToolRunStatus.RUNNING || tool.status == ToolRunStatus.AWAITING_CONFIRMATION

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = container),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ToolIconBadge(visual.icon, animate = tool.status == ToolRunStatus.RUNNING)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.labelMedium)
                    Text(
                        tool.call.signature(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusIcon(tool.status)
            }
            if (inProgress) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                )
            }
        }
    }
}

@Composable
private fun ToolIconBadge(icon: ImageVector, animate: Boolean) {
    val transition = rememberInfiniteTransition(label = "toolIcon")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (animate) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "toolIconScale",
    )
    Box(
        Modifier
            .size(38.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun StatusIcon(status: ToolRunStatus) {
    val colors = MaterialTheme.colorScheme
    when (status) {
        ToolRunStatus.RUNNING -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        ToolRunStatus.AWAITING_CONFIRMATION -> Icon(Icons.Outlined.Lock, "Requiere autorización", tint = colors.secondary)
        ToolRunStatus.SUCCESS -> Icon(Icons.Filled.CheckCircle, "Completada", tint = colors.tertiary)
        ToolRunStatus.ERROR -> Icon(Icons.Filled.Error, "Error", tint = colors.error)
        ToolRunStatus.REJECTED -> Icon(Icons.Filled.Block, "Rechazada", tint = colors.outline)
    }
}
