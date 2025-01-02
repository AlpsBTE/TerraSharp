package de.btegermany.terraplusminus.commands;

import com.alpsbte.alpslib.io.config.ConfigurationUtil;
import de.btegermany.terraplusminus.utils.ChatUtils;
import de.btegermany.terraplusminus.utils.io.ConfigPaths;
import de.btegermany.terraplusminus.utils.io.ConfigUtil;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;

@SuppressWarnings("UnstableApiUsage")
public class WhereCommand implements BasicCommand {

    private final EarthGeneratorSettings bteGeneratorSettings = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS);

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage("This command can only be used by players!");
            return;
        }
        if (!player.hasPermission("t+-.where")) {
            player.sendMessage(ChatUtils.getAlertMessage("No permission for /where"));
            return;
        }

        ConfigurationUtil.ConfigFile configFile = ConfigUtil.getInstance().configs[0];
        int xOffset = configFile.getInt(ConfigPaths.TERRAIN_OFFSET_X);
        int zOffset = configFile.getInt(ConfigPaths.TERRAIN_OFFSET_Z);

        double[] mcCoordinates = new double[2];
        mcCoordinates[0] = player.getLocation().getX() - xOffset;
        mcCoordinates[1] = player.getLocation().getZ() - zOffset;
        System.out.println(mcCoordinates[0] + ", " + mcCoordinates[1]);
        double[] coordinates = new double[0];
        try {
            coordinates = bteGeneratorSettings.projection().toGeo(mcCoordinates[0], mcCoordinates[1]);
        } catch (OutOfProjectionBoundsException e) {
            e.printStackTrace();
        }

        player.sendMessage(ChatUtils.getInfoMessage("Your coordinates are:"));
        player.sendMessage(text(coordinates[1] + ", " + coordinates[0], DARK_GRAY)
                .clickEvent(ClickEvent.openUrl("https://maps.google.com/maps?t=k&q=loc:" + coordinates[1] + "+" + coordinates[0]))
                .hoverEvent(HoverEvent.showText(text("Click here to view in Google Maps.", GRAY))));
    }
}
