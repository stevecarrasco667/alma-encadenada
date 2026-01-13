package cl.esteban.nexo.managers

import cl.esteban.nexo.NexoPlugin
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Gestor central de vínculos entre jugadores.
 * Reemplaza al antiguo GameManager.
 *
 * Mantiene el estado de quién está atado a quién.
 * Persistente entre reinicios.
 */
class LinkManager(private val plugin: NexoPlugin) {

    // Mapa de vínculos: UUID -> Set de UUIDs vinculados
    // Es bidireccional: si A -> {B}, entonces B -> {A}
    private val links = ConcurrentHashMap<UUID, MutableSet<UUID>>()
    
    private val gson = Gson()
    private val dataFile = File(plugin.dataFolder, "links.json")

    init {
        loadLinks()
    }

    /**
     * Vincula a dos jugadores.
     */
    fun linkPlayers(player1: Player, player2: Player) {
        linkUUIDs(player1.uniqueId, player2.uniqueId)
    }

    /**
     * Vincula dos UUIDs.
     */
    fun linkUUIDs(id1: UUID, id2: UUID) {
        if (id1 == id2) return

        links.computeIfAbsent(id1) { mutableSetOf() }.add(id2)
        links.computeIfAbsent(id2) { mutableSetOf() }.add(id1)
        
        saveLinks()
    }

    /**
     * Desvincula a un jugador de todos sus vínculos.
     */
    fun unlinkPlayer(player: Player) {
        unlinkUUID(player.uniqueId)
    }

    /**
     * Desvincula un UUID de todos sus vínculos.
     */
    fun unlinkUUID(id: UUID) {
        val linked = links.remove(id) ?: return
        
        for (otherId in linked) {
            links[otherId]?.remove(id)
            if (links[otherId]?.isEmpty() == true) {
                links.remove(otherId)
            }
        }
        
        saveLinks()
    }

    /**
     * Obtiene los jugadores vinculados a un jugador.
     */
    fun getLinkedPlayers(player: Player): Set<UUID> {
        return links[player.uniqueId] ?: emptySet()
    }
    
    /**
     * Obtiene todos los vínculos (para visualización, etc).
     */
    fun getAllLinks(): Map<UUID, Set<UUID>> {
        return links
    }

    /**
     * Guarda los vínculos en disco.
     */
    fun saveLinks() {
        try {
            if (!dataFile.exists()) {
                dataFile.parentFile.mkdirs()
                dataFile.createNewFile()
            }
            
            FileWriter(dataFile).use { writer ->
                gson.toJson(links, writer)
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error guardando vínculos: ${e.message}")
        }
    }

    /**
     * Carga los vínculos desde disco.
     */
    fun loadLinks() {
        if (!dataFile.exists()) return

        try {
            FileReader(dataFile).use { reader ->
                val type = object : TypeToken<ConcurrentHashMap<UUID, MutableSet<UUID>>>() {}.type
                val loaded: ConcurrentHashMap<UUID, MutableSet<UUID>>? = gson.fromJson(reader, type)
                
                if (loaded != null) {
                    links.clear()
                    links.putAll(loaded)
                }
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error cargando vínculos: ${e.message}")
        }
    }
}
