package cl.esteban.nexo.listeners

import cl.esteban.nexo.NexoPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent

class SharedDeathListener(private val plugin: NexoPlugin) : Listener {

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity

        // 1. Verificar configuración
        if (!plugin.config.getBoolean("settings.share-death", false)) return

        // 2. Obtener vinculados
        val linkedPlayers = plugin.linkManager.getLinkedPlayers(player)
        if (linkedPlayers.isEmpty()) return

        // 3. Matar a los vinculados
        for (linkedUUID in linkedPlayers) {
            val linkedPlayer = plugin.server.getPlayer(linkedUUID) ?: continue
            
            if (!linkedPlayer.isDead) {
                linkedPlayer.health = 0.0
                linkedPlayer.sendMessage(Component.text("¡El vínculo ha sido fatal!", NamedTextColor.RED))
            }
        }
    }
}
