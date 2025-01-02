package de.btegermany.terraplusminus;

import com.alpsbte.alpslib.io.YamlFileFactory;
import com.alpsbte.alpslib.io.config.ConfigNotImplementedException;
import de.btegermany.terraplusminus.commands.OffsetCommand;
import de.btegermany.terraplusminus.commands.TpllCommand;
import de.btegermany.terraplusminus.commands.TpsgCommand;
import de.btegermany.terraplusminus.commands.WhereCommand;
import de.btegermany.terraplusminus.events.PlayerJoinEvent;
import de.btegermany.terraplusminus.events.PlayerMoveEvent;
import de.btegermany.terraplusminus.events.PluginMessageEvent;
import de.btegermany.terraplusminus.gen.RealWorldGenerator;
import de.btegermany.terraplusminus.utils.ChatUtils;
import de.btegermany.terraplusminus.utils.ConfigurationHelper;
import de.btegermany.terraplusminus.utils.LinkedWorld;
import de.btegermany.terraplusminus.utils.PlayerHashMapManagement;
import de.btegermany.terraplusminus.utils.io.ConfigPaths;
import de.btegermany.terraplusminus.utils.io.ConfigUtil;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.buildtheearth.terraminusminus.TerraConfig;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.List;
import java.util.logging.Level;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

public final class TerraSharp extends JavaPlugin implements Listener {
    public static TerraSharp instance;

    @Override
    public void onEnable() {
        instance = this;
        PluginDescriptionFile pdf = this.getDescription();
        String pluginVersion = pdf.getVersion();

        getLogger().log(Level.INFO,
                "  _____  _  _\n" +
                        " |_   _|| || |_\n" +
                        "   | ||_  ..  _|\n" +
                        "   | ||_      _|\n" +
                        "   |_|  |_||_|\n" +
                        "TerraSharp version: " + pluginVersion);

        // --------------------------
        // Initialise Config
        try {
            YamlFileFactory.registerPlugin(this);
            ConfigUtil.init();
        } catch (ConfigNotImplementedException e) {
            this.getComponentLogger().warn(text("Could not load configuration file."));
            this.getComponentLogger().info(text("The config file must be configured!", YELLOW));
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        reloadConfig();

        // --------------------------

        // Copies osm.json5 into terraplusplus/config/
        File[] terraPlusPlusDirectories = {new File("terraplusplus"), new File("terraplusplus/config/")};
        for (File file : terraPlusPlusDirectories) {
            if (!file.exists()) {
                file.mkdir();
            }
        }
        File osmJsonFile = new File("terraplusplus" + File.separator + "config" + File.separator + "osm.json5");
        if (!osmJsonFile.exists()) {
            this.copyFileFromResource("assets/terraplusminus/data/osm.json5", osmJsonFile);
        }
        // --------------------------

        // Register plugin messaging channel
        PlayerHashMapManagement playerHashMapManagement = new PlayerHashMapManagement();
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "bungeecord:terraplusminus");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "bungeecord:terraplusminus", new PluginMessageEvent(playerHashMapManagement));
        // --------------------------

        // Registering events
        Bukkit.getPluginManager().registerEvents(this, this);
        if (ConfigUtil.getInstance().configs[0].getBoolean(ConfigPaths.HEIGHT_IN_ACTIONBAR)) {
            Bukkit.getPluginManager().registerEvents(new PlayerMoveEvent(this), this);
        }

        if (ConfigUtil.getInstance().configs[0].getBoolean(ConfigPaths.LINKED_WORLDS_ENABLED)) {
            Bukkit.getPluginManager().registerEvents(new PlayerJoinEvent(playerHashMapManagement), this);
        }
        // --------------------------

        // Disables console log of fetching data
        TerraConfig.reducedConsoleMessages = ConfigUtil.getInstance().configs[0].getBoolean(ConfigPaths.REDUCED_CONSOLE_MESSAGES);

        registerCommands();

        getComponentLogger().info("[T#] TerraSharp successfully enabled");
    }

    @Override
    public void onDisable() {
        // Unregister plugin messaging channel
        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
        // --------------------------

        getComponentLogger().info("[T#] Plugin deactivated");
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        String datapackName = "world-height-datapack.zip";
        File datapackPath = new File(event.getWorld().getWorldFolder() + File.separator + "datapacks" + File.separator + datapackName);

        if (ConfigUtil.getInstance().configs[0].getBoolean(ConfigPaths.HEIGHT_DATAPACK)) {
            if (!event.getWorld().getName().contains("_nether") && !event.getWorld().getName().contains("_the_end")) { //event.getWorld().getGenerator() is null here
                if (!datapackPath.exists()) {
                    copyFileFromResource(datapackName, datapackPath);
                }
            }
        }
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, String id) {
        // Multiverse different y-offset support
        int yOffset = 0;

        boolean linkedWorldsEnabled = ConfigUtil.getInstance().configs[0].getBoolean(ConfigPaths.LINKED_WORLDS_ENABLED);
        String linkedWorldsMethod = ConfigUtil.getInstance().configs[0].getString(ConfigPaths.LINKED_WORLDS_METHOD);
        if (linkedWorldsEnabled && linkedWorldsMethod.equalsIgnoreCase("MULTIVERSE")) {
            for (LinkedWorld world : ConfigurationHelper.getWorlds()) {
                if (world.getWorldName().equalsIgnoreCase(worldName)) {
                    yOffset = world.getOffset();
                }
            }
        } else {
            yOffset = ConfigUtil.getInstance().configs[0].getInt(ConfigPaths.TERRAIN_OFFSET_Y);
        }
        return new RealWorldGenerator(yOffset);
    }


    public void copyFileFromResource(String resourcePath, File destination) {
        InputStream in = getResource(resourcePath);
        OutputStream out;
        try {
            out = new FileOutputStream(destination);
        } catch (FileNotFoundException e) {
            getComponentLogger().error("[T#] " + destination.getName() + " not found");
            throw new RuntimeException(e);
        }
        byte[] buf = new byte[1024];
        int len;
        try {
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException io) {
            getComponentLogger().error("[T#] Could not copy " + destination, io);
        } finally {
            try {
                out.close();
                if (resourcePath.equals("world-height-datapack.zip")) {
                    getComponentLogger().info("[T+-] Copied datapack to world folder");
                    getComponentLogger().warn("[T+-] Stopping server to start again with datapack");
                    Bukkit.getServer().shutdown();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void reloadConfig() {
        ConfigUtil.getInstance().reloadFiles();
        ConfigUtil.getInstance().saveFiles();
        ChatUtils.setChatFormat(
                ConfigUtil.getInstance().configs[0].getString(ConfigPaths.CHAT_FORMAT_INFO_PREFIX),
                ConfigUtil.getInstance().configs[0].getString(ConfigPaths.CHAT_FORMAT_ALERT_PREFIX)
        );
    }

    private void registerCommands() {
        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register("tpll", "Teleports you to longitude and latitude", List.of("tpc"), new TpllCommand());
            commands.register("tpsg", "Teleports you to LV95 swiss grid coordinates", new TpsgCommand());
            commands.register("where", "Gives you the longitude and latitude of your minecraft coordinates", new WhereCommand());
            commands.register("offset", "Displays the x,y and z offset of your world", new OffsetCommand());
        });
    }
}