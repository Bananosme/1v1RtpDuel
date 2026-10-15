package me.bananosme.rtpduel.listeners;

import me.bananosme.rtpduel.RtpDuelPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class PlayerListener implements Listener {

    private final RtpDuelPlugin plugin;

    public PlayerListener(RtpDuelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        plugin.getOneVOneManager().handleDeath(event.getEntity());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        plugin.getOneVOneManager().handleRespawn(event);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getOneVOneManager().onPlayerQuit(event.getPlayer());
    }
}