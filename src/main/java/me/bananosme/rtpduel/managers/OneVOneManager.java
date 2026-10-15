package me.bananosme.rtpduel.managers;

import me.bananosme.rtpduel.RtpDuelPlugin;
import me.bananosme.rtpduel.utils.AsyncTeleporter;
import me.bananosme.rtpduel.utils.SpawnFinder;
import me.bananosme.rtpduel.utils.TextManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class OneVOneManager {

    private final RtpDuelPlugin plugin;
    private final List<Player> queue = new ArrayList<>();
    private final Map<UUID, BossBar> queueBars = new HashMap<>();
    private final Set<UUID> respawnInSpawnWorld = ConcurrentHashMap.newKeySet();
    private final Random random = new Random();

    private record WorldSettings(World world, int border, int spawnDistance) {
    }

    public OneVOneManager(RtpDuelPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Загруженные миры дуэлей с их настройками границ спавна из конфига.
     */
    private Map<String, WorldSettings> getArenaWorlds() {
        Map<String, WorldSettings> worlds = new LinkedHashMap<>();
        int defaultBorder = plugin.getConfig().getInt("default-border", 10000);
        int defaultDistance = plugin.getConfig().getInt("default-spawn-distance", 8);
        var section = plugin.getConfig().getConfigurationSection("worlds");
        if (section == null) {
            return worlds;
        }
        for (String name : section.getKeys(false)) {
            World world = Bukkit.getWorld(name);
            if (world == null) {
                continue;
            }
            int border = plugin.getConfig().getInt("worlds." + name + ".border", defaultBorder);
            int distance = plugin.getConfig().getInt("worlds." + name + ".spawn-distance", defaultDistance);
            worlds.put(name, new WorldSettings(world, border, distance));
        }
        return worlds;
    }

    public void joinQueue(Player player) {
        TextManager lang = plugin.getTextManager();
        if (queue.contains(player)) {
            player.sendMessage(lang.message("already-in-queue"));
            return;
        }
        queue.add(player);
        player.sendMessage(lang.message("in-queue", Map.of("position", queue.indexOf(player) + 1)));
        showQueueBar(player);
        updateQueueBars();
        if (queue.size() >= 2) {
            List<Player> firstTwo = new ArrayList<>();
            int count = 0;
            java.util.Iterator<Player> it = queue.iterator();
            while (it.hasNext() && count < 2) {
                firstTwo.add(it.next());
                it.remove();
                count++;
            }
            updateQueueBars();
            startMatch(firstTwo.get(0), firstTwo.get(1));
        }
    }

    public void leaveQueue(Player player) {
        TextManager lang = plugin.getTextManager();
        if (queue.remove(player)) {
            hideQueueBar(player);
            player.sendMessage(lang.message("left-queue"));
            updateQueueBars();
        } else {
            player.sendMessage(lang.message("not-in-queue"));
        }
    }

    private void startMatch(Player a, Player b) {
        TextManager lang = plugin.getTextManager();
        Map<String, WorldSettings> worlds = getArenaWorlds();
        if (worlds.isEmpty()) {
            var section = plugin.getConfig().getConfigurationSection("worlds");
            String names = section == null ? "?" : String.join(", ", section.getKeys(false));
            a.sendMessage(lang.message("no-worlds", Map.of("worlds", names)));
            b.sendMessage(lang.message("no-worlds", Map.of("worlds", names)));
            return;
        }
        List<WorldSettings> settingsList = new ArrayList<>(worlds.values());
        WorldSettings settings = settingsList.get(random.nextInt(settingsList.size()));

        prepare(a);
        prepare(b);
        hideQueueBar(a);
        hideQueueBar(b);
        for (Player p : new Player[]{a, b}) {
            p.sendMessage(lang.message("found", Map.of("opponent", opponentOf(p, a, b).getName())));
        }

        findSpawnsAsync(a, b, settings, 0);
    }

    /**
     * Асинхронно ищет точки спавна (чанки грузятся без фриза сервера),
     * при неудаче повторяет до 3 попыток.
     */
    private void findSpawnsAsync(Player a, Player b, WorldSettings settings, int attempt) {
        SpawnFinder.findPairSpawnsAsync(plugin, settings.world(), settings.border(), settings.spawnDistance(), random)
                .thenAccept(spawns -> {
                    if ((spawns == null || spawns[1] == null) && attempt < 3) {
                        findSpawnsAsync(a, b, settings, attempt + 1);
                        return;
                    }
                    if (spawns == null || spawns[1] == null) {
                        TextManager lang = plugin.getTextManager();
                        if (a.isOnline()) {
                            a.sendMessage(lang.message("arena-not-found"));
                        }
                        if (b.isOnline()) {
                            b.sendMessage(lang.message("arena-not-found"));
                        }
                        return;
                    }
                    teleportPair(a, b, spawns[0], spawns[1]);
                });
    }

    /**
     * Прогружает чанки обеих точек и телепортирует игроков, после чего показывает титул.
     */
    private void teleportPair(Player a, Player b, Location aLocation, Location bLocation) {
        AtomicInteger pending = new AtomicInteger(2);
        Runnable done = () -> {
            if (pending.decrementAndGet() != 0) {
                return;
            }
            Duration stay = Duration.ofMillis(2500);
            Duration fade = Duration.ofMillis(500);
            for (Player p : new Player[]{a, b}) {
                if (!p.isOnline()) {
                    continue;
                }
                p.showTitle(Title.title(
                        plugin.getTextManager().message("title-go"),
                        Component.empty(),
                        Title.Times.times(Duration.ZERO, stay, fade)));
            }
        };
        AsyncTeleporter.teleport(plugin, a, aLocation).thenRun(done);
        AsyncTeleporter.teleport(plugin, b, bLocation).thenRun(done);
    }

    private Player opponentOf(Player p, Player a, Player b) {
        return p == a ? b : a;
    }

    @SuppressWarnings("deprecation")
    private void prepare(Player player) {
        try {
            player.setHealth(20.0);
        } catch (IllegalArgumentException ignored) {
            // Максимальное здоровье урезано другим плагином — лечим в пределах максимума.
            player.setHealth(player.getMaxHealth());
        }
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.setExp(0);
        player.setLevel(0);
    }

    /**
     * Смерть на арене — обычная смерть: респавн на спавне spawn-world.
     */
    public void handleDeath(Player dead) {
        if (isDuelWorld(dead.getWorld())) {
            respawnInSpawnWorld.add(dead.getUniqueId());
        }
    }

    public void handleRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!respawnInSpawnWorld.remove(player.getUniqueId())) {
            return;
        }
        String spawnWorldName = plugin.getConfig().getString("spawn-world", "");
        World world = spawnWorldName.isEmpty() ? null : Bukkit.getWorld(spawnWorldName);
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        if (world != null) {
            event.setRespawnLocation(world.getSpawnLocation());
        }
    }

    public boolean isDuelWorld(World world) {
        return getArenaWorlds().values().stream().anyMatch(s -> s.world().equals(world));
    }

    public void onPlayerQuit(Player player) {
        // Иначе UUID останется в сете навсегда и следующая смерть игрока
        // в обычном мире отправит его на спавн вместо кровати.
        respawnInSpawnWorld.remove(player.getUniqueId());
        if (queue.remove(player)) {
            hideQueueBar(player);
            updateQueueBars();
        }
    }

    /**
     * Обновляет текст босбаров очереди (позиция меняется при входе/выходе).
     */
    private void updateQueueBars() {
        var it = queueBars.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, BossBar> entry = it.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            BossBar bar = entry.getValue();
            if (player == null || !player.isOnline()) {
                bar.removeAll();
                it.remove();
                continue;
            }
            int position = queue.indexOf(player) + 1;
            if (position > 0) {
                bar.setTitle(plugin.getTextManager().messageLegacy("bossbar-queue",
                        Map.of("position", position)));
                bar.setVisible(true);
            } else {
                bar.removeAll();
                it.remove();
            }
        }
    }

    private void showQueueBar(Player player) {
        hideQueueBar(player);
        BarColor color = parseColor(plugin.getConfig().getString("bossbar-color", "GREEN"));
        BarStyle style = parseStyle(plugin.getConfig().getString("bossbar-style", "SOLID"));
        BossBar bar = Bukkit.createBossBar(
                plugin.getTextManager().messageLegacy("bossbar-queue",
                        Map.of("position", queue.indexOf(player) + 1)),
                color, style);
        bar.setProgress(1.0f);
        bar.addPlayer(player);
        queueBars.put(player.getUniqueId(), bar);
    }

    private void hideQueueBar(Player player) {
        BossBar bar = queueBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }

    private BarColor parseColor(String name) {
        try {
            return BarColor.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return BarColor.GREEN;
        }
    }

    private BarStyle parseStyle(String name) {
        try {
            return BarStyle.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return BarStyle.SOLID;
        }
    }

    public void cleanup() {
        queue.clear();
        for (BossBar bar : queueBars.values()) {
            bar.removeAll();
        }
        queueBars.clear();
        respawnInSpawnWorld.clear();
    }
}