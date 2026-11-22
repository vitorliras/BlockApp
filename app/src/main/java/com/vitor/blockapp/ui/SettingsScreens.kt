package com.vitor.blockapp.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.vitor.blockapp.Permissions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vitor.blockapp.data.LockMethod
import com.vitor.blockapp.data.LockSettings
import com.vitor.blockapp.data.loadAppBrief

/** Segurança geral: como desbloquear quando o app não tem configuração própria. */
@Composable
fun SecurityScreen(
    settings: LockSettings,
    onBack: () -> Unit,
    onSelectDefault: (LockMethod) -> Unit,
    onSetup: (LockMethod) -> Unit,
    onSelectGrace: (Int) -> Unit,
) {
    val context = LocalContext.current
    var adminOn by remember { mutableStateOf(Permissions.isAdminActive(context)) }
    val adminLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        adminOn = Permissions.isAdminActive(context)
    }
    LifecycleResumeEffect(Unit) {
        adminOn = Permissions.isAdminActive(context)
        onPauseOrDispose { }
    }

    ScreenFrame(title = "Segurança", onBack = onBack) {
        SectionTitle("Como desbloquear (padrão geral)")
        Text(
            text = "Vale para todos os apps bloqueados, exceto os que tiverem uma configuração própria.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        OptionCard(
            selected = settings.defaultMethod == LockMethod.PHONE,
            title = "Senha do celular",
            subtitle = "Digital, rosto, PIN ou padrão do Android",
            onClick = { onSelectDefault(LockMethod.PHONE) },
        )
        Spacer(Modifier.height(10.dp))
        OptionCard(
            selected = settings.defaultMethod == LockMethod.PIN,
            title = "PIN do BlockApp",
            subtitle = if (settings.globalPin != null) "PIN definido" else "Ainda não criado",
            actionLabel = if (settings.globalPin != null) "Alterar" else "Criar",
            onAction = { onSetup(LockMethod.PIN) },
            onClick = {
                if (settings.globalPin != null) onSelectDefault(LockMethod.PIN) else onSetup(LockMethod.PIN)
            },
        )
        Spacer(Modifier.height(10.dp))
        OptionCard(
            selected = settings.defaultMethod == LockMethod.PATTERN,
            title = "Padrão do BlockApp",
            subtitle = if (settings.globalPattern != null) "Padrão definido" else "Ainda não criado",
            actionLabel = if (settings.globalPattern != null) "Alterar" else "Criar",
            onAction = { onSetup(LockMethod.PATTERN) },
            onClick = {
                if (settings.globalPattern != null) onSelectDefault(LockMethod.PATTERN) else onSetup(LockMethod.PATTERN)
            },
        )

        Spacer(Modifier.height(28.dp))
        SectionTitle("Tempo de carência")
        Text(
            text = "Depois de sair de um app desbloqueado, ele continua liberado por este tempo. " +
                "Vale também para reabrir o BlockApp.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(0 to "Imediato", 15 to "15 s", 30 to "30 s", 60 to "1 min", 300 to "5 min")
                .forEach { (seconds, label) ->
                    FilterChip(
                        selected = settings.graceSeconds == seconds,
                        onClick = { onSelectGrace(seconds) },
                        label = { Text(label) },
                        shape = RoundedCornerShape(14.dp),
                    )
                }
        }

        Spacer(Modifier.height(28.dp))
        SectionTitle("Proteção contra desinstalação")
        Text(
            text = if (adminOn) {
                "Ativa. Para desinstalar o BlockApp, é preciso desativar o administrador do dispositivo antes."
            } else {
                "Desativada. Ative para que o BlockApp não possa ser desinstalado direto."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!adminOn) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { adminLauncher.launch(Permissions.adminIntent(context)) },
                shape = RoundedCornerShape(14.dp),
            ) { Text("Ativar proteção") }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Dica: bloqueie também o app Configurações. Assim ninguém consegue desativar a " +
                "proteção nem o serviço de acessibilidade do BlockApp sem a sua senha.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Configuração de um app específico. */
@Composable
fun AppConfigScreen(
    packageName: String,
    settings: LockSettings,
    onBack: () -> Unit,
    onSelectMethod: (LockMethod?) -> Unit,
    onSetup: (LockMethod) -> Unit,
) {
    val context = LocalContext.current
    val brief = remember(packageName) { loadAppBrief(context, packageName) }
    val cfg = settings.perApp[packageName]
    val selected = cfg?.method

    ScreenFrame(title = brief?.label ?: packageName, onBack = onBack, icon = brief?.icon) {
        SectionTitle("Como desbloquear este app")
        Spacer(Modifier.height(12.dp))

        OptionCard(
            selected = selected == null,
            title = "Usar o padrão geral",
            subtitle = "Agora: " + when (settings.defaultMethod) {
                LockMethod.PHONE -> "senha do celular"
                LockMethod.PIN -> "PIN do BlockApp"
                LockMethod.PATTERN -> "padrão do BlockApp"
            },
            onClick = { onSelectMethod(null) },
        )
        Spacer(Modifier.height(10.dp))
        OptionCard(
            selected = selected == LockMethod.PHONE,
            title = "Senha do celular",
            subtitle = "Digital, rosto, PIN ou padrão do Android",
            onClick = { onSelectMethod(LockMethod.PHONE) },
        )
        Spacer(Modifier.height(10.dp))
        OptionCard(
            selected = selected == LockMethod.PIN,
            title = "PIN próprio",
            subtitle = if (cfg?.pin != null) "PIN definido só para este app" else "Ainda não criado",
            actionLabel = if (cfg?.pin != null) "Alterar" else "Criar",
            onAction = { onSetup(LockMethod.PIN) },
            onClick = {
                if (cfg?.pin != null) onSelectMethod(LockMethod.PIN) else onSetup(LockMethod.PIN)
            },
        )
        Spacer(Modifier.height(10.dp))
        OptionCard(
            selected = selected == LockMethod.PATTERN,
            title = "Padrão próprio",
            subtitle = if (cfg?.pattern != null) "Padrão definido só para este app" else "Ainda não criado",
            actionLabel = if (cfg?.pattern != null) "Alterar" else "Criar",
            onAction = { onSetup(LockMethod.PATTERN) },
            onClick = {
                if (cfg?.pattern != null) onSelectMethod(LockMethod.PATTERN) else onSetup(LockMethod.PATTERN)
            },
        )
    }
}

@Composable
private fun ScreenFrame(
    title: String,
    onBack: () -> Unit,
    icon: androidx.compose.ui.graphics.ImageBitmap? = null,
    content: @Composable () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun OptionCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}
