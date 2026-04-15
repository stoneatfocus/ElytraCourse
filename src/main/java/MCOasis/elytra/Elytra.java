package MCOasis.elytra;

import MCOasis.elytra.command.CourseCommand;
import MCOasis.elytra.hook.ElytraPlaceholderExpansion;
import MCOasis.elytra.listener.GameListener;
import MCOasis.elytra.listener.InventoryListener;
import MCOasis.elytra.listener.StartPlateListener;
import MCOasis.elytra.listener.ToolInteractListener;
import MCOasis.elytra.manager.CourseManager;
import MCOasis.elytra.manager.GhostBlockManager;
import MCOasis.elytra.manager.RunManager;
import MCOasis.elytra.manager.ToolManager;
import MCOasis.elytra.object.CourseData;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class Elytra extends JavaPlugin {

    private CourseManager courseManager;
    private RunManager runManager;
    private ToolManager toolManager;
    private GhostBlockManager ghostBlockManager;

    @Override
    public void onEnable() {
        // Config
        saveDefaultConfig();

        // Managers
        this.courseManager     = new CourseManager(this);
        this.ghostBlockManager = new GhostBlockManager(this);
        this.runManager        = new RunManager(this, courseManager);
        this.toolManager       = new ToolManager(this);
        this.runManager.start();

        // For every loaded world that has course data, adopt any existing display entities
        // (they survive restarts because they're persistent) then spawn any that are missing.
        for (World world : Bukkit.getWorlds()) {
            CourseData course = courseManager.getCourse(world.getName());
            if (!course.getBoostBlocks().isEmpty()) {
                ghostBlockManager.adoptExisting(course, world);
                ghostBlockManager.spawnMissing(course, world);
            }
        }

        // Commands
        getCommand("elytracourse").setExecutor(new CourseCommand(this, toolManager));

        // Listeners
        getServer().getPluginManager().registerEvents(new StartPlateListener(this), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new ToolInteractListener(this, toolManager, ghostBlockManager), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);

        // Placeholders
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ElytraPlaceholderExpansion(this).register();
        }
    }

    @Override
    public void onDisable() {
        if (runManager != null) runManager.stop();
        if (courseManager != null) courseManager.saveAll();
    }

    public CourseManager getCourseManager()        { return courseManager; }
    public RunManager getRunManager()              { return runManager; }
    public GhostBlockManager getGhostBlockManager() { return ghostBlockManager; }
}
