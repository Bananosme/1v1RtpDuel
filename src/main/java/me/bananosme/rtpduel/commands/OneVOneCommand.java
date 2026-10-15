package me.bananosme.rtpduel.commands;

import me.bananosme.rtpduel.RtpDuelPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public final class OneVOneCommand implements CommandExecutor {

    private final RtpDuelPlugin plugin;

    public OneVOneCommand(RtpDuelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getTextManager().message("console-only"));
            return true;
        }
        if (!player.hasPermission("rtpduel.1v1")) {
            player.sendMessage(plugin.getTextManager().message("no-permission",
                    Map.of("permission", "rtpduel.1v1")));
            return true;
        }
        if (command.getName().toLowerCase().contains("leave")) {
            plugin.getOneVOneManager().leaveQueue(player);
        } else {
            plugin.getOneVOneManager().joinQueue(player);
        }
        return true;
    }
}