package cl.esteban.nexo.listeners

import cl.esteban.nexo.NexoPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.EquipmentSlot

class DownedListener(private val plugin: NexoPlugin) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerDamage(event: EntityDamageEvent) {
        if (event.entity !is Player) return
        val player = event.entity as Player

        // 1. Cancelar reanimación si el sanador recibe daño
        if (plugin.reviveManager.isReviving(player)) {
            plugin.reviveManager.cancelRevive(player)
            player.sendMessage(Component.text("¡Te han herido! Reanimación interrumpida.", NamedTextColor.RED))
        }

        // 2. Lógica de Last Stand (Daño letal)
        if (player.health - event.finalDamage <= 0) {
            
            // Si YA está abatido, permitir que muera (remate)
            if (plugin.reviveManager.isDowned(player)) {
                return
            }

            // Si NO está abatido, interceptar la muerte
            event.isCancelled = true
            player.health = 2.0 // Dejar con 1 corazón (visual)
            
            // Activar estado abatido
            plugin.reviveManager.setDowned(player, true)
            
            // Enviar mensaje clickeable para rendirse
            val giveUpMessage = Component.text(" [CLICK PARA RENDIRTE]", NamedTextColor.RED, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/nexo giveup"))
                .hoverEvent(Component.text("Morir inmediatamente y reiniciar con tu equipo"))
            
            player.sendMessage(Component.text("¡ESTÁS ABATIDO!", NamedTextColor.DARK_RED, TextDecoration.BOLD)
                .append(giveUpMessage))
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEntityEvent) {
        // Solo mano principal para evitar doble ejecución
        if (event.hand != EquipmentSlot.HAND) return
        
        if (event.rightClicked !is Player) return
        val target = event.rightClicked as Player
        val healer = event.player

        // Verificar si el objetivo está abatido
        if (!plugin.reviveManager.isDowned(target)) return

        // Verificar si son compañeros vinculados
        val linkedPlayers = plugin.linkManager.getLinkedPlayers(healer)
        if (!linkedPlayers.contains(target.uniqueId)) {
            healer.sendMessage(Component.text("Solo puedes reanimar a tu compañero vinculado.", NamedTextColor.RED))
            return
        }
        
        // Verificar si ya está reanimando
        if (plugin.reviveManager.isReviving(healer)) return

        // Iniciar reanimación temporizada
        plugin.reviveManager.startReviving(healer, target)
    }
    
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        
        // Si se mueve significativamente mientras reanima, cancelar
        if (plugin.reviveManager.isReviving(player)) {
            if (event.from.distanceSquared(event.to) > 0.01) { // Sensibilidad alta
                // La cancelación ya se maneja en la tarea del ReviveManager,
                // pero podemos hacerlo aquí para respuesta instantánea
                // plugin.reviveManager.cancelRevive(player)
                // Dejamos que el Manager maneje la tolerancia de distancia (0.5 bloques)
            }
        }
    }
}
