package cl.esteban.nexo.services

import cl.esteban.nexo.NexoPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask

/**
 * Servicio que actualiza la brújula de los jugadores para que apunte
 * automáticamente a su jugador vinculado.
 * 
 * Características:
 * - Actualización cada 20 ticks (1 segundo) por defecto
 * - Funciona entre mundos (apunta al spawn si están en mundos diferentes)
 * - Maneja casos de jugadores offline o muertos
 */
class CompassService(private val plugin: NexoPlugin) {

    private var updateTask: BukkitTask? = null

    /**
     * Inicia el servicio de actualización de brújulas.
     */
    fun start() {
        // Verificar si está habilitado en la configuración
        if (!plugin.config.getBoolean("settings.compass-tracking", true)) {
            plugin.logger.info("[Compass Service] Deshabilitado en configuración")
            return
        }

        // Frecuencia de actualización (en ticks)
        val updateInterval = plugin.config.getLong("settings.compass-update-interval", 20L)

        updateTask = object : BukkitRunnable() {
            override fun run() {
                updateAllCompasses()
            }
        }.runTaskTimer(plugin, 0L, updateInterval)

        plugin.logger.info("[Compass Service] Iniciado (actualización cada ${updateInterval} ticks)")
    }

    /**
     * Detiene el servicio de actualización de brújulas.
     */
    fun stop() {
        updateTask?.let { task ->
            if (!task.isCancelled) {
                task.cancel()
            }
        }
        updateTask = null
        plugin.logger.info("[Compass Service] Detenido")
    }

    /**
     * Actualiza la brújula de todos los jugadores online con vínculos activos.
     */
    private fun updateAllCompasses() {
        val allLinks = plugin.linkManager.getAllLinks()

        for ((playerUUID, linkedUUIDs) in allLinks) {
            val player = plugin.server.getPlayer(playerUUID) ?: continue

            // Si el jugador no tiene vínculos, continuar
            if (linkedUUIDs.isEmpty()) continue

            // Obtener el primer vinculado (en caso de múltiples vínculos)
            // Nota: Si hay múltiples vínculos, la brújula apuntará al primero
            val linkedUUID = linkedUUIDs.firstOrNull() ?: continue
            val linkedPlayer = plugin.server.getPlayer(linkedUUID)

            when {
                // Caso 1: El vinculado está online y en el mismo mundo
                linkedPlayer != null && linkedPlayer.isOnline && !linkedPlayer.isDead -> {
                    if (player.world == linkedPlayer.world) {
                        // Apuntar a la ubicación del vinculado
                        player.compassTarget = linkedPlayer.location
                    } else {
                        // Están en mundos diferentes: apuntar al spawn del mundo del vinculado
                        player.compassTarget = linkedPlayer.world.spawnLocation
                    }
                }

                // Caso 2: El vinculado está offline o muerto
                else -> {
                    // Apuntar al spawn del mundo actual del jugador
                    player.compassTarget = player.world.spawnLocation
                }
            }
        }
    }

    /**
     * Actualiza la brújula de un jugador específico inmediatamente.
     * Útil para llamar cuando se crea un nuevo vínculo.
     */
    fun updateCompass(playerUUID: java.util.UUID) {
        val player = plugin.server.getPlayer(playerUUID) ?: return
        val linkedPlayers = plugin.linkManager.getLinkedPlayers(player)

        if (linkedPlayers.isEmpty()) {
            // Sin vínculos: apuntar al spawn
            player.compassTarget = player.world.spawnLocation
            return
        }

        val linkedUUID = linkedPlayers.firstOrNull() ?: return
        val linkedPlayer = plugin.server.getPlayer(linkedUUID)

        when {
            linkedPlayer != null && linkedPlayer.isOnline && !linkedPlayer.isDead -> {
                if (player.world == linkedPlayer.world) {
                    player.compassTarget = linkedPlayer.location
                } else {
                    player.compassTarget = linkedPlayer.world.spawnLocation
                }
            }
            else -> {
                player.compassTarget = player.world.spawnLocation
            }
        }
    }
}
