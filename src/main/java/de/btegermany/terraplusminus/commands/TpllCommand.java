package de.btegermany.terraplusminus.commands;

import com.alpsbte.alpslib.io.config.ConfigurationUtil;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.btegermany.terraplusminus.TerraSharp;
import de.btegermany.terraplusminus.data.TerraConnector;
import de.btegermany.terraplusminus.gen.RealWorldGenerator;
import de.btegermany.terraplusminus.utils.ChatUtils;
import de.btegermany.terraplusminus.utils.ConfigurationHelper;
import de.btegermany.terraplusminus.utils.LinkedWorld;
import de.btegermany.terraplusminus.utils.io.ConfigPaths;
import de.btegermany.terraplusminus.utils.io.ConfigUtil;
import io.papermc.lib.PaperLib;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.text;

@SuppressWarnings("UnstableApiUsage")
public class TpllCommand implements BasicCommand {
    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage(text("This command can only be used by players!"));
            return;
        }

        if (!player.hasPermission("t+-.tpll")) {
            player.sendMessage(ChatUtils.getAlertMessage("No permission for /tpll"));
            return;
        }

        if (args.length == 0)  {
            sendUsage(player);
            return;
        }

        // force TPLL
        // TODO: make separate command
        if ((args[0].startsWith("@") || !isDouble(args[0].replace(",", "").replace("°", ""))) && player.hasPermission("t+-.forcetpll")) {
            if (args[0].equals("@a")) {
                StringBuilder playerList = new StringBuilder();
                TerraSharp.instance.getServer().getOnlinePlayers().forEach(p -> {
                    p.chat("/tpll " + String.join(" ", args).substring(2));
                    if (TerraSharp.instance.getServer().getOnlinePlayers().size() > 1) {
                        playerList.append(p.getName()).append(", ");
                    } else {
                        playerList.append(p.getName()).append(" ");
                    }
                });
                // delete last comma if no player follows
                if (!playerList.isEmpty() && playerList.charAt(playerList.length() - 2) == ',') {
                    playerList.deleteCharAt(playerList.length() - 2);
                }
                player.sendMessage(ChatUtils.getInfoMessage("Teleported <blue>" + playerList + "</blue> to" + String.join(" ", args).substring(2)));
                return;
            } else if (args[0].equals("@p")) {
                // find nearest player but not the player itself
                Player nearestPlayer = null;
                double nearestDistance = Double.MAX_VALUE;
                for (Player p : TerraSharp.instance.getServer().getOnlinePlayers()) {
                    if (p.getLocation().distanceSquared(player.getLocation()) < nearestDistance && (!p.equals(player) || TerraSharp.instance.getServer().getOnlinePlayers().size() == 1)) {
                        nearestPlayer = p;
                        nearestDistance = p.getLocation().distanceSquared(player.getLocation());
                    }
                }
                if (nearestPlayer != null) {
                    player.sendMessage(ChatUtils.getInfoMessage("Teleported <blue>" + nearestPlayer.getName() + "</blue> to" + String.join(" ", args).substring(2)));
                    nearestPlayer.chat("/tpll " + String.join(" ", args).substring(2));
                }
                return;
            } else {
                Player target = null;
                //check if target player is online
                for (Player p : TerraSharp.instance.getServer().getOnlinePlayers()) {
                    if (p.getName().equals(args[0])) {
                        target = p;
                    }
                }

                if (target == null) {
                    player.sendMessage(ChatUtils.getAlertMessage("No player found with name <blue>" + args[0] + "</blue>"));
                    return;
                }

                player.sendMessage(ChatUtils.getInfoMessage("Teleported <blue>" + target.getName() + "</blue> to " + args[1] + " " + args[2]));
                target.chat("/tpll " + String.join(" ", args).replace(target.getName(), ""));
                return;
            }
        }

        // pass through tpll to other bukkit plugins.
        String passthroughTpll = ConfigUtil.getInstance().configs[0].getString(ConfigPaths.PASSTHROUGH_TPLL);

        if (passthroughTpll != null && !passthroughTpll.isEmpty()) {
            player.chat("/" + passthroughTpll + ":tpll " + String.join(" ", args));
            return;
        }

        // -
        if (args.length < 2) return;

        World tpWorld = player.getWorld();

        ConfigurationUtil.ConfigFile configFile = ConfigUtil.getInstance().configs[0];

        int xOffset = configFile.getInt(ConfigPaths.TERRAIN_OFFSET_X);
        int zOffset = configFile.getInt(ConfigPaths.TERRAIN_OFFSET_Z);
        double minLat = configFile.getDouble(ConfigPaths.MIN_LATITUDE);
        double maxLat = configFile.getDouble(ConfigPaths.MAX_LATITUDE);
        double minLon = configFile.getDouble(ConfigPaths.MIN_LONGITUDE);
        double maxLon = configFile.getDouble(ConfigPaths.MAX_LONGITUDE);

        double[] coordinates = new double[2];
        try {
            coordinates[1] = Double.parseDouble(args[0].replace(",", "").replace("°", ""));
            coordinates[0] = Double.parseDouble(args[1].replace("°", ""));
        } catch (NumberFormatException e) {
            sendUsage(player);
            return;
        }

        ChunkGenerator generator = tpWorld.getGenerator();
        if (!(generator instanceof RealWorldGenerator terraGenerator)) { // after server reload the generator isn't instanceof RealWorldGenerator anymore
            player.sendMessage(ChatUtils.getAlertMessage("The world generator must be set to Terraplusminus!"));
            return;
        }
        EarthGeneratorSettings generatorSettings = terraGenerator.getSettings();
        GeographicProjection projection = generatorSettings.projection();
        int yOffset = terraGenerator.getYOffset();

        double[] mcCoordinates;
        try {
            mcCoordinates = projection.fromGeo(coordinates[0], coordinates[1]);
        } catch (OutOfProjectionBoundsException e) {
            player.sendMessage(ChatUtils.getAlertMessage("Location is not within projection bounds!"));
            return;
        }

        if (minLat != 0 && maxLat != 0 && minLon != 0 && maxLon != 0 && !player.hasPermission("t+-.admin")) {
            if (coordinates[1] < minLat || coordinates[0] < minLon || coordinates[1] > maxLat || coordinates[0] > maxLon) {
                player.sendMessage(ChatUtils.getAlertMessage("You cannot tpll to these coordinates, because this area is being worked on by another build team."));
                return;
            }
        }

        TerraConnector terraConnector = new TerraConnector();

        double height;
        if (args.length >= 3) {
            height = Double.parseDouble(args[2]) + yOffset;
        } else {
            height = terraConnector.getHeight((int) mcCoordinates[0], (int) mcCoordinates[1]).join() + yOffset; // 57 + (-2032) = -1975
        }
        if (height > player.getWorld().getMaxHeight()) {
            boolean isLinkedWorldsEnabled = configFile.getBoolean(ConfigPaths.LINKED_WORLDS_ENABLED);
            if (!isLinkedWorldsEnabled) {
                player.sendMessage(ChatUtils.getAlertMessage("You cannot tpll to these coordinates, because the world is not high enough at the moment."));
                return;
            }

            String linkedWorldsMethod = configFile.getString(ConfigPaths.LINKED_WORLDS_METHOD);
            if (linkedWorldsMethod.equalsIgnoreCase("SERVER")) {
                // send player uuid and coordinates to bungee
                sendPluginMessageToBungeeBridge(true, player, coordinates);
                return;
            } else if (linkedWorldsMethod.equalsIgnoreCase("MULTIVERSE")) {
                LinkedWorld nextServer = ConfigurationHelper.getNextServerName(player.getWorld().getName());
                if (nextServer == null) {
                    player.sendMessage(ChatUtils.getAlertMessage("You cannot tpll to these coordinates, because the world is not high enough at the moment."));
                    return;
                }
                tpWorld = Bukkit.getWorld(nextServer.getWorldName());
                height = height - yOffset + nextServer.getOffset();

                player.sendMessage(ChatUtils.getInfoMessage("Teleporting to " + coordinates[1] + ", " + coordinates[0] + " in another world. This may take a bit..."));
                PaperLib.teleportAsync(player, new Location(tpWorld, mcCoordinates[0] + xOffset, height, mcCoordinates[1] + zOffset, player.getLocation().getYaw(), player.getLocation().getPitch()));
                return;
            }
        } else if (height <= player.getWorld().getMinHeight()) {
            if (!ConfigUtil.getInstance().configs[0].getBoolean(ConfigPaths.LINKED_WORLDS_ENABLED)) {
                player.sendMessage(ChatUtils.getAlertMessage("You cannot tpll to these coordinates, because the world is not low enough at the moment."));
                return;
            }

            String method = ConfigUtil.getInstance().configs[0].getString(ConfigPaths.LINKED_WORLDS_METHOD);
            if (method.equalsIgnoreCase("SERVER")) {
                // send player uuid and coordinates to bungee
                sendPluginMessageToBungeeBridge(false, player, coordinates);
                return;
            } else if (method.equalsIgnoreCase("MULTIVERSE")) {
                LinkedWorld previousServer = ConfigurationHelper.getPreviousServerName(player.getWorld().getName());
                if (previousServer == null) {
                    player.sendMessage(ChatUtils.getAlertMessage("You cannot tpll to these coordinates, because the world is not low enough at the moment."));
                    return;
                }
                tpWorld = Bukkit.getWorld(previousServer.getWorldName());
                height = height - yOffset + previousServer.getOffset();
                player.sendMessage(ChatUtils.getInfoMessage("Teleporting to " + coordinates[1] + ", " + coordinates[0] + " in another world. This may take a bit..."));
                PaperLib.teleportAsync(player, new Location(tpWorld, mcCoordinates[0] + xOffset, height, mcCoordinates[1] + zOffset, player.getLocation().getYaw(), player.getLocation().getPitch()));
                return;
            }
        }
        Location location = new Location(tpWorld, mcCoordinates[0], height, mcCoordinates[1], player.getLocation().getYaw(), player.getLocation().getPitch());

        if (PaperLib.isChunkGenerated(location)) {
            if (args.length >= 3) {
                location = new Location(tpWorld, mcCoordinates[0], height, mcCoordinates[1], player.getLocation().getYaw(), player.getLocation().getPitch());
            } else {
                location = new Location(tpWorld, mcCoordinates[0], tpWorld.getHighestBlockYAt((int) mcCoordinates[0], (int) mcCoordinates[1]) + 1, mcCoordinates[1], player.getLocation().getYaw(), player.getLocation().getPitch());
            }
        } else {
            player.sendMessage(ChatUtils.getInfoMessage("Location is generating. Please wait a moment..."));
        }
        PaperLib.teleportAsync(player, location);


        if (args.length >= 3) {
            player.sendMessage(ChatUtils.getInfoMessage("Teleported to " + coordinates[1] + ", " + coordinates[0] + ", " + height + "."));
        } else {
            player.sendMessage(ChatUtils.getInfoMessage("Teleported to " + coordinates[1] + ", " + coordinates[0] + "."));
        }
    }

    private static void sendUsage(Player player) {
        player.sendMessage(ChatUtils.getAlertMessage("Invalid coordinates or command usage!"));
        player.sendMessage(ChatUtils.getAlertMessage("Proper usage: /tpll <latitude> <longitude> [height (optional]"));
    }

    private static boolean sendPluginMessageToBungeeBridge(boolean isNextServer, Player player, double[] coordinates) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(player.getUniqueId().toString());
        LinkedWorld server;
        if (isNextServer) {
            server = ConfigurationHelper.getNextServerName(Bukkit.getServer().getName()); //TODO: Bukkit.getServer().getName() does not return the real name
        } else {
            server = ConfigurationHelper.getPreviousServerName(Bukkit.getServer().getName()); //TODO: Bukkit.getServer().getName() does not return the real name
        }

        if (server != null) {
            out.writeUTF(server.getWorldName() + ", " + server.getOffset());
        } else {
            player.sendMessage(ChatUtils.getAlertMessage("Please contact server administrator. Your config is not set up correctly."));
            return true;
        }
        out.writeUTF(coordinates[1] + ", " + coordinates[0]);
        player.sendPluginMessage(TerraSharp.instance, "bungeecord:terraplusminus", out.toByteArray());
        player.sendMessage(ChatUtils.getAlertMessage("Sending to another server..."));
        return true;
    }

    private boolean isDouble(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
