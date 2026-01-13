package cl.esteban.nexo.managers

import cl.esteban.nexo.NexoPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

/**
 * Gestor del sistema "Last Stand" (Herido Crítico).
 * Controla el estado de abatimiento, sangrado y reanimación.
 */
class ReviveManager(private val plugin: NexoPlugin) {

    // Jugadores abatidos y sus tareas de sangrado
    private val downedPlayers = mutableMapOf<UUID, BukkitTask>()
    
    // Jugadores que están reanimando y sus tareas de canalización
    private val revivingPlayers = mutableMapOf<UUID, BukkitTask>()
    
    // Tiempo de sangrado en segundos (configurable)
    private val bleedTimeSeconds: Int
        get() = plugin.config.getInt("settings.last-stand.bleed-time", 60)

    /**
     * Verifica si un jugador está abatido.
     */
    fun isDowned(player: Player): Boolean {
        return downedPlayers.containsKey(player.uniqueId)
    }
    
    /**
     * Verifica si un jugador está reanimando a alguien.
     */
    fun isReviving(player: Player): Boolean {
        return revivingPlayers.containsKey(player.uniqueId)
    }

    /**
     * Establece el estado de abatido de un jugador.
     */
    fun setDowned(player: Player, downed: Boolean) {
        if (downed) {
            if (isDowned(player)) return // Ya está abatido

            // Iniciar sangrado
            startBleeding(player)
            
            // Aplicar efectos de abatido
            applyDownedEffects(player)
            
            // Mensaje de aviso
            player.sendMessage(Component.text("¡ESTÁS HERIDO DE MUERTE!", NamedTextColor.RED))
            player.sendMessage(Component.text("Espera ayuda o usa /nexo giveup para rendirte.", NamedTextColor.GRAY))
            
            // Sonido dramático
            player.playSound(player.location, Sound.ENTITY_IRON_GOLEM_DEATH, 1.0f, 0.5f)
            
        } else {
            // Reanimar / Limpiar estado
            stopBleeding(player)
            removeDownedEffects(player)
        }
    }
    
    /**
     * Inicia el proceso de reanimación (canalización).
     */
    fun startReviving(healer: Player, target: Player) {
        if (isReviving(healer)) return
        
        val totalTicks = 60 // 3 segundos
        var currentTick = 0
        val startLocation = healer.location.clone()
        
        val task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            // Validaciones continuas
            if (!healer.isOnline || !target.isOnline || !isDowned(target)) {
                cancelRevive(healer)
                return@Runnable
            }
            
            // Verificar movimiento (permitir pequeño margen)
            if (healer.location.distance(startLocation) > 0.5) {
                cancelRevive(healer)
                healer.sendMessage(Component.text("¡Te moviste! Reanimación cancelada.", NamedTextColor.RED))
                return@Runnable
            }
            
            // Verificar distancia al objetivo
            if (healer.location.distance(target.location) > 3.0) {
                cancelRevive(healer)
                healer.sendMessage(Component.text("¡Te alejaste demasiado! Reanimación cancelada.", NamedTextColor.RED))
                return@Runnable
            }
            
            // Feedback visual y auditivo
            if (currentTick % 5 == 0) {
                // Barra de progreso
                val progress = (currentTick.toDouble() / totalTicks.toDouble()) * 20
                val bar = StringBuilder("[")
                for (i in 0 until 20) {
                    if (i < progress) bar.append("|") else bar.append(".")
                }
                bar.append("] Reanimando...")
                
                healer.sendActionBar(Component.text(bar.toString(), NamedTextColor.GREEN))
                
                // Sonido suave
                healer.playSound(healer.location, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f + (currentTick / 120.0f))
                
                // Partículas
                target.world.spawnParticle(Particle.HEART, target.location.add(0.0, 1.0, 0.0), 1, 0.3, 0.3, 0.3, 0.0)
            }
            
            currentTick++
            
            // Finalizar
            if (currentTick >= totalTicks) {
                completeRevive(healer, target)
            }
            
        }, 0L, 1L)
        
        revivingPlayers[healer.uniqueId] = task
    }
    
    /**
     * Cancela el proceso de reanimación.
     */
    fun cancelRevive(healer: Player) {
        revivingPlayers.remove(healer.uniqueId)?.cancel()
        healer.sendActionBar(Component.text("Reanimación cancelada", NamedTextColor.RED))
    }
    
    private fun completeRevive(healer: Player, target: Player) {
        cancelRevive(healer) // Limpiar tarea
        revive(target)
        healer.sendMessage(Component.text("¡Has reanimado a ${target.name}!", NamedTextColor.GREEN))
    }

    /**
     * Reanima a un jugador abatido (Lógica interna).
     */
    fun revive(player: Player) {
        if (!isDowned(player)) return

        setDowned(player, false)
        
        // Restaurar vida (6 corazones)
        player.health = 12.0
        
        // Sonido de alivio
        player.playSound(player.location, Sound.ENTITY_PLAYER_BREATH, 1.0f, 1.0f)
        player.sendMessage(Component.text("¡Has sido reanimado!", NamedTextColor.GREEN))
    }

    private fun startBleeding(player: Player) {
        // Cancelar tarea previa si existe
        stopBleeding(player)

        var timeLeft = bleedTimeSeconds

        val task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            if (!player.isOnline) {
                stopBleeding(player)
                return@Runnable
            }

            // Actualizar efectos visuales (asegurar que sigan aplicados)
            if (timeLeft % 5 == 0) { // Cada 5 segundos
                applyDownedEffects(player)
            }
            
            // Notificar tiempo restante
            if (timeLeft == 30 || timeLeft == 10 || timeLeft <= 5) {
                player.sendActionBar(Component.text("Sangrando... Muerte en ${timeLeft}s", NamedTextColor.RED))
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.0f, 1.0f)
            }

            if (timeLeft <= 0) {
                // Muerte definitiva
                player.health = 0.0
                stopBleeding(player)
            }

            timeLeft--
        }, 0L, 20L) // Ejecutar cada segundo

        downedPlayers[player.uniqueId] = task
    }

    private fun stopBleeding(player: Player) {
        downedPlayers.remove(player.uniqueId)?.cancel()
    }

    private fun applyDownedEffects(player: Player) {
        // Inmovilizar (Slowness 255)
        player.addPotionEffect(PotionEffect(PotionEffectType.SLOW, Int.MAX_VALUE, 255, false, false))
        
        // Ceguera para tensión
        player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, Int.MAX_VALUE, 0, false, false))
        
        // Glow para que el compañero lo encuentre
        player.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, Int.MAX_VALUE, 0, false, false))
        
        // Simular estar tirado (nadando)
        player.isSwimming = true
        player.isGliding = true // A veces ayuda visualmente
    }

    private fun removeDownedEffects(player: Player) {
        player.removePotionEffect(PotionEffectType.SLOW)
        player.removePotionEffect(PotionEffectType.BLINDNESS)
        player.removePotionEffect(PotionEffectType.GLOWING)
        
        player.isSwimming = false
        player.isGliding = false
    }
    
    /**
     * Limpia todos los estados al desactivar el plugin.
     */
    fun clearAll() {
        downedPlayers.values.forEach { it.cancel() }
        downedPlayers.clear()
        
        revivingPlayers.values.forEach { it.cancel() }
        revivingPlayers.clear()
    }
}
