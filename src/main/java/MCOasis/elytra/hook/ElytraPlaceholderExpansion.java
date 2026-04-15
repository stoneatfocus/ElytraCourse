package MCOasis.elytra.hook;

import MCOasis.elytra.Elytra;
import MCOasis.elytra.manager.RunManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ElytraPlaceholderExpansion extends PlaceholderExpansion {

    private final Elytra plugin;

    public ElytraPlaceholderExpansion(Elytra plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "elytracourse";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MCOasis";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return null;

        // %elytracourse_last_time%
        if (params.equals("last_time")) {
            long time = plugin.getRunManager().getLastTime(player.getUniqueId());
            return RunManager.formatTime(time);
        }

        // %elytracourse_best_time%
        // Handles current world context naturally if player is online.
        // Support explicit world: %elytracourse_best_time_<world>%
        if (params.startsWith("best_time")) {
            String worldName = null;
            if (params.equals("best_time")) {
                if (player.isOnline()) {
                   worldName = ((Player) player).getWorld().getName();
                }
            } else if (params.startsWith("best_time_")) {
                worldName = params.substring("best_time_".length());
            }

            if (worldName != null) {
                long time = plugin.getCourseManager().getBestTime(player.getUniqueId(), worldName);
                if (time == -1) return "--:--";
                return RunManager.formatTime(time);
            }
        }

        // %elytracourse_top_name_<rank>_<world>%
        // %elytracourse_top_time_<rank>_<world>%
        if (params.startsWith("top_")) {
            String[] parts = params.split("_");
            // Expecting: top, name/time, rank, world...
            // parts[0] = top
            // parts[1] = name or time
            // parts[2] = rank (1-based)
            // parts[3+] = world name (might contain underscores, so join rest)

            if (parts.length >= 4) {
                String type = parts[1];
                int rank;
                try {
                    rank = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    return null;
                }

                // Reassemble world name in case it contains underscores
                StringBuilder w = new StringBuilder();
                for (int i = 3; i < parts.length; i++) {
                    if (i > 3) w.append("_");
                    w.append(parts[i]);
                }
                String worldName = w.toString();

                var leaderboard = plugin.getCourseManager().getLeaderboard(worldName);
                if (rank > leaderboard.size() || rank < 1) {
                    return "---";
                }

                var entry = leaderboard.get(rank - 1);

                if (type.equals("name")) {
                    return org.bukkit.Bukkit.getOfflinePlayer(entry.getKey()).getName();
                }
                if (type.equals("time")) {
                    return RunManager.formatTime(entry.getValue());
                }
            }
        }

        return null;
    }
}
