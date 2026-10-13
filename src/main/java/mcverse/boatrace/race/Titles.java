package mcverse.boatrace.race;

import mcverse.boatrace.Messages;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class Titles {
    private Titles() {
    }

    public static void countdown(Player player, int number) {
        player.sendTitle(ChatColor.GOLD.toString() + number, ChatColor.YELLOW + "Get ready", 0, 16, 4);
    }

    public static void start(Player player) {
        player.sendTitle(ChatColor.GREEN + "START", ChatColor.YELLOW + "Go!", 0, 20, 10);
    }

    public static void place(Player player, int place) {
        player.sendTitle(ChatColor.GOLD + Messages.place(place), ChatColor.YELLOW + "Finished", 5, 40, 10);
    }

    public static void action(Player player, String text) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));
    }
}
