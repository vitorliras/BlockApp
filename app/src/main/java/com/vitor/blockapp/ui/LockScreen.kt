package com.vitor.blockapp.ui

import android.content.pm.PackageManager
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

private data class BlockedAppInfo(val label: String, val icon: ImageBitmap?)

@Composable
fun LockScreen(packageName: String, onUnlock: () -> Unit, onGoHome: () -> Unit) {
    val context = LocalContext.current
    val info = remember(packageName) {
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            BlockedAppInfo(
                label = pm.getApplicationLabel(appInfo).toString(),
                icon = pm.getApplicationIcon(appInfo).toBitmap(160, 160).asImageBitmap(),
            )
        } catch (e: PackageManager.NameNotFoundException) {
            BlockedAppInfo(label = packageName, icon = null)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            // Cadeado com brilho
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
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
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = "App bloqueado",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(20.dp))

            // Cartão com o app que foi bloqueado
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (info.icon != null) {
                        Image(
                            bitmap = info.icon,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        text = info.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Este app está protegido pelo BlockApp.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onUnlock,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Desbloquear", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onGoHome) {
                Text("Voltar para o início")
            }
        }
    }
}
