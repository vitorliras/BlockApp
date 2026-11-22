package com.vitor.blockapp

import android.app.admin.DevicePolicyManager
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

    private fun adminComponent(context: Context) = ComponentName(context, BlockAdminReceiver::class.java)

    fun isAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(adminComponent(context))
    }

    /**
     * Tela do Android que pede para ativar o BlockApp como administrador do dispositivo.
     * Atenção: NÃO pode ter FLAG_ACTIVITY_NEW_TASK (o Android fecha a tela na hora) e deve ser
     * aberta com startActivityForResult (por isso o app usa um "launcher" ao chamar).
     */
    fun adminIntent(context: Context) = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent(context))
        .putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "Impede que o BlockApp seja desinstalado sem desativar esta proteção antes.",
        )

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
