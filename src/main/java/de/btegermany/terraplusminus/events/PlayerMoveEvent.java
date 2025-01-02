package de.btegermany.terraplusminus.events;

import com.alpsbte.alpslib.io.config.ConfigurationUtil;
import de.btegermany.terraplusminus.TerraSharp;
import de.btegermany.terraplusminus.utils.ChatUtils;
import de.btegermany.terraplusminus.utils.ConfigurationHelper;
import de.btegermany.terraplusminus.utils.LinkedWorld;
import de.btegermany.terraplusminus.utils.io.ConfigPaths;
import de.btegermany.terraplusminus.utils.io.ConfigUtil;
import io.papermc.lib.PaperLib;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static net.kyori.adventure.text.Component.text;

public class PlayerMoveEvent implements Listener {

    private BukkitRunnable runnable;
    private final ArrayList<Integer> taskIDs = new ArrayList<>();
    private int yOffset;
    final int yOffsetConfigEntry;

    private final int xOffset;
    private final int zOffset;
    private final boolean linkedWorldsEnabled;

    private final String linkedWorldsMethod;
    private final Plugin plugin;

    private final HashMap<String, Integer> worldHashMap;
    private List<LinkedWorld> worlds;

    public PlayerMoveEvent(Plugin plugin) {
        this.plugin = plugin;

        ConfigurationUtil.ConfigFile configFile = ConfigUtil.getInstance().configs[0];
        this.xOffset = configFile.getInt(ConfigPaths.TERRAIN_OFFSET_X);
        this.yOffsetConfigEntry = configFile.getInt(ConfigPaths.TERRAIN_OFFSET_Y);
        this.zOffset = configFile.getInt(ConfigPaths.TERRAIN_OFFSET_Z);
        this.linkedWorldsEnabled = configFile.getBoolean(ConfigPaths.LINKED_WORLDS_ENABLED);
        this.linkedWorldsMethod = configFile.getString(ConfigPaths.LINKED_WORLDS_METHOD);
        this.worldHashMap = new HashMap<>();
        if (this.linkedWorldsEnabled && Objects.requireNonNull(this.linkedWorldsMethod).equalsIgnoreCase("MULTIVERSE")) {
            this.worlds = ConfigurationHelper.getWorlds();
            for (LinkedWorld world : worlds) {
                this.worldHashMap.put(world.getWorldName(), world.getOffset());
            }

            TerraSharp.instance.getComponentLogger().info("[T#] Linked worlds enabled, using Multiverse method.");
        }
        this.startKeepActionBarAlive();
    }

    @EventHandler
    void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();
        setHeightInActionBar(player);
    }

    private void startKeepActionBarAlive() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                setHeightInActionBar(p);
            }
        }, 0, 20);
    }

    private void setHeightInActionBar(Player p) {
        worldHashMap.putIfAbsent(p.getWorld().getName(), yOffsetConfigEntry);
        if (p.getInventory().getItemInMainHand().getType() != Material.DEBUG_STICK) {
            int height = p.getLocation().getBlockY() - worldHashMap.get(p.getWorld().getName());
            p.sendActionBar(text(height + "m").decoration(TextDecoration.BOLD, true));
        }
    }

    @EventHandler
    void onPlayerFall(org.bukkit.event.player.PlayerMoveEvent event) {
        if (!this.linkedWorldsEnabled && !this.linkedWorldsMethod.equalsIgnoreCase("MULTIVERSE")) {
            return;
        }

        Player p = event.getPlayer();
        World world = p.getWorld();
        Location location = p.getLocation();

        // delayed teleporting
        new BukkitRunnable() {
            @Override
            public void run() {
                // Teleport player from world to world
                if (p.getLocation().getY() < 0) {
                    LinkedWorld previousServer = ConfigurationHelper.getPreviousServerName(world.getName());
                    if (previousServer != null) {
                        teleportPlayer(previousServer, location, p);
                    }
                } else if (p.getLocation().getY() > world.getMaxHeight()) {
                    LinkedWorld nextServer = ConfigurationHelper.getNextServerName(world.getName());
                    if (nextServer != null) {
                        teleportPlayer(nextServer, location, p);
                    }
                }
            }
        }.runTaskLater(plugin, 60L);
    }

    private void teleportPlayer(LinkedWorld linkedWorld, Location location, Player p) {
        World tpWorld = Bukkit.getWorld(linkedWorld.getWorldName());
        Location newLocation = new Location(tpWorld, location.getX() + xOffset, tpWorld.getMinHeight(), location.getZ() + zOffset, location.getYaw(), location.getPitch());
        PaperLib.teleportAsync(p, newLocation);
        p.setFlying(true);
        p.sendMessage(ChatUtils.getInfoMessage("You have been teleported to another world."));
    }
}
