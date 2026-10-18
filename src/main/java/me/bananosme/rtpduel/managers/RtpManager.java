package me.bananosme.rtpduel.managers;

import me.bananosme.rtpduel.RtpDuelPlugin;
import me.bananosme.rtpduel.utils.AsyncTeleporter;
import me.bananosme.rtpduel.utils.SpawnFinder;
import me.bananosme.rtpduel.utils.TextManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Случайная телепортация (/rtp): открывает кастомизируемый GUI выбора мира,
 * при клике телепортирует игрока в случайную точку выбранного мира.
 */
public final class RtpManager implements Listener {

    private static final Material DEFAULT_MATERIAL = Material.GRASS_BLOCK;

    private final RtpDuelPlugin plugin;
    private final Map<UUID, Inventory> openGuis = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, String>> guiWorlds = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public RtpManager(RtpDuelPlugin plugin) {
        this.plugin = plugin;
    }

    public void openGui(Player player) {
        TextManager lang = plugin.getTextManager();
        Inventory inventory = Bukkit.createInventory(null, guiSize(),
                lang.parse(plugin.getConfig().getString("rtp.gui.title", "<green>Select a world for RTP")));

        Map<Integer, String> slots = new LinkedHashMap<>();
        for (String name : worldNames()) {
            World world = Bukkit.getWorld(name);
            if (world == null) {
                continue;
            }
            int slot = nextFreeSlot(name, slots, inventory.getSize());
            slots.put(slot, name);
            inventory.setItem(slot, worldItem(name, world));
        }

        if (slots.isEmpty()) {
            player.sendMessage(lang.message("rtp-no-worlds"));
            return;
        }

        fillFiller(inventory, slots.keySet());

        player.openInventory(inventory);
        openGuis.put(player.getUniqueId(), inventory);
        guiWorlds.put(player.getUniqueId(), slots);
    }

    /**
     * Имена миров для GUI: из секции rtp.gui.worlds, либо из worlds (если gui пуст).
     */
    private List<String> worldNames() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("rtp.gui.worlds");
        if (section != null && !section.getKeys(false).isEmpty()) {
            return new ArrayList<>(section.getKeys(false));
        }
        ConfigurationSection worlds = plugin.getConfig().getConfigurationSection("worlds");
        return worlds == null ? List.of() : new ArrayList<>(worlds.getKeys(false));
    }

    /**
     * Отображаемое имя мира из конфига (rtp.gui.worlds.<key>.name),
     * иначе — сам ключ/имя мира.
     */
    private String displayName(String key) {
        return plugin.getConfig().getString("rtp.gui.worlds." + key + ".name", key);
    }

    private int guiSize() {
        int size = plugin.getConfig().getInt("rtp.gui.size", 27);
        if (size < 9) {
            return 9;
        }
        if (size > 54) {
            return 54;
        }
        return size - size % 9;
    }

    private int nextFreeSlot(String name, Map<Integer, String> slots, int size) {
        int slot = plugin.getConfig().getInt("rtp.gui.worlds." + name + ".slot", -1);
        if (slot >= 0 && slot < size && !slots.containsKey(slot)) {
            return slot;
        }
        for (int i = 0; i < size; i++) {
            if (!slots.containsKey(i)) {
                return i;
            }
        }
        return 0;
    }

    private ItemStack worldItem(String name, World world) {
        String path = "rtp.gui.worlds." + name;
        Material material = Material.matchMaterial(plugin.getConfig().getString(path + ".material", ""));
        if (material == null) {
            material = DEFAULT_MATERIAL;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.getTextManager().parse(plugin.getConfig().getString(path + ".name", name)));
        List<Component> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList(path + ".lore")) {
            lore.add(plugin.getTextManager().parse(line));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void fillFiller(Inventory inventory, Set<Integer> usedSlots) {
        if (!plugin.getConfig().getBoolean("rtp.gui.filler.enabled", true)) {
            return;
        }
        String materialName = plugin.getConfig().getString("rtp.gui.filler.material", "GRAY_STAINED_GLASS_PANE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            return;
        }
        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(plugin.getTextManager().parse(
                plugin.getConfig().getString("rtp.gui.filler.name", " ")));
        filler.setItemMeta(meta);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (!usedSlots.contains(i)) {
                inventory.setItem(i, filler);
            }
        }
    }

    public void teleport(Player player, String worldName) {
        TextManager lang = plugin.getTextManager();
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(lang.message("rtp-world-unavailable", Map.of("world", displayName(worldName))));
            return;
        }
        int border = plugin.getConfig().getInt("worlds." + worldName + ".border",
                plugin.getConfig().getInt("default-border", 10000));
        findSpawnAsync(player, world, border, 0);
    }

    /**
     * Асинхронно ищет точку спавна (чанк грузится без фриза сервера),
     * при неудаче повторяет до 3 попыток.
     */
    private void findSpawnAsync(Player player, World world, int border, int attempt) {
        SpawnFinder.findGroundAsync(plugin, world, border, random).thenAccept(ground -> {
            if (ground == null && attempt < 3) {
                findSpawnAsync(player, world, border, attempt + 1);
                return;
            }
            if (ground == null) {
                player.sendMessage(plugin.getTextManager().message("rtp-not-found"));
                return;
            }
            Location location = SpawnFinder.spawnLocation(ground);
            player.closeInventory();
            AsyncTeleporter.teleport(plugin, player, location).thenRun(() ->
                    player.sendMessage(plugin.getTextManager().message("rtp-teleported",
                            Map.of("x", location.getBlockX(),
                                    "y", location.getBlockY(),
                                    "z", location.getBlockZ()))));
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory gui = openGuis.get(player.getUniqueId());
        if (gui == null || !gui.equals(event.getInventory())) {
            return;
        }
        // Отменяем любые действия с GUI: click, shift-click и перекладывание
        // из инвентаря игрока (getInventory() — верхний инвентарь открытого view).
        event.setCancelled(true);
        if (!gui.equals(event.getClickedInventory())) {
            return;
        }
        String worldName = guiWorlds.getOrDefault(player.getUniqueId(), Map.of()).get(event.getSlot());
        if (worldName != null) {
            teleport(player, worldName);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory gui = openGuis.get(player.getUniqueId());
        if (gui == null || !gui.equals(event.getInventory())) {
            return;
        }
        int topSize = gui.getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        openGuis.remove(event.getPlayer().getUniqueId());
        guiWorlds.remove(event.getPlayer().getUniqueId());
    }

    public void cleanup() {
        for (Inventory gui : openGuis.values()) {
            if (gui.getViewers().isEmpty()) {
                continue;
            }
            // Копия списка: closeInventory() синхронно меняет viewers (CME иначе).
            for (var viewer : new ArrayList<>(gui.getViewers())) {
                if (viewer instanceof Player player) {
                    player.closeInventory();
                }
            }
        }
        openGuis.clear();
        guiWorlds.clear();
    }
}