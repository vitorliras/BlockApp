package com.vitor.blockapp.data

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap,
)

/** Nome e ícone de um app pelo pacote, ou nulo se ele não existe mais. */
fun loadAppBrief(context: Context, packageName: String): InstalledApp? = try {
    val pm = context.packageManager
    val info = pm.getApplicationInfo(packageName, 0)
    InstalledApp(
        packageName = packageName,
        label = pm.getApplicationLabel(info).toString(),
        icon = pm.getApplicationIcon(info).toBitmap(160, 160).asImageBitmap(),
    )
} catch (e: android.content.pm.PackageManager.NameNotFoundException) {
    null
}

/** Lê os apps que aparecem no menu do celular (exceto o próprio BlockApp). */
fun loadLaunchableApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != context.packageName }
        .distinctBy { it.activityInfo.packageName }
        .map { info ->
            InstalledApp(
                packageName = info.activityInfo.packageName,
                label = info.loadLabel(pm).toString(),
                icon = info.loadIcon(pm).toBitmap(128, 128).asImageBitmap(),
            )
        }
        .sortedBy { it.label.lowercase() }
}
