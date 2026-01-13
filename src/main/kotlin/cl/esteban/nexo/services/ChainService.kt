package cl.esteban.nexo.services

import cl.esteban.nexo.NexoPlugin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import java.util.UUID

/**
 * Servicio que gestiona la física de la cadena.
 * 
 * Responsabilidades:
 * - Aplicar fuerzas de arrastre entre jugadores vinculados.
 * - Leer configuración de físicas.
 */
class ChainService(private val plugin: NexoPlugin) {
    
    /**
     * Configuración: Distancia máxima permitida entre jugadores (en bloques).
     */
    internal val maxDistance: Double
        get() = plugin.config.getDouble("settings.max-distance", 15.0)
    
    /**
     * Configuración: Fuerza base del tirón.
     */
    internal val pullStrength: Double
        get() = 0.3 // Valor fijo o configurable
    
    /**
     * Configuración: ¿Físicas activas?
     */
    val isPhysicsEnabled: Boolean
        get() = plugin.config.getBoolean("settings.physics", true)
    
    private val airReduction = 0.5
    private val tickInterval = 2L
    
    init {
        startPhysicsTask()
    }
    
    private fun startPhysicsTask() {
        object : BukkitRunnable() {
            override fun run() {
                if (!isPhysicsEnabled) return
                
                applyPhysicsToAllLinks()
            }
        }.runTaskTimer(plugin, 20L, tickInterval)
    }
    
    private fun applyPhysicsToAllLinks() {
        val links = plugin.linkManager.getAllLinks()
        
        // Para evitar procesar el mismo par dos veces, usamos un Set de pares procesados
        val processedPairs = mutableSetOf<String>()
        
        for ((uuid1, targets) in links) {
            val player1 = Bukkit.getPlayer(uuid1) ?: continue
            
            for (uuid2 in targets) {
                // Crear ID único para el par (ordenado alfabéticamente para consistencia)
                val pairId = if (uuid1.toString() < uuid2.toString()) "$uuid1-$uuid2" else "$uuid2-$uuid1"
                
                if (processedPairs.contains(pairId)) continue
                processedPairs.add(pairId)
                
                val player2 = Bukkit.getPlayer(uuid2) ?: continue
                
                // Aplicar física entre player1 y player2
                applyPullForceBetweenPlayers(player1, player2)
            }
        }
    }
    
    private fun applyPullForceBetweenPlayers(player1: Player, player2: Player) {
        // Verificar mundos
        if (player1.world != player2.world) return

        val loc1 = player1.location
        val loc2 = player2.location
        
        val distance = loc1.distance(loc2)
        
        if (distance <= maxDistance) return
        
        val excessDistance = distance - maxDistance
        val forceMagnitude = excessDistance * pullStrength
        
        applyPullToPlayer(player1, loc2, forceMagnitude)
        applyPullToPlayer(player2, loc1, forceMagnitude)
    }
    
    private fun applyPullToPlayer(player: Player, targetLocation: Location, forceMagnitude: Double) {
        val playerLoc = player.location
        val direction = targetLocation.toVector().subtract(playerLoc.toVector()).normalize()
        val pullVector = direction.multiply(forceMagnitude)
        
        if (!player.isOnGround) {
            pullVector.multiply(airReduction)
        }
        
        val currentVelocity = player.velocity
        val newVelocity = currentVelocity.add(pullVector)
        
        val maxVelocity = 2.0
        if (newVelocity.length() > maxVelocity) {
            newVelocity.normalize().multiply(maxVelocity)
        }
        
        player.velocity = newVelocity
    }
    
    fun clearAll() {
        // Nada que limpiar por ahora, la tarea se cancela con el plugin
    }
}
