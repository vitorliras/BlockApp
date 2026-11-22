package com.vitor.blockapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vitor.blockapp.LockMode
import com.vitor.blockapp.data.LockMethod
import com.vitor.blockapp.data.loadAppBrief

@Composable
fun LockScreen(
    packageName: String,
    mode: LockMode?,
    wrong: Boolean,
    lockedSeconds: Int,
    onUnlockPhone: () -> Unit,
    onSecret: (String) -> Unit,
    onInputStarted: () -> Unit,
    onGoHome: () -> Unit,
) {
    val context = LocalContext.current
    val app = remember(packageName) { loadAppBrief(context, packageName) }
    val compact = mode is LockMode.Secret

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            LockBadge(size = if (compact) 84.dp else 112.dp, glow = if (compact) 160.dp else 220.dp)
            Spacer(Modifier.height(if (compact) 14.dp else 24.dp))
            Text(
                text = "App bloqueado",
                style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(if (compact) 12.dp else 20.dp))

            // Cartão com o app que foi bloqueado
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    app?.icon?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        text = app?.label ?: packageName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            when (mode) {
                null -> Unit
                LockMode.Phone -> {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Este app está protegido pelo BlockApp.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onUnlockPhone,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Desbloquear", style = MaterialTheme.typography.titleMedium)
                    }
                }
                is LockMode.Secret -> {
                    val isPin = mode.kind == LockMethod.PIN
                    val enabled = lockedSeconds == 0
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = when {
                            !enabled -> "Muitas tentativas. Aguarde ${lockedSeconds}s."
                            wrong && isPin -> "PIN incorreto"
                            wrong -> "Padrão incorreto"
                            isPin -> "Digite seu PIN"
                            else -> "Desenhe seu padrão"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (wrong || !enabled) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    if (isPin) {
                        PinInput(
                            enabled = enabled,
                            error = wrong,
                            onInputStarted = onInputStarted,
                            onSubmit = onSecret,
                        )
                    } else {
                        PatternPad(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            enabled = enabled,
                            error = wrong,
                            onInputStarted = onInputStarted,
                            onComplete = { path -> onSecret(path.joinToString("")) },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onGoHome) {
                Text("Voltar para o início")
            }
        }
    }
}

@Composable
private fun LockBadge(size: Dp, glow: Dp) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(glow)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(size / 2),
            )
        }
    }
}
