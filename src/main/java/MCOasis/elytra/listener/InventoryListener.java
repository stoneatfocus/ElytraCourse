package MCOasis.elytra.listener;

import MCOasis.elytra.Elytra;
import MCOasis.elytra.manager.RunManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;

public class InventoryListener implements Listener {

    private final RunManager runManager;

    public InventoryListener(Elytra plugin) {
        this.runManager = plugin.getRunManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // If the player is in an active run, lock their armor slots
        if (runManager.isRunning(player)) {
            if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
                event.setCancelled(true);
            }
            // Prevent Shift-Clicking the Elytra out
            if (event.getCurrentItem() != null && event.getCurrentItem().getType().name().contains("ELYTRA")) {
                event.setCancelled(true);
            }
            // Prevent hotbar swapping the armor
            if (event.getClick().name().contains("NUMBER") || event.getAction().name().contains("HOTBAR")) {
                if (event.getSlot() == 38 || event.getRawSlot() == 6) { // 38 or 6 depending on inventory view
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        // Prevent any item damage (durability loss) during a run
        if (runManager.isRunning(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        // Prevent dropping armor mid-run
        if (runManager.isRunning(event.getPlayer()) && event.getItemDrop().getItemStack().getType().name().contains("ELYTRA")) {
            event.setCancelled(true);
        }
    }
}
