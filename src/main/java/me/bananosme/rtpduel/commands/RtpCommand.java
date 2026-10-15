package me.bananosme.rtpduel.commands;

import me.bananosme.rtpduel.RtpDuelPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public final class RtpCommand implements CommandExecutor {

    private final RtpDuelPlugin plugin;

    public RtpCommand(RtpDuelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getTextManager().message("console-only"));
            return true;
        }
        if (!player.hasPermission("rtpduel.rtp")) {
            player.sendMessage(plugin.getTextManager().message("no-permission",
                    Map.of("permission", "rtpduel.rtp")));
            return true;
        }
        plugin.getRtpManager().openGui(player);
        return true;
    }
}