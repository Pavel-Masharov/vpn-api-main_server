package com.skyrox.vpnapp

import com.skyrox.vpnapp.ApiService.VpnConfig
import com.skyrox.vpnapp.ApiService.VpnServer

object VpnConfigManager {
    var selectedServer: VpnServer? = null
        private set

    var vpnConfig: VpnConfig? = null
        private set

    /**
     * Устанавливает новый сервер и его конфигурацию.
     * Вызывается при выборе сервера на странице выбора серверов.
     */
    fun setConfig(server: VpnServer, config: VpnConfig) {
        selectedServer = server
        vpnConfig = config
    }
}
