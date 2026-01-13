package cl.esteban.nexo.listeners

import cl.esteban.nexo.NexoPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Maneja la desconexión de jugadores.
 */
class PlayerQuitListener(private val plugin: NexoPlugin) : Listener {
    
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // Los vínculos lógicos se mantienen (persistencia)
        // Ya no hay cadenas visuales que limpiar (Soul Link es invisible)
    }
}
