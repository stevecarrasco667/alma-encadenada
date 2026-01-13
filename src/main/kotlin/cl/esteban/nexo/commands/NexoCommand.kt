package cl.esteban.nexo.commands

import cl.esteban.nexo.NexoPlugin
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class NexoCommand(private val plugin: NexoPlugin) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("nexo.admin")) {
            sender.sendMessage("${ChatColor.RED}No tienes permiso.")
            return true
        }

        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "link" -> handleLink(sender, args)
            "unlink" -> handleUnlink(sender, args)
            "linkall" -> handleLinkAll(sender)
            "mode" -> handleMode(sender, args)
            else -> sendHelp(sender)
        }

        return true
    }

    private fun handleLink(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage("${ChatColor.RED}Uso: /nexo link <jugador1> <jugador2>")
            return
        }

        val p1 = Bukkit.getPlayer(args[1])
        val p2 = Bukkit.getPlayer(args[2])

        if (p1 == null || p2 == null) {
            sender.sendMessage("${ChatColor.RED}Uno de los jugadores no está conectado.")
            return
        }

        if (p1 == p2) {
            sender.sendMessage("${ChatColor.RED}No puedes vincular a un jugador consigo mismo.")
            return
        }

        plugin.linkManager.linkPlayers(p1, p2)
        sender.sendMessage("${ChatColor.GREEN}¡Vínculo creado entre ${p1.name} y ${p2.name}!")
        p1.sendMessage("${ChatColor.YELLOW}Has sido vinculado a ${p2.name}.")
        p2.sendMessage("${ChatColor.YELLOW}Has sido vinculado a ${p1.name}.")
    }

    private fun handleUnlink(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Uso: /nexo unlink <jugador>")
            return
        }

        val target = Bukkit.getPlayer(args[1])
        if (target == null) {
            sender.sendMessage("${ChatColor.RED}Jugador no encontrado.")
            return
        }

        plugin.linkManager.unlinkPlayer(target)
        sender.sendMessage("${ChatColor.GREEN}Se han roto las cadenas de ${target.name}.")
        target.sendMessage("${ChatColor.GREEN}¡Tus cadenas han sido rotas!")
    }

    private fun handleLinkAll(sender: CommandSender) {
        val players = Bukkit.getOnlinePlayers().toList()
        if (players.size < 2) {
            sender.sendMessage("${ChatColor.RED}Se necesitan al menos 2 jugadores conectados.")
            return
        }

        // Limpiar todos los vínculos primero
        plugin.linkManager.getAllLinks().keys.forEach { uuid ->
            plugin.linkManager.unlinkUUID(uuid)
        }

        // Vincular en cadena: P1-P2-P3...
        for (i in 0 until players.size - 1) {
            plugin.linkManager.linkPlayers(players[i], players[i+1])
        }

        sender.sendMessage("${ChatColor.GREEN}¡Todos los jugadores han sido vinculados en cadena!")
        Bukkit.broadcastMessage("${ChatColor.GOLD}¡Evento de Cadena Global iniciado!")
    }

    private fun handleMode(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage("${ChatColor.RED}Uso: /nexo mode <damage|physics> <true/false>")
            return
        }

        val setting = args[1].lowercase()
        val value = args[2].toBooleanStrictOrNull()

        if (value == null) {
            sender.sendMessage("${ChatColor.RED}El valor debe ser true o false.")
            return
        }

        when (setting) {
            "physics" -> {
                plugin.config.set("settings.physics", value)
                plugin.saveConfig()
                sender.sendMessage("${ChatColor.GREEN}Físicas establecidas a: $value")
            }
            "damage" -> {
                plugin.config.set("settings.share-damage", value)
                plugin.saveConfig()
                sender.sendMessage("${ChatColor.GREEN}Daño compartido establecido a: $value")
            }
            else -> sender.sendMessage("${ChatColor.RED}Modo desconocido. Usa: damage, physics")
        }
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("${ChatColor.GOLD}--- Nexo Challenge Core ---")
        sender.sendMessage("${ChatColor.YELLOW}/nexo link <p1> <p2> ${ChatColor.WHITE}- Vincular dos jugadores")
        sender.sendMessage("${ChatColor.YELLOW}/nexo unlink <p> ${ChatColor.WHITE}- Desvincular jugador")
        sender.sendMessage("${ChatColor.YELLOW}/nexo linkall ${ChatColor.WHITE}- Vincular a todos")
        sender.sendMessage("${ChatColor.YELLOW}/nexo mode <mode> <val> ${ChatColor.WHITE}- Cambiar configuración")
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (args.size == 1) {
            return listOf("link", "unlink", "linkall", "mode").filter { it.startsWith(args[0].lowercase()) }
        }
        if (args.size == 2) {
            when (args[0].lowercase()) {
                "link", "unlink" -> return null // Return null to show player list
                "mode" -> return listOf("damage", "physics").filter { it.startsWith(args[1].lowercase()) }
            }
        }
        if (args.size == 3) {
            if (args[0].lowercase() == "link") return null // Player list
            if (args[0].lowercase() == "mode") return listOf("true", "false")
        }
        return emptyList()
    }
}
