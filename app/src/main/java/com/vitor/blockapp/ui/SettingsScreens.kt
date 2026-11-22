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
import androidx.compose.ui.res.stringResource
import com.vitor.blockapp.Permissions
import com.vitor.blockapp.R
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

    ScreenFrame(title = stringResource(R.string.security_title), onBack = onBack) {
        SectionTitle(stringResource(R.string.section_default))
        Text(
            text = stringResource(R.string.section_default_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        OptionCard(
            selected = settings.defaultMethod == LockMethod.PHONE,
            title = stringResource(R.string.method_phone_title),
            subtitle = stringResource(R.string.method_phone_desc),
            onClick = { onSelectDefault(LockMethod.PHONE) },
        )
        Spacer(Modifier.height(10.dp))
        OptionCard(
            selected = settings.defaultMethod == LockMethod.PIN,
            title = stringResource(R.string.pin_blockapp),
            subtitle = stringResource(if (settings.globalPin != null) R.string.pin_defined else R.string.not_created),
            actionLabel = stringResource(if (settings.globalPin != null) R.string.action_change else R.string.action_create),
            onAction = { onSetup(LockMethod.PIN) },
            onClick = {
                if (settings.globalPin != null) onSelectDefault(LockMethod.PIN) else onSetup(LockMethod.PIN)
            },
        )
        Spacer(Modifier.height(10.dp))
        OptionCard(
            selected = settings.defaultMethod == LockMethod.PATTERN,
            title = stringResource(R.string.pattern_blockapp),
            subtitle = stringResource(if (settings.globalPattern != null) R.string.pattern_defined else R.string.not_created),
            actionLabel = stringResource(if (settings.globalPattern != null) R.string.action_change else R.string.action_create),
            onAction = { onSetup(LockMethod.PATTERN) },
            onClick = {
                if (settings.globalPattern != null) onSelectDefault(LockMethod.PATTERN) else onSetup(LockMethod.PATTERN)
            },
        )

        Spacer(Modifier.height(28.dp))
        SectionTitle(stringResource(R.string.grace_title))
        Text(
            text = stringResource(R.string.grace_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(0, 15, 30, 60, 300).forEach { seconds ->
                FilterChip(
                    selected = settings.graceSeconds == seconds,
                    onClick = { onSelectGrace(seconds) },
                    label = {
                        Text(
                            when {
                                seconds == 0 -> stringResource(R.string.grace_now)
                                seconds < 60 -> stringResource(R.string.grace_seconds, seconds)
                                else -> stringResource(R.string.grace_minutes, seconds / 60)
                            }
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        SectionTitle(stringResource(R.string.uninstall_title))
        Text(
            text = stringResource(if (adminOn) R.string.uninstall_on else R.string.uninstall_off),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!adminOn) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { adminLauncher.launch(Permissions.adminIntent(context)) },
                shape = RoundedCornerShape(14.dp),
            ) { Text(stringResource(R.string.action_activate_protection)) }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.uninstall_tip),
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
        SectionTitle(stringResource(R.string.section_app))
        Spacer(Modifier.height(12.dp))

        OptionCard(
            selected = selected == null,
            title = stringResource(R.string.use_general),
            subtitle = stringResource(
                R.string.use_general_now,
                stringResource(
                    when (settings.defaultMethod) {
                        LockMethod.PHONE -> R.string.method_name_phone
                        LockMethod.PIN -> R.string.method_name_pin
                        LockMethod.PATTERN -> R.string.method_name_pattern
                    }
                ),
            ),
            onClick = { onSelectMethod(null) },
        )
        Spacer(Modifier.height(10.dp))
        OptionCard(
            selected = selected == LockMethod.PHONE,
            title = stringResource(R.string.method_phone_title),
            subtitle = stringResource(R.string.method_phone_desc),
            onClick = { onSelectMethod(LockMethod.PHONE) },
        )
        Spacer(Modifier.height(10.dp))
        OptionCard(
            selected = selected == LockMethod.PIN,
            title = stringResource(R.string.own_pin),
            subtitle = stringResource(if (cfg?.pin != null) R.string.own_pin_defined else R.string.not_created),
            actionLabel = stringResource(if (cfg?.pin != null) R.string.action_change else R.string.action_create),
            onAction = { onSetup(LockMethod.PIN) },
            onClick = {
                if (cfg?.pin != null) onSelectMethod(LockMethod.PIN) else onSetup(LockMethod.PIN)
            },
        )
        Spacer(Modifier.height(10.dp))
        OptionCard(
            selected = selected == LockMethod.PATTERN,
            title = stringResource(R.string.own_pattern),
            subtitle = stringResource(if (cfg?.pattern != null) R.string.own_pattern_defined else R.string.not_created),
            actionLabel = stringResource(if (cfg?.pattern != null) R.string.action_change else R.string.action_create),
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
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
