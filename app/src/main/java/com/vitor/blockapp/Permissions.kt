package com.vitor.blockapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Verifica e abre as telas de permissão que o BlockApp precisa. */
object Permissions {

    fun isAccessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val mine = ComponentName(context, BlockerService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == mine }
    }

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun accessibilitySettingsIntent() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun overlaySettingsIntent(context: Context) = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Tela "Informações do app" — onde fica a opção "Permitir configurações restritas". */
    fun appDetailsIntent(context: Context) = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
