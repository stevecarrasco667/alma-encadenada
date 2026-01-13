package cl.esteban.nexo.listeners

import cl.esteban.nexo.NexoPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent

class SharedDeathListener(private val plugin: NexoPlugin) : Listener {

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity

        // 1. Obtener vinculados
        val linkedPlayers = plugin.linkManager.getLinkedPlayers(player)
        // Aunque no tenga vinculados, ¿la muerte debería reiniciar el server si es Hardcore?
        // El prompt dice: "Al detectar muerte, matar a los compañeros vinculados y llamar a GameCycleManager.resetGame()".
        // Asumiremos que si tienes link, se mueren todos y reset. Si juegas solo, reset también.
        // Pero el código original chequeaba vinculados.
        // Vamos a mantener la lógica de vinculados para matar a los otros, pero el reset debería ser global si es "Hardcore Cycle".
        // Sin embargo, si es SOLO vinculados, entonces solo si linkedPlayers no es empty.
        
        if (linkedPlayers.isNotEmpty()) {
             // 2. Matar a los vinculados
            for (linkedUUID in linkedPlayers) {
                val linkedPlayer = plugin.server.getPlayer(linkedUUID) ?: continue
                
                if (!linkedPlayer.isDead) {
                    linkedPlayer.health = 0.0
                    linkedPlayer.sendMessage(Component.text("¡El vínculo ha sido fatal!", NamedTextColor.RED))
                }
            }
        }

        // 3. Iniciar el ciclo de reinicio (Reset Global)
        // Se ejecuta siempre que alguien muera en modo Hardcore, tenga o no vínculos, si queremos ser estrictos.
        // Pero sigamos la instrucción: "Al detectar muerte, matar a los compañeros ... y llamar a resetGame".
        plugin.gameCycleManager.resetGame()
    }
}
