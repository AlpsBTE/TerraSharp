package de.btegermany.terraplusminus.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

public class ChatUtils {
    private static Component infoPrefix, alertPrefix;

    public static void setChatFormat(String infoPrefix, String alertPrefix) {
        ChatUtils.infoPrefix = MiniMessage.miniMessage().deserialize(infoPrefix);
        ChatUtils.alertPrefix = MiniMessage.miniMessage().deserialize(alertPrefix);
    }

    public static Component getInfoMessage(String message) {
        return ChatUtils.infoPrefix.append(MiniMessage.miniMessage()
                .deserialize(message)
                .color(GRAY));
    }

    public static Component getAlertMessage(String message) {
        return ChatUtils.alertPrefix.append(MiniMessage.miniMessage()
                .deserialize(message)
                .color(RED));
    }
}
