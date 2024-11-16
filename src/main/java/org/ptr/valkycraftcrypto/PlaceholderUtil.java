package org.ptr.valkycraftcrypto;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class PlaceholderUtil {

    public static String applyPlaceholders(Player player, String text) {
        if (text == null || player == null) {
            return text;
        }
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
