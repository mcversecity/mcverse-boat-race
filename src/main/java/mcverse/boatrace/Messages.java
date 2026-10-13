package mcverse.boatrace;

import org.bukkit.ChatColor;

public final class Messages {
    private static final String PREFIX = ChatColor.AQUA + "[BoatRace] ";

    private Messages() {
    }

    public static String text(String body) {
        return PREFIX + ChatColor.GRAY + body;
    }

    public static String ok(String body) {
        return PREFIX + ChatColor.GREEN + body;
    }

    public static String error(String body) {
        return PREFIX + ChatColor.RED + body;
    }

    public static String place(int place) {
        int mod100 = place % 100;
        if (mod100 >= 11 && mod100 <= 13) {
            return place + "th";
        }
        return switch (place % 10) {
            case 1 -> place + "st";
            case 2 -> place + "nd";
            case 3 -> place + "rd";
            default -> place + "th";
        };
    }
}
