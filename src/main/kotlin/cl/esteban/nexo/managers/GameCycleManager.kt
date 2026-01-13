package cl.esteban.nexo.managers

import cl.esteban.nexo.NexoPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.util.Random

class GameCycleManager(private val plugin: NexoPlugin) {

    private val LOBBY_WORLD_NAME = "world"
    private val ARENA_WORLD_NAME = "hardcore_arena"

    fun resetGame() {
        val arenaWorld = Bukkit.getWorld(ARENA_WORLD_NAME)
        val lobbyWorld = Bukkit.getWorld(LOBBY_WORLD_NAME) ?: Bukkit.getWorlds().first()

        // 1. Teletransportar a todos los jugadores al Lobby
        Bukkit.getOnlinePlayers().forEach { player ->
            player.teleport(lobbyWorld.spawnLocation)
            player.sendMessage(Component.text("¡El mundo se está reiniciando!", NamedTextColor.YELLOW))
        }

        // 2. Descargar el mundo hardcore
        if (arenaWorld != null) {
            plugin.logger.info("Desconectando mundo $ARENA_WORLD_NAME...")
            val unloaded = Bukkit.unloadWorld(arenaWorld, false) // false = no guardar cambios
            if (!unloaded) {
                plugin.logger.severe("No se pudo descargar el mundo $ARENA_WORLD_NAME. Abortando reset.")
                return
            }
        }

        // 3. Borrar el mundo (en un hilo asíncrono para no congelar demasiado, aunque Files IO suele requerir cuidado)
        // Dado que el mundo ya está unloaded, borrar archivos debería ser seguro.
        // Bukkit requiere que la creación del mundo sea en el Main Thread.
        // Haremos el borrado en un Runnable asíncrono y luego volveremos al sync para crear.
        object : BukkitRunnable() {
            override fun run() {
                val regionFolder = File(Bukkit.getWorldContainer(), ARENA_WORLD_NAME)
                if (regionFolder.exists()) {
                    plugin.logger.info("Borrando carpeta del mundo $ARENA_WORLD_NAME...")
                    val deleted = regionFolder.deleteRecursively()
                    if (!deleted) {
                        plugin.logger.warning("No se pudo borrar completamente la carpeta $ARENA_WORLD_NAME.")
                    } else {
                        plugin.logger.info("Carpeta borrada exitosamente.")
                    }
                }

                // 4. Crear nuevo mundo (Volver al Main Thread)
                object : BukkitRunnable() {
                    override fun run() {
                        createNewWorld()
                    }
                }.runTask(plugin)
            }
        }.runTaskLaterAsynchronously(plugin, 40L) // Esperar 2 segundos para asegurar que todos salieron
    }

    private fun createNewWorld() {
        plugin.logger.info("Generando nuevo mundo $ARENA_WORLD_NAME...")
        val seed = Random().nextLong()
        val creator = WorldCreator(ARENA_WORLD_NAME)
        creator.seed(seed)
        
        val newWorld = creator.createWorld()
        
        if (newWorld != null) {
            plugin.logger.info("¡Nuevo mundo generado con semilla $seed!")
            newWorld.setGameRule(org.bukkit.GameRule.DO_IMMEDIATE_RESPAWN, true)
            
            // Opcional: Teletransportar jugadores de vuelta
            Bukkit.getOnlinePlayers().forEach { player ->
                player.teleport(newWorld.spawnLocation)
                player.sendMessage(Component.text("¡Nuevo ciclo comenzado!", NamedTextColor.GREEN))
                player.health = 20.0
                player.foodLevel = 20
                player.inventory.clear() 
            }
        } else {
            plugin.logger.severe("Error crítico al crear el mundo $ARENA_WORLD_NAME")
        }
    }
}
