package de.btegermany.terraplusminus.commands;

import de.btegermany.terraplusminus.Terraplusminus;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

public class TpsgCommand implements BasicCommand {
    @Override
    public void execute(CommandSourceStack stack, String[] strings) {
        if (!(stack.getSender() instanceof Player player)) return;
        if (!player.hasPermission("t+-.tpll")) {
            player.sendMessage(Terraplusminus.config.getString("prefix") + "§7No permission for /tpll");
            return;
        }

        if (strings.length < 2 || strings.length > 3) return;

        int easting = Integer.parseInt(strings[0]
                .replace("’", "")
                .replace(",", ""));
        int northing = Integer.parseInt(strings[strings.length - 1]
                .replace("’", "")
                .replace(",", ""));

        double eDiff = ((double) easting - 2600000) / 1000000; // y'
        double nDiff = ((double) northing - 1200000) / 1000000; // x'

        double longitude = 2.6779094 +
                (4.728982 * eDiff) +
                (0.791484 * eDiff * nDiff) +
                (0.1306 * eDiff * Math.pow(nDiff, 2)) -
                (0.0436 * Math.pow(eDiff, 3));

        double latitude = 16.9023892 +
                (3.238272 * nDiff) -
                (0.270978 * Math.pow(eDiff, 2)) -
                (0.002528 * Math.pow(nDiff, 2)) -
                (0.0447 * Math.pow(eDiff, 2) * nDiff) -
                (0.0140 * Math.pow(nDiff, 3));

        player.performCommand("tpll " + (latitude * 100 / 36) + " " + (longitude * 100 / 36));
    }
}
