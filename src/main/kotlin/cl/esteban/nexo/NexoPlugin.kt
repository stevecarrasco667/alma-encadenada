package cl.esteban.nexo

import cl.esteban.nexo.commands.NexoCommand
import cl.esteban.nexo.listeners.PlayerQuitListener
import cl.esteban.nexo.services.ChainService
import cl.esteban.nexo.services.CompassService
import cl.esteban.nexo.managers.LinkManager
import cl.esteban.nexo.managers.ReviveManager
import org.bukkit.plugin.java.JavaPlugin

/**
 * Plugin principal Nexo.
 *
 * Transformado a "Nexo Challenge Core":
 * Sistema de supervivencia cooperativo donde los jugadores están
 * permanentemente unidos por una cadena.
 */
class NexoPlugin : JavaPlugin() {

    lateinit var linkManager: LinkManager
        private set

    lateinit var chainService: ChainService
        private set

    lateinit var compassService: CompassService
        private set

    lateinit var reviveManager: ReviveManager
        private set

    override fun onEnable() {
        // Cargar configuración
        saveDefaultConfig()
        reloadConfig()

        // Inicializar LinkManager (Nuevo Núcleo)
        linkManager = LinkManager(this)
        linkManager.loadLinks()

        // Inicializar ChainService
        chainService = ChainService(this)

        // Inicializar CompassService
        compassService = CompassService(this)
        compassService.start()

        // Inicializar ReviveManager
        reviveManager = ReviveManager(this)

        // Registrar listeners
        server.pluginManager.registerEvents(PlayerQuitListener(this), this)
        server.pluginManager.registerEvents(cl.esteban.nexo.listeners.SharedDamageListener(this), this)
        server.pluginManager.registerEvents(cl.esteban.nexo.listeners.SharedDeathListener(this), this)
        server.pluginManager.registerEvents(cl.esteban.nexo.listeners.SharedEffectListener(this), this)
        server.pluginManager.registerEvents(cl.esteban.nexo.listeners.DownedListener(this), this)

        // Registrar comandos
        getCommand("nexo")?.setExecutor(NexoCommand(this))

        logger.info("✓ Nexo Challenge Core habilitado")
    }

    override fun onDisable() {
        // Guardar vínculos
        if (::linkManager.isInitialized) {
            linkManager.saveLinks()
        }

        // Limpiar servicios
        if (::chainService.isInitialized) {
            chainService.clearAll()
        }
        if (::compassService.isInitialized) {
            compassService.stop()
        }
        if (::reviveManager.isInitialized) {
            reviveManager.clearAll()
        }

        logger.info("✓ Nexo deshabilitado")
    }
}
