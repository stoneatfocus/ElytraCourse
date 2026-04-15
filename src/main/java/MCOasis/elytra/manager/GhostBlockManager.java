package MCOasis.elytra.manager;

import MCOasis.elytra.object.CourseData;
import MCOasis.elytra.util.BlockPosUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages boost zone visuals using BlockDisplay entities — no collision, visible to all players,
 * persistent across logins. The world block underneath is never touched.
 */
public class GhostBlockManager {

    // Tag we stamp on every display entity so we can find/clean them up on reload
    private static final String PDC_KEY_NAME = "boost_display";

    private final NamespacedKey pdcKey;
    // world name → packed pos → display entity UUID
    private final Map<String, Map<Long, UUID>> displays = new HashMap<>();

    private static final Material[] GLASS_BY_TIER = {
            Material.LIGHT_BLUE_STAINED_GLASS,  // 1–2
            Material.CYAN_STAINED_GLASS,         // 3–4
            Material.LIME_STAINED_GLASS,         // 5–6
            Material.YELLOW_STAINED_GLASS,       // 7–8
            Material.ORANGE_STAINED_GLASS        // 9–10
    };

    public GhostBlockManager(JavaPlugin plugin) {
        this.pdcKey = new NamespacedKey(plugin, PDC_KEY_NAME);
    }

    public static Material glassForStrength(int strength) {
        int idx = Math.max(0, Math.min(4, (strength - 1) / 2));
        return GLASS_BY_TIER[idx];
    }

    /**
     * Spawns a BlockDisplay at the block position. If one already exists there, remove it first
     * (handles strength update / re-mark).
     */
    public void spawnDisplay(World world, long packed, int strength) {
        // Remove any existing display at this position first
        removeDisplay(world, packed);

        int x = BlockPosUtil.getX(packed);
        int y = BlockPosUtil.getY(packed);
        int z = BlockPosUtil.getZ(packed);
        // BlockDisplay position is its bottom-northwest corner, same as block coordinates
        Location loc = new Location(world, x, y, z);

        BlockDisplay display = (BlockDisplay) world.spawnEntity(loc, EntityType.BLOCK_DISPLAY);
        display.setBlock(glassForStrength(strength).createBlockData());
        display.setPersistent(true);
        display.setInvulnerable(true);
        // Tag it so we can find it after a reload
        display.getPersistentDataContainer().set(pdcKey, PersistentDataType.INTEGER, strength);

        displays.computeIfAbsent(world.getName(), k -> new HashMap<>()).put(packed, display.getUniqueId());
    }

    /** Removes the display entity at a position if one exists. */
    public void removeDisplay(World world, long packed) {
        Map<Long, UUID> worldMap = displays.get(world.getName());
        if (worldMap == null) return;
        UUID uid = worldMap.remove(packed);
        if (uid == null) return;
        Entity e = Bukkit.getEntity(uid);
        if (e != null) e.remove();
    }

    /** Removes only plugin-owned boost display entities for a world (tracked in map). */
    public void removeAllDisplays(World world) {
        Map<Long, UUID> worldMap = displays.remove(world.getName());
        if (worldMap == null) return;
        for (UUID uid : worldMap.values()) {
            Entity e = Bukkit.getEntity(uid);
            if (e != null) e.remove();
        }
    }

    /**
     * Re-adopts any BlockDisplay entities that were already in the world before this plugin loaded
     * (e.g. after /reload or server restart — entities are persistent).
     * Call this once on enable after course data is loaded.
     */
    public void adoptExisting(CourseData course, World world) {
        if (world == null) return;
        for (BlockDisplay e : world.getEntitiesByClass(BlockDisplay.class)) {
            Integer strength = e.getPersistentDataContainer().get(pdcKey, PersistentDataType.INTEGER);
            if (strength == null) continue;
            int x = (int) Math.floor(e.getLocation().getX());
            int y = (int) Math.floor(e.getLocation().getY());
            int z = (int) Math.floor(e.getLocation().getZ());
            long packed = BlockPosUtil.asLong(x, y, z);
            if (course.getBoostStrength(packed) > 0) {
                displays.computeIfAbsent(world.getName(), k -> new HashMap<>())
                        .put(packed, e.getUniqueId());
            }
        }
    }

    /**
     * Spawns displays for all boost blocks in a course that don't already have one.
     * Used after loading course data on startup.
     */
    public void spawnMissing(CourseData course, World world) {
        if (world == null) return;
        Map<Long, UUID> worldMap = displays.getOrDefault(world.getName(), new HashMap<>());
        for (Map.Entry<Long, Integer> entry : course.getBoostBlocks().entrySet()) {
            if (!worldMap.containsKey(entry.getKey())) {
                spawnDisplay(world, entry.getKey(), entry.getValue());
            }
        }
    }
}
