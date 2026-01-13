package cl.esteban.nexo.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

/**
 * Gestor de selecciones de regiones para administradores.
 * Proporciona un sistema de "varita" para seleccionar áreas cúbicas.
 * 
 * Adaptado para Nexo Standalone.
 */
object SelectionManager {
    
    /**
     * ItemStack de la varita de selección.
     * Los administradores la usan para marcar corners de regiones.
     */
    val selectionWand: ItemStack by lazy {
        ItemStack(Material.BLAZE_ROD).apply {
            itemMeta = itemMeta?.apply {
                displayName(
                    Component.text("Varita de Selección", NamedTextColor.GOLD, TextDecoration.BOLD)
                )
                lore(
                    listOf(
                        Component.text("Clic Izquierdo: Posición 1", NamedTextColor.YELLOW),
                        Component.text("Clic Derecho: Posición 2", NamedTextColor.YELLOW),
                        Component.empty(),
                        Component.text("Usa para seleccionar regiones", NamedTextColor.GRAY)
                    )
                )
            }
        }
    }
    
    // Almacena las selecciones de cada jugador: UUID -> (Pos1, Pos2)
    private val selections = mutableMapOf<UUID, Pair<Location?, Location?>>()
    
    // Jugadores actualmente en modo selección (opcional, simplificado para Nexo)
    private val inSelectionMode = mutableSetOf<UUID>()
    
    /**
     * Establece la primera posición de selección.
     */
    fun setPos1(player: Player, location: Location) {
        val uuid = player.uniqueId
        val current = selections[uuid] ?: Pair(null, null)
        selections[uuid] = Pair(location, current.second)
        
        player.sendMessage(
            Component.text("✓ Posición 1 establecida: ", NamedTextColor.GREEN)
                .append(Component.text(formatLocation(location), NamedTextColor.WHITE))
        )
        
        // Mostrar progreso
        if (current.second != null) {
            player.sendMessage(
                Component.text("¡Selección completa!", NamedTextColor.GOLD, TextDecoration.BOLD)
            )
        }
    }
    
    /**
     * Establece la segunda posición de selección.
     */
    fun setPos2(player: Player, location: Location) {
        val uuid = player.uniqueId
        val current = selections[uuid] ?: Pair(null, null)
        selections[uuid] = Pair(current.first, location)
        
        player.sendMessage(
            Component.text("✓ Posición 2 establecida: ", NamedTextColor.GREEN)
                .append(Component.text(formatLocation(location), NamedTextColor.WHITE))
        )
        
        // Mostrar progreso
        if (current.first != null) {
            player.sendMessage(
                Component.text("¡Selección completa!", NamedTextColor.GOLD, TextDecoration.BOLD)
            )
        }
    }
    
    /**
     * Obtiene la selección actual de un jugador como Cuboid.
     * 
     * @return Cuboid si ambas posiciones están establecidas, null en caso contrario
     */
    fun getSelection(player: Player): Cuboid? {
        val selection = selections[player.uniqueId] ?: return null
        val (pos1, pos2) = selection
        
        if (pos1 == null || pos2 == null) {
            return null
        }
        
        return Cuboid.fromLocations(pos1, pos2)
    }
    
    /**
     * Verifica si un ItemStack es la varita de selección.
     */
    fun isSelectionWand(item: ItemStack): Boolean {
        if (item.type != Material.BLAZE_ROD) return false
        
        val meta = item.itemMeta ?: return false
        val displayName = meta.displayName() ?: return false
        
        // Comparar el nombre de la varita
        return displayName == selectionWand.itemMeta?.displayName()
    }
    
    /**
     * Formatea una location para mostrarla al jugador.
     */
    private fun formatLocation(loc: Location): String {
        return "(${loc.blockX}, ${loc.blockY}, ${loc.blockZ})"
    }
}
