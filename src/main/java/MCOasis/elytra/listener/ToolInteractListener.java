package MCOasis.elytra.listener;

import MCOasis.elytra.Elytra;
import MCOasis.elytra.manager.CourseManager;
import MCOasis.elytra.manager.GhostBlockManager;
import MCOasis.elytra.manager.ToolManager;
import MCOasis.elytra.object.CourseData;
import MCOasis.elytra.util.BlockPosUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ToolInteractListener implements Listener {

    private final Elytra plugin;
    private final ToolManager toolManager;
    private final CourseManager courseManager;
    private final GhostBlockManager ghostBlockManager;

    public ToolInteractListener(Elytra plugin, ToolManager toolManager, GhostBlockManager ghostBlockManager) {
        this.plugin = plugin;
        this.toolManager = toolManager;
        this.courseManager = plugin.getCourseManager();
        this.ghostBlockManager = ghostBlockManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        String type = toolManager.getToolType(item);
        if (type == null) return;

        Action action = event.getAction();

        // Always cancel don't place/interact with the world
        event.setCancelled(true);

        // Left-click: for BOOST this cycles strength, for others it unmarks
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            if (ToolManager.TOOL_BOOST.equals(type)) {
                toolManager.cycleBoostStrength(item, player);
                return;
            }
            // EXCEPTION / WIN left-click unmarks
            Block block = event.getClickedBlock();
            if (block == null) return;
            if (!player.hasPermission("elytracourse.admin")) return;
            handleUnmark(player, block, type);
            return;
        }

        // Right-click on a block
        if (action == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block == null) return;
            if (!player.hasPermission("elytracourse.admin")) return;

            if (ToolManager.TOOL_BOOST.equals(type) && player.isSneaking()) {
                // Sneak + right-click = unmark boost
                handleUnmark(player, block, type);
            } else {
                handleMark(player, block, item, type);
            }
        }
    }

    private void handleMark(Player player, Block block, ItemStack item, String type) {
        World world = block.getWorld();
        CourseData data = courseManager.getCourse(world.getName());
        long packed = BlockPosUtil.asLong(block);
        String msgKey;
        String strengthStr = "";

        switch (type) {
            case ToolManager.TOOL_EXCEPTION -> {
                data.addExceptionBlock(packed);
                msgKey = "messages.tool-mark-exception";
            }
            case ToolManager.TOOL_WIN -> {
                data.addWinBlock(packed);
                msgKey = "messages.tool-mark-win";
            }
            case ToolManager.TOOL_BOOST -> {
                int strength = toolManager.getBoostStrength(item);
                data.addBoostBlock(packed, strength);
                ghostBlockManager.spawnDisplay(world, packed, strength);
                msgKey = "messages.tool-mark-boost";
                strengthStr = String.valueOf(strength);
            }
            default -> { return; }
        }

        courseManager.saveCourse(world.getName());
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        sendFeedback(player, msgKey, strengthStr);
    }

    private void handleUnmark(Player player, Block block, String type) {
        World world = block.getWorld();
        CourseData data = courseManager.getCourse(world.getName());
        long packed = BlockPosUtil.asLong(block);
        String msgKey;

        switch (type) {
            case ToolManager.TOOL_EXCEPTION -> {
                data.removeExceptionBlock(packed);
                msgKey = "messages.tool-unmark-exception";
            }
            case ToolManager.TOOL_WIN -> {
                data.removeWinBlock(packed);
                msgKey = "messages.tool-unmark-win";
            }
            case ToolManager.TOOL_BOOST -> {
                if (!data.getBoostBlocks().containsKey(packed)) return;
                data.removeBoostBlock(packed);
                ghostBlockManager.removeDisplay(world, packed);
                msgKey = "messages.tool-unmark-boost";
            }
            default -> { return; }
        }

        courseManager.saveCourse(world.getName());
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f);
        sendFeedback(player, msgKey, "");
    }

    private void sendFeedback(Player player, String msgKey, String strengthStr) {
        var resolver = strengthStr.isEmpty()
                ? Placeholder.parsed("strength", "")
                : Placeholder.parsed("strength", strengthStr);
        player.sendActionBar(MiniMessage.miniMessage().deserialize(
                plugin.getConfig().getString(msgKey, "Updated"), resolver));
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (toolManager.getToolType(event.getPlayer().getInventory().getItemInMainHand()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (toolManager.getToolType(event.getPlayer().getInventory().getItemInMainHand()) != null) {
            event.setCancelled(true);
        }
    }
}
