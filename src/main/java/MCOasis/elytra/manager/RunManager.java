package MCOasis.elytra.manager;

import MCOasis.elytra.Elytra;
import MCOasis.elytra.object.CourseData;
import MCOasis.elytra.object.RunState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RunManager {

    private final Elytra plugin;
    private final CourseManager courseManager;
    private final Map<UUID, RunState> activeRuns = new HashMap<>();
    private final Map<UUID, Long> lastTimes = new HashMap<>(); // Transient last times
    private BukkitTask timerTask;

    public RunManager(Elytra plugin, CourseManager courseManager) {
        this.plugin = plugin;
        this.courseManager = courseManager;
    }

    public void start() {
        int period = plugin.getConfig().getInt("options.actionbar-update-period", 2);
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, period);
    }

    public void stop() {
        if (timerTask != null) {
            timerTask.cancel();
        }
        activeRuns.clear();
    }

    public boolean isRunning(Player player) {
        return activeRuns.containsKey(player.getUniqueId());
    }

    public RunState getRunState(Player player) {
        return activeRuns.get(player.getUniqueId());
    }

    public void startRun(Player player, CourseData course) {
        // Silent reset if already running to allow instant restart
        if (isRunning(player)) {
            activeRuns.remove(player.getUniqueId());
        }

        // Store chest item
        ItemStack chestItem = player.getInventory().getChestplate();
        ItemStack stored = null;
        if (plugin.getConfig().getBoolean("options.restore-chest-item", true)) {
            stored = chestItem != null ? chestItem.clone() : null;
        }

        // Teleport to start
        player.teleport(course.getStartLocation());

        // Equip Elytra (Unbreakable)
        ItemStack elytra = new ItemStack(Material.ELYTRA);
        ItemMeta meta = elytra.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            elytra.setItemMeta(meta);
        }
        player.getInventory().setChestplate(elytra);

        // Start state
        RunState state = new RunState(player.getWorld().getName(), stored);
        activeRuns.put(player.getUniqueId(), state);

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
    }

    public void resetRun(Player player, CourseData course) {
        RunState state = activeRuns.remove(player.getUniqueId());
        if (state == null) return;

        // Teleport to reset
        player.teleport(course.getResetLocation());

        // Clear velocity
        player.setVelocity(new Vector(0, 0, 0));
        player.setFallDistance(0);
        player.setGliding(false);

        // Restore item
        if (state.getStoredChestItem() != null) {
            player.getInventory().setChestplate(state.getStoredChestItem());
        } else {
            // Remove the elytra if it was the one we equipped
            ItemStack current = player.getInventory().getChestplate();
            if (current != null && current.getType() == Material.ELYTRA) {
                 player.getInventory().setChestplate(null);
            }
        }

        player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1f, 0.8f);
    }

    public void finishRun(Player player, CourseData course) {
        RunState state = activeRuns.remove(player.getUniqueId());
        if (state == null) return;

        long timeMs = state.getElapsedTimeMs();
        lastTimes.put(player.getUniqueId(), timeMs);

        // Check PB
        long oldBest = courseManager.getBestTime(player.getUniqueId(), state.getWorldName());
        boolean isPb = oldBest == -1 || timeMs < oldBest;

        courseManager.recordTime(player.getUniqueId(), state.getWorldName(), timeMs);
        long newBest = courseManager.getBestTime(player.getUniqueId(), state.getWorldName());

        // Restore item / cleanup
        player.teleport(course.getFinishLocation());
        player.setVelocity(new Vector(0, 0, 0));
        player.setFallDistance(0);
        player.setGliding(false);

        if (state.getStoredChestItem() != null) {
            player.getInventory().setChestplate(state.getStoredChestItem());
        } else {
             ItemStack current = player.getInventory().getChestplate();
             if (current != null && current.getType() == Material.ELYTRA) {
                 player.getInventory().setChestplate(null);
             }
        }

        // Broadcast to world
        String timeStr = formatTime(timeMs);
        String bestStr = formatTime(newBest);

        String msgPath = isPb ? "messages.run-finish-pb" : "messages.run-finish";
        String msgTemplate = plugin.getConfig().getString(msgPath);

        Component msg = MiniMessage.miniMessage().deserialize(msgTemplate,
                Placeholder.component("player", player.name()),
                Placeholder.parsed("time", timeStr),
                Placeholder.parsed("best_time", bestStr)
        );

        for (Player p : player.getWorld().getPlayers()) {
            p.sendMessage(msg);
        }
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    public long getLastTime(UUID uuid) {
        return lastTimes.getOrDefault(uuid, -1L);
    }

    private void tick() {
        String barFormat = plugin.getConfig().getString("messages.timer-actionbar");
        for (Player p : Bukkit.getOnlinePlayers()) {
            RunState state = activeRuns.get(p.getUniqueId());
            if (state != null) {
                // Ensure they are still in the correct world
                if (!p.getWorld().getName().equals(state.getWorldName())) {
                    activeRuns.remove(p.getUniqueId()); // Silently fail run if world changed externally
                    continue;
                }

                String time = formatTime(state.getElapsedTimeMs());
                Component bar = MiniMessage.miniMessage().deserialize(barFormat, Placeholder.parsed("time", time));
                p.sendActionBar(bar);
            }
        }
    }

    public static String formatTime(long ms) {
        if (ms < 0) return "--:--";
        long totalSecs = ms / 1000;
        long minutes = totalSecs / 60;
        long seconds = totalSecs % 60;
        long millis = ms % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }
}
