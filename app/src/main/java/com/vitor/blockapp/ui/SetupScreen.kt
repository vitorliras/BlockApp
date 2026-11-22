package com.vitor.blockapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vitor.blockapp.R
import com.vitor.blockapp.data.LockMethod

/** Fluxo de criar PIN ou padrão: digita uma vez e repete para confirmar. */
@Composable
fun SetupScreen(
    kind: LockMethod,
    title: String,
    onDone: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var first by remember { mutableStateOf<String?>(null) }
    var errorRes by remember { mutableStateOf<Int?>(null) }

    fun handle(secret: String) {
        val previous = first
        when {
            previous == null -> {
                first = secret
                errorRes = null
            }
            previous == secret -> onDone(secret)
            else -> {
                first = null
                errorRes = R.string.setup_mismatch
            }
        }
    }

    val isPin = kind == LockMethod.PIN
    val hasError = errorRes != null
    val message = stringResource(
        errorRes ?: when {
            first == null && isPin -> R.string.setup_pin_hint
            first == null -> R.string.setup_pattern_hint
            isPin -> R.string.setup_pin_confirm
            else -> R.string.setup_pattern_confirm
        }
    )

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(
                    when {
                        first != null -> R.string.setup_confirm
                        isPin -> R.string.setup_create_pin
                        else -> R.string.setup_create_pattern
                    }
                ),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (hasError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            Spacer(Modifier.height(32.dp))

            if (isPin) {
                PinInput(
                    error = hasError,
                    onInputStarted = { errorRes = null },
                    onSubmit = ::handle,
                )
            } else {
                PatternPad(
                    modifier = Modifier.padding(horizontal = 48.dp),
                    error = hasError,
                    onInputStarted = { errorRes = null },
                    onComplete = { path ->
                        if (path.size < 4) {
                            errorRes = R.string.setup_min_points
                        } else {
                            handle(path.joinToString(""))
                        }
                    },
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }
}
