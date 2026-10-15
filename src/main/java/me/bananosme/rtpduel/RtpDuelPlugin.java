package me.bananosme.rtpduel;

import me.bananosme.rtpduel.bstats.Metrics;
import me.bananosme.rtpduel.commands.OneVOneCommand;
import me.bananosme.rtpduel.commands.RtpCommand;
import me.bananosme.rtpduel.listeners.PlayerListener;
import me.bananosme.rtpduel.managers.OneVOneManager;
import me.bananosme.rtpduel.managers.RtpManager;
import me.bananosme.rtpduel.utils.TextManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class RtpDuelPlugin extends JavaPlugin {

    /** ID плагина на bStats: https://bstats.org/what-is-my-plugin-id */
    private static final int BSTATS_PLUGIN_ID = 34705;

    private static RtpDuelPlugin instance;

    private TextManager textManager;
    private OneVOneManager oneVOneManager;
    private RtpManager rtpManager;
    private Metrics metrics;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        textManager = new TextManager(this);
        oneVOneManager = new OneVOneManager(this);
        rtpManager = new RtpManager(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(rtpManager, this);

        OneVOneCommand executor = new OneVOneCommand(this);
        for (String name : new String[]{"1v1", "1v1_leave"}) {
            PluginCommand command = getCommand(name);
            if (command != null) {
                command.setExecutor(executor);
            }
        }

        RtpCommand rtpExecutor = new RtpCommand(this);
        PluginCommand rtpCommand = getCommand("rtp");
        if (rtpCommand != null) {
            rtpCommand.setExecutor(rtpExecutor);
        }

        metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        metrics.addCustomChart(new Metrics.SimplePie("arena_worlds", this::arenaWorldsChart));

        getLogger().info("1v1RtpDuel включен. Автор: Bananosme_Play");
    }

    @Override
    public void onDisable() {
        oneVOneManager.cleanup();
        rtpManager.cleanup();
        if (metrics != null) {
            metrics.shutdown();
        }
        getLogger().info("1v1RtpDuel выключен.");
    }

    /**
     * Кастомный график bStats: сколько миров-арен настроено в конфиге.
     * id графика — "arena_worlds" (создаётся/наблюдается в панели bStats).
     */
    private String arenaWorldsChart() {
        var section = getConfig().getConfigurationSection("worlds");
        int count = section == null ? 0 : section.getKeys(false).size();
        return count <= 0 ? null : String.valueOf(count);
    }

    public static RtpDuelPlugin getInstance() {
        return instance;
    }

    public TextManager getTextManager() {
        return textManager;
    }

    public OneVOneManager getOneVOneManager() {
        return oneVOneManager;
    }

    public RtpManager getRtpManager() {
        return rtpManager;
    }
}