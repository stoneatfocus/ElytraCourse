package MCOasis.elytra.listener;

import MCOasis.elytra.Elytra;
import MCOasis.elytra.manager.CourseManager;
import MCOasis.elytra.manager.RunManager;
import MCOasis.elytra.object.CourseData;
import org.bukkit.Location;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class StartPlateListener implements Listener {

    private final Elytra plugin;
    private final CourseManager courseManager;
    private final RunManager runManager;

    public StartPlateListener(Elytra plugin) {
        this.plugin = plugin;
        this.courseManager = plugin.getCourseManager();
        this.runManager = plugin.getRunManager();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Only react to physical interactions (stepping on the plate)
        if (event.getAction() != Action.PHYSICAL) return;
        if (event.getClickedBlock() == null) return;

        // Quick verification: ensure it's actually a pressure plate
        if (!event.getClickedBlock().getType().name().contains("PLATE")) return;

        CourseData data = courseManager.getCourse(event.getClickedBlock().getWorld().getName());
        Location plateLoc = data.getStartPlate();

        if (plateLoc != null && plateLoc.equals(event.getClickedBlock().getLocation())) {
            // Check if course is fully set up before starting
            if (data.isConfigured()) {
                // If the player is already in a run, silently reset them so they can
                // instant-restart without spamming messages or needing to stop manually.
                if (runManager.isRunning(event.getPlayer())) {
                     // Logic handled inside startRun
                }

                runManager.startRun(event.getPlayer(), data);
            }
        }
    }
}
