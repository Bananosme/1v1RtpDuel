package me.bananosme.rtpduel.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

/**
 * Асинхронно прогружает чанк цели и только потом телепортирует игрока
 * (на главном потоке), чтобы телепорт не фризил сервер.
 */
public final class AsyncTeleporter {

    private AsyncTeleporter() {
    }

    public static CompletableFuture<Void> teleport(JavaPlugin plugin, Player player, Location location) {
        World world = location.getWorld();
        if (world == null) {
            return CompletableFuture.completedFuture(null);
        }
        return world.getChunkAtAsync(location.getBlockX() >> 4, location.getBlockZ() >> 4).thenAccept(chunk ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.teleport(location);
                    }
                }));
    }
}