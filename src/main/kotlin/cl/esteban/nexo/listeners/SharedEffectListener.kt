package cl.esteban.nexo.listeners

import cl.esteban.nexo.NexoPlugin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.EntityPotionEffectEvent.Action
import org.bukkit.event.entity.EntityPotionEffectEvent.Cause
import java.util.UUID

/**
 * Listener que sincroniza efectos de poción entre jugadores vinculados.
 * 
 * Cuando un jugador recibe un efecto de poción (beber, splash, beacon, etc.),
 * el mismo efecto se aplica automáticamente a todos sus vinculados.
 */
class SharedEffectListener(private val plugin: NexoPlugin) : Listener {

    companion object {
        // ThreadLocal para evitar bucles infinitos al aplicar efectos
        private val processingEffects = ThreadLocal.withInitial { mutableSetOf<UUID>() }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPotionEffect(event: EntityPotionEffectEvent) {
        // Solo procesar jugadores
        if (event.entity !is Player) return
        val player = event.entity as Player

        // Verificar si la función está habilitada
        if (!plugin.config.getBoolean("settings.share-effects", true)) return

        // Solo procesar cuando se añade o modifica un efecto
        if (event.action != Action.ADDED && event.action != Action.CHANGED) return

        // Verificar que hay un nuevo efecto
        val newEffect = event.newEffect ?: return

        // Filtrar causas relevantes (evitar bucles y efectos naturales no deseados)
        val validCauses = setOf(
            Cause.POTION_DRINK,
            Cause.POTION_SPLASH,
            Cause.AREA_EFFECT_CLOUD,
            Cause.BEACON,
            Cause.COMMAND,
            Cause.FOOD
        )

        if (event.cause !in validCauses) return

        // Evitar bucles: si ya estamos procesando efectos para este jugador, salir
        val processing = processingEffects.get()
        if (player.uniqueId in processing) return

        // Obtener jugadores vinculados
        val linkedPlayers = plugin.linkManager.getLinkedPlayers(player)
        if (linkedPlayers.isEmpty()) return

        // Marcar como procesando
        processing.add(player.uniqueId)

        try {
            for (linkedUUID in linkedPlayers) {
                val linkedPlayer = plugin.server.getPlayer(linkedUUID) ?: continue

                // Evitar aplicar al mismo jugador o a jugadores muertos
                if (linkedPlayer.uniqueId == player.uniqueId || linkedPlayer.isDead) continue

                // Marcar al vinculado como procesando para evitar bucle
                processing.add(linkedUUID)

                try {
                    // Aplicar el mismo efecto al jugador vinculado
                    linkedPlayer.addPotionEffect(newEffect, true)

                    // Log para debugging (opcional)
                    plugin.logger.fine(
                        "[Soul Link] Efecto ${newEffect.type.name} compartido: " +
                        "${player.name} -> ${linkedPlayer.name}"
                    )
                } finally {
                    // Desmarcar al vinculado
                    processing.remove(linkedUUID)
                }
            }
        } finally {
            // Desmarcar al jugador original
            processing.remove(player.uniqueId)
        }
    }
}
