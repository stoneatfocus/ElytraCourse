package MCOasis.elytra.listener;

import MCOasis.elytra.Elytra;
import MCOasis.elytra.manager.CourseManager;
import MCOasis.elytra.manager.RunManager;
import MCOasis.elytra.object.CourseData;
import MCOasis.elytra.object.RunState;
import MCOasis.elytra.util.BlockPosUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class GameListener implements Listener {

    private final Elytra plugin;
    private final CourseManager courseManager;
    private final RunManager runManager;

    public GameListener(Elytra plugin) {
        this.plugin = plugin;
        this.courseManager = plugin.getCourseManager();
        this.runManager = plugin.getRunManager();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // Catch every slight movement for accurate collision detection,
        // especially when a player is just hovering or barely touching a block.

        Player player = event.getPlayer();
        if (!runManager.isRunning(player)) return;

        CourseData course = courseManager.getCourse(player.getWorld().getName());
        // Ensure course exists before checking collisions
        if (!course.isConfigured()) return;

        RunState state = runManager.getRunState(player);
        if (!state.isRunning()) return;

        // 1. Trigger Logic (Win Blocks & Boosters)
        // Check these *before* solids to allow players to pass through
        // a win/boost block even if it's solid without crashing.

        boolean hitWin = false;
        boolean hitBoost = false;

        // Use bounding box for intersection
        BoundingBox box = player.getBoundingBox();

        // Expand bounds slightly to catch surface contacts.
        // If checking for "Standing On", explicitly check the block below IF on ground.

        int minX = floor(box.getMinX());
        int maxX = floor(box.getMaxX());
        int minY = floor(box.getMinY());
        int maxY = floor(box.getMaxY());
        int minZ = floor(box.getMinZ());
        int maxZ = floor(box.getMaxZ());

        Set<Long> intersectedBlocks = new HashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    intersectedBlocks.add(BlockPosUtil.asLong(x, y, z));
                }
            }
        }

        // If on ground, add block immediately below
        if (player.isOnGround()) {
            int belowY = floor(player.getLocation().getY() - 0.1); // Just below feet
            int pX = floor(player.getLocation().getX());
            int pZ = floor(player.getLocation().getZ());
            // Add the block directly below center of player
            intersectedBlocks.add(BlockPosUtil.asLong(pX, belowY, pZ));
        }

        // Check Triggers first
        for (long packed : intersectedBlocks) {
            if (course.isWinBlock(packed)) {
                runManager.finishRun(player, course);
                return; // Win!
            }

            int strength = course.getBoostStrength(packed);
            if (strength > 0) {
                 hitBoost = true;
                 long cooldown = (long)(plugin.getConfig().getDouble("options.boost-cooldown-seconds", 0.5) * 1000);
                 if (state.canBoost(packed, cooldown)) {
                     Vector direction = player.getLocation().getDirection().normalize();
                     double speed = 1.0 + (strength * 0.2);
                     player.setVelocity(direction.multiply(speed));
                     player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1.5f);
                 }
            }
        }

        // 2. Collision Logic (Solid Blocks)
        boolean failed = false;

        for (long packed : intersectedBlocks) {
            // Unpack coordinates for the lookup
            int x = BlockPosUtil.getX(packed);
            int y = BlockPosUtil.getY(packed);
            int z = BlockPosUtil.getZ(packed);

            // Skip checks for blocks that are explicitly helpful/safe
            if (course.isExceptionBlock(packed)) continue;
            if (course.isWinBlock(packed)) continue;
            if (course.getBoostStrength(packed) > 0) continue;

            // Hitting an unlisted solid block causes a crash
            Block b = player.getWorld().getBlockAt(x, y, z);
            if (b.getType().isSolid()) {
                failed = true;
                break;
            }
        }

        // Reset if gliding stops unexpectedly while in an unsafe location
        boolean resetOnStop = plugin.getConfig().getBoolean("options.reset-on-stop-gliding", false);
        if (resetOnStop && !player.isGliding() && !LocationIsSafe(player.getLocation())) {
             failed = true;
        }

        if (failed) {
            runManager.resetRun(player, course);
        }
    }

    private int floor(double d) {
        return (int) Math.floor(d);
    }

    private boolean LocationIsSafe(Location loc) {
        // Just a helper if needed
        return false;
    }
}
