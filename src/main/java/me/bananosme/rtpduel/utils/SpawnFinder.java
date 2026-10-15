package me.bananosme.rtpduel.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SpawnFinder {

    private static final Set<Material> FORBIDDEN = EnumSet.of(
            Material.OBSIDIAN,
            Material.CRYING_OBSIDIAN,
            Material.BEDROCK,
            Material.BARRIER,
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.STRUCTURE_BLOCK,
            Material.JIGSAW,
            Material.END_PORTAL,
            Material.END_PORTAL_FRAME,
            Material.END_GATEWAY,
            Material.LIGHT,
            Material.STRUCTURE_VOID,
            Material.DRAGON_EGG,
            Material.RESPAWN_ANCHOR,
            Material.REINFORCED_DEEPSLATE
    );

    private static final int MAX_ATTEMPTS = 100;

    private static final int[][] OFFSETS = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1},
            {1, 1},
            {1, -1},
            {-1, 1},
            {-1, -1}
    };

    private SpawnFinder() {
    }

    /**
     * Проверяет, что точка спавна у этого блока-пола валидна (2+ воздуха над полом).
     */
    public static boolean isValidSpawnPoint(Block ground) {
        return ground != null && hasAirAbove(ground, 3);
    }

    /**
     * Спавн-позиция игрока: Y пола + 1.
     */
    public static Location spawnLocation(Block ground) {
        return ground.getLocation().add(0.5, 1, 0.5);
    }

    /**
     * Асинхронно ищет блок-пол для спавна: чанки грузятся без фриза сервера,
     * но чтение блоков выполняется только на главном потоке. Future завершается
     * на главном потоке, поэтому колбэки можно безопасно трогать Bukkit API.
     *
     * @return future с блоком-полом, или null если не найдено
     */
    public static CompletableFuture<Block> findGroundAsync(JavaPlugin plugin, World world, int border, Random random) {
        CompletableFuture<Block> result = new CompletableFuture<>();
        runOnMain(plugin, () -> {
            for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                int x = random.nextInt(border * 2 + 1) - border;
                int z = random.nextInt(border * 2 + 1) - border;
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                Block ground = findHighestSolidBlock(world, x, z);
                if (isValidSpawnPoint(ground)) {
                    result.complete(ground);
                    return;
                }
            }
            int x = random.nextInt(border * 2 + 1) - border;
            int z = random.nextInt(border * 2 + 1) - border;
            world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk ->
                    runOnMain(plugin, () -> {
                        Block ground = findHighestSolidBlock(world, x, z);
                        result.complete(isValidSpawnPoint(ground) ? ground : null);
                    }));
        });
        return result;
    }

    /**
     * Асинхронно подбирает пару точек для дуэли: базовая точка + смещение по одной
     * из осей на заданное расстояние. Возвращает [loc1, null], если точка для
     * второго игрока не найдена. Future завершается на главном потоке.
     */
    public static CompletableFuture<Location[]> findPairSpawnsAsync(JavaPlugin plugin, World world, int border, int distance, Random random) {
        return findGroundAsync(plugin, world, border, random).thenCompose(base -> {
            if (base == null) {
                return CompletableFuture.completedFuture(null);
            }
            Location first = spawnLocation(base);
            for (int[] offset : OFFSETS) {
                int x = base.getX() + offset[0] * distance;
                int z = base.getZ() + offset[1] * distance;
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                Block other = findHighestSolidBlock(world, x, z);
                if (isValidSpawnPoint(other)) {
                    return CompletableFuture.completedFuture(new Location[]{first, spawnLocation(other)});
                }
            }
            int[] offset = OFFSETS[random.nextInt(OFFSETS.length)];
            int x = base.getX() + offset[0] * distance;
            int z = base.getZ() + offset[1] * distance;
            CompletableFuture<Location[]> pair = new CompletableFuture<>();
            world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk ->
                    runOnMain(plugin, () -> {
                        Block other = findHighestSolidBlock(world, x, z);
                        pair.complete(isValidSpawnPoint(other)
                                ? new Location[]{first, spawnLocation(other)}
                                : new Location[]{first, null});
                    }));
            return pair;
        });
    }

    /**
     * Выполняет задачу на главном потоке (inline, если уже на нём). Если плагин
     * выключен — задача не выполняется (future останется незавершённым, но плагин
     * в этот момент уже выгружается и это ни на что не влияет).
     */
    private static void runOnMain(JavaPlugin plugin, Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }
        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Ищет самый верхний твердый блок в колонке (x, z), отбрасывая воду/лаву и искусственные блоки.
     * Вызывать только на главном потоке.
     */
    private static Block findHighestSolidBlock(World world, int x, int z) {
        for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            if (type.isAir() || type == Material.WATER || type == Material.LAVA
                    || type == Material.SEAGRASS || type == Material.TALL_SEAGRASS || type == Material.KELP
                    || type == Material.KELP_PLANT) {
                continue;
            }
            if (block.isSolid() && !FORBIDDEN.contains(type)) {
                return block;
            }
        }
        return null;
    }

    private static boolean hasAirAbove(Block ground, int airBlocks) {
        int y = ground.getY() + 1;
        for (int i = 0; i < airBlocks; i++) {
            Material type = ground.getWorld().getBlockAt(ground.getX(), y + i, ground.getZ()).getType();
            if (!type.isAir()) {
                return false;
            }
        }
        return true;
    }
}
