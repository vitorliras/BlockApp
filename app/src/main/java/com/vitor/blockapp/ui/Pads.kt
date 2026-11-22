package com.vitor.blockapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vitor.blockapp.R

/** Teclado numérico com bolinhas. Quando o usuário confirma, chama [onSubmit] com os dígitos. */
@Composable
fun PinInput(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: Boolean = false,
    minLength: Int = 4,
    maxLength: Int = 8,
    onInputStarted: () -> Unit = {},
    onSubmit: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.height(18.dp)) {
            repeat(maxOf(minLength, pin.length)) { index ->
                val filled = index < pin.length
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                !filled -> colors.outline
                                error -> colors.error
                                else -> colors.primary
                            }
                        ),
                )
            }
        }
        Spacer(Modifier.height(28.dp))

        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "<", "0", ">")
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                row.forEach { key ->
                    when (key) {
                        "<" -> PadKey(enabled = enabled && pin.isNotEmpty(), filled = false, onClick = {
                            pin = pin.dropLast(1)
                            onInputStarted()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = stringResource(R.string.cd_delete))
                        }
                        ">" -> PadKey(enabled = enabled && pin.length >= minLength, filled = true, onClick = {
                            val value = pin
                            pin = ""
                            onSubmit(value)
                        }) {
                            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.cd_confirm))
                        }
                        else -> PadKey(enabled = enabled, filled = false, onClick = {
                            if (pin.length < maxLength) {
                                pin += key
                                onInputStarted()
                            }
                        }) {
                            Text(key, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun PadKey(
    enabled: Boolean,
    filled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (filled) colors.primary else colors.surfaceContainerHigh,
        contentColor = if (filled) colors.onPrimary else colors.onSurface,
        modifier = Modifier.size(70.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

/**
 * Grade 3x3 para desenhar o padrão. Devolve a lista de pontos (0 a 8) na ordem do desenho.
 * Os pontos são numerados da esquerda para a direita, de cima para baixo.
 */
@Composable
fun PatternPad(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: Boolean = false,
    onInputStarted: () -> Unit = {},
    onComplete: (List<Int>) -> Unit,
) {
    var path by remember { mutableStateOf(emptyList<Int>()) }
    var finger by remember { mutableStateOf<Offset?>(null) }
    val completeState by rememberUpdatedState(onComplete)
    val startedState by rememberUpdatedState(onInputStarted)

    val colors = MaterialTheme.colorScheme
    val lineColor = if (error) colors.error else colors.primary
    val idleColor = colors.outline

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    startedState()
                    val cell = size.width / 3f
                    var last = down.position
                    path = addHits(emptyList(), last, last, cell)
                    finger = last
                    do {
                        val change = awaitPointerEvent().changes.first()
                        val pos = change.position
                        path = addHits(path, last, pos, cell)
                        last = pos
                        finger = pos
                        change.consume()
                    } while (change.pressed)
                    finger = null
                    if (path.isNotEmpty()) completeState(path)
                }
            },
    ) {
        val cell = size.width / 3f
        val stroke = 6.dp.toPx()

        for (i in 0 until path.size - 1) {
            drawLine(lineColor, nodeCenter(path[i], cell), nodeCenter(path[i + 1], cell), stroke, StrokeCap.Round)
        }
        val tip = finger
        if (tip != null && path.isNotEmpty()) {
            drawLine(lineColor.copy(alpha = 0.6f), nodeCenter(path.last(), cell), tip, stroke, StrokeCap.Round)
        }
        for (i in 0..8) {
            val center = nodeCenter(i, cell)
            if (i in path) {
                drawCircle(lineColor.copy(alpha = 0.25f), cell * 0.26f, center)
                drawCircle(lineColor, 10.dp.toPx(), center)
            } else {
                drawCircle(idleColor, 7.dp.toPx(), center)
            }
        }
    }
}

private fun nodeCenter(index: Int, cell: Float) =
    Offset((index % 3 + 0.5f) * cell, (index / 3 + 0.5f) * cell)

/** Acrescenta ao caminho os pontos que o dedo encostou entre [from] e [to]. */
private fun addHits(path: List<Int>, from: Offset, to: Offset, cell: Float): List<Int> {
    val radius = cell * 0.3f
    val hits = (0..8)
        .filter { it !in path && distanceToSegment(nodeCenter(it, cell), from, to) <= radius }
        .sortedBy { (nodeCenter(it, cell) - from).getDistance() }
    return if (hits.isEmpty()) path else path + hits
}

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val lengthSq = abx * abx + aby * aby
    if (lengthSq == 0f) return (p - a).getDistance()
    val t = (((p.x - a.x) * abx + (p.y - a.y) * aby) / lengthSq).coerceIn(0f, 1f)
    return (p - Offset(a.x + t * abx, a.y + t * aby)).getDistance()
}
