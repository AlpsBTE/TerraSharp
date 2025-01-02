package de.btegermany.terraplusminus.commands;

import com.alpsbte.alpslib.io.config.ConfigurationUtil;
import de.btegermany.terraplusminus.utils.ChatUtils;
import de.btegermany.terraplusminus.utils.ConfigurationHelper;
import de.btegermany.terraplusminus.utils.io.ConfigPaths;
import de.btegermany.terraplusminus.utils.io.ConfigUtil;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public class OffsetCommand implements BasicCommand {
    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (stack.getSender() instanceof Player player) {
            if (!player.hasPermission("t+-.offset")) {
                player.sendMessage(ChatUtils.getAlertMessage("No permission for /offset"));
                return;
            }

            ConfigurationUtil.ConfigFile configFile = ConfigUtil.getInstance().configs[0];

            player.sendMessage(ChatUtils.getInfoMessage("Offsets:"));
            player.sendMessage(ChatUtils.getInfoMessage("| X: <dark_gray>" + configFile.getInt(ConfigPaths.TERRAIN_OFFSET_X) + "</dark_gray>"));

            boolean isLinkedWorldsEnabled = configFile.getBoolean(ConfigPaths.LINKED_WORLDS_ENABLED);
            boolean isMultiverse = Objects.requireNonNull(configFile.getString(ConfigPaths.LINKED_WORLDS_METHOD))
                    .equalsIgnoreCase("MULTIVERSE");

            if (!isLinkedWorldsEnabled || !isMultiverse) {
                player.sendMessage(ChatUtils.getInfoMessage("| Y: <dark_gray>" + configFile.getInt(ConfigPaths.TERRAIN_OFFSET_Y) + "</dark_gray>"));
            } else {
                ConfigurationHelper.getWorlds().forEach(world ->
                        player.sendMessage(ChatUtils.getInfoMessage(world.getWorldName() + " | Y: <dark_gray>" + world.getOffset() + "</dark_gray>")));
            }

            player.sendMessage(ChatUtils.getInfoMessage("| Z: <dark_gray>" + configFile.getInt(ConfigPaths.TERRAIN_OFFSET_Z) + "</dark_gray>"));
        }
    }
}
