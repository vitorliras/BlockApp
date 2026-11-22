package com.vitor.blockapp

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Enquanto o BlockApp for "administrador do dispositivo", o Android não deixa desinstalá-lo
 * direto: primeiro é preciso desativar a administração nas configurações.
 */
class BlockAdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        context.getString(R.string.admin_disable_warning)
}
