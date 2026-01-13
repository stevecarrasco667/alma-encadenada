package cl.esteban.nexo.listeners

import cl.esteban.nexo.NexoPlugin
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import java.util.UUID

class SharedDamageListener(private val plugin: NexoPlugin) : Listener {

    // Evita bucles infinitos de daño
    private val processingDamage = mutableSetOf<UUID>()

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerDamage(event: EntityDamageEvent) {
        if (event.entity !is Player) return
        val player = event.entity as Player

        // 2. Verificar si ya estamos procesando este daño (evitar bucle)
        if (processingDamage.contains(player.uniqueId)) return

        // 3. Obtener vinculados
        val linkedPlayers = plugin.linkManager.getLinkedPlayers(player)
        if (linkedPlayers.isEmpty()) return

        val damage = event.finalDamage
        if (damage <= 0) return

        // 3. Obtener porcentaje de daño compartido
        val damagePercentage = plugin.config.getDouble("settings.shared-damage-percentage", 0.5)
        val sharedDamage = damage * damagePercentage

        // Marcar al jugador original como procesado
        processingDamage.add(player.uniqueId)

        try {
            for (linkedUUID in linkedPlayers) {
                val linkedPlayer = plugin.server.getPlayer(linkedUUID) ?: continue
                
                // Evitar dañar si ya está muerto o es el mismo jugador
                if (linkedPlayer.isDead || linkedPlayer.uniqueId == player.uniqueId) continue

                // Marcar al vinculado para que su evento de daño no dispare otra cadena
                processingDamage.add(linkedUUID)
                
                try {
                    // Aplicar daño compartido
                    linkedPlayer.damage(sharedDamage)
                    
                    // Reproducir sonido espiritual al jugador que recibe el daño reflejado
                    linkedPlayer.playSound(
                        linkedPlayer.location,
                        Sound.PARTICLE_SOUL_ESCAPE,
                        1.0f,
                        1.2f // Pitch más alto para efecto etéreo
                    )
                } finally {
                    // Desmarcar inmediatamente después
                    processingDamage.remove(linkedUUID)
                }
            }
        } finally {
            processingDamage.remove(player.uniqueId)
        }
    }
}
