package com.vitor.blockapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Cartão que guia o usuário a ativar as permissões que faltam. */
@Composable
fun PermissionCard(
    accessibilityOn: Boolean,
    overlayOn: Boolean,
    adminOn: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenAppDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Ative a proteção",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Sem estas permissões o BlockApp não consegue bloquear nada.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))

            Step(
                number = "1",
                title = "Acessibilidade",
                detail = "Procure por BlockApp e ligue o serviço.",
                done = accessibilityOn,
                action = "Abrir",
                onClick = onOpenAccessibility,
            )
            Spacer(Modifier.height(10.dp))
            Step(
                number = "2",
                title = "Exibir sobre outros apps",
                detail = "Permita para o BlockApp.",
                done = overlayOn,
                action = "Abrir",
                onClick = onOpenOverlay,
            )
            Spacer(Modifier.height(10.dp))
            Step(
                number = "3",
                title = "Proteção contra desinstalação",
                detail = "Ative como administrador do dispositivo.",
                done = adminOn,
                action = "Ativar",
                onClick = onOpenAdmin,
            )

            if (!accessibilityOn) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "A opção de Acessibilidade está cinza ou pede aviso de segurança? " +
                        "Abra as informações do app, toque nos três pontinhos (⋮) e escolha " +
                        "\"Permitir configurações restritas\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenAppDetails) {
                    Text("Abrir informações do app")
                }
            }
        }
    }
}

@Composable
private fun Step(
    number: String,
    title: String,
    detail: String,
    done: Boolean,
    action: String,
    onClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (done) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Ativado",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        } else {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(28.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (done) "Ativado" else detail,
                style = MaterialTheme.typography.bodySmall,
                color = if (done) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!done) {
            Spacer(Modifier.width(8.dp))
            Button(onClick = onClick, shape = RoundedCornerShape(14.dp)) { Text(action) }
        }
    }
}
