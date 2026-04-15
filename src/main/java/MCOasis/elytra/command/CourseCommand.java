package MCOasis.elytra.command;

import MCOasis.elytra.Elytra;
import MCOasis.elytra.manager.CourseManager;
import MCOasis.elytra.manager.ToolManager;
import MCOasis.elytra.object.CourseData;
import MCOasis.elytra.util.LocationUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class CourseCommand implements CommandExecutor {

    private final Elytra plugin;
    private final CourseManager courseManager;
    private final ToolManager toolManager;

    public CourseCommand(Elytra plugin, ToolManager toolManager) {
        this.plugin = plugin;
        this.courseManager = plugin.getCourseManager();
        this.toolManager = toolManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            // Auto-send help if no args
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("help")) {
            sendHelp(sender);
            return true;
        }

        if (sub.equals("reload")) {
            if (!checkPerm(sender, "elytracourse.admin")) return true;
            plugin.reloadConfig();
            sender.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.reload-success")));
            return true;
        }

        if (!(sender instanceof Player player)) {
             sender.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.only-players")));
             return true;
        }

        if (sub.equals("tools")) {
            if (!sender.hasPermission("elytracourse.admin") && !sender.hasPermission("elytracourse.tools")) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.no-permission")));
                return true;
            }
            Player target = player;
            if (args.length > 1) {
                Player t = Bukkit.getPlayer(args[1]);
                if (t != null) target = t;
            }
            toolManager.giveTools(target);
            target.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.tools-given")));
            return true;
        }

        // Configuration commands require admin
        if (!checkPerm(player, "elytracourse.admin")) return true;

        String worldName = player.getWorld().getName();
        CourseData data = courseManager.getCourse(worldName);

        switch (sub) {
            case "deletecourse":
            case "resetcourse":
                data.clear();
                courseManager.saveCourse(worldName);
                plugin.getGhostBlockManager().removeAllDisplays(player.getWorld());
                player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Course data for world <yellow>" + worldName + "</yellow> has been deleted.</green>"));
                break;

            case "setstartplate":
                Block targetBlock = player.getTargetBlock((Set<Material>) null, 5);
                if (targetBlock == null || targetBlock.getType().isAir()) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You must look at a block.</red>"));
                    return true;
                }
                if (!targetBlock.getType().name().contains("PRESSURE_PLATE")) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Target block is not a pressure plate.</red>"));
                    return true;
                }
                data.setStartPlate(targetBlock.getLocation());
                saveAndNotify(player, data, "messages.set-start-plate",
                        Placeholder.component("location", Component.text(LocationUtil.serialize(targetBlock.getLocation()))),
                        Placeholder.component("world", Component.text(worldName)));
                break;

            case "setstartloc":
                data.setStartLocation(player.getLocation());
                saveAndNotify(player, data, "messages.set-start-loc");
                break;

            case "setresetloc":
                data.setResetLocation(player.getLocation());
                saveAndNotify(player, data, "messages.set-reset-loc");
                break;

            case "setfinishloc":
                data.setFinishLocation(player.getLocation());
                saveAndNotify(player, data, "messages.set-finish-loc");
                break;

            case "info":
                sendInfo(player, data, worldName);
                break;

            default:
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Unknown subcommand.</red>"));
                break;
        }

        return true;
    }

    private boolean checkPerm(CommandSender sender, String perm) {
        if (!sender.hasPermission(perm)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.no-permission")));
            return false;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        String header = "<gradient:#00BFFF:#1E90FF><bold>ElytraCourse Help</bold></gradient>";
        sender.sendMessage(MiniMessage.miniMessage().deserialize(header));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gray>/ec setstartplate <dark_gray>-</dark_gray> <white>Set start pressure plate</white></gray>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gray>/ec setstartloc <dark_gray>-</dark_gray> <white>Set launch location</white></gray>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gray>/ec setresetloc <dark_gray>-</dark_gray> <white>Set reset location</white></gray>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gray>/ec setfinishloc <dark_gray>-</dark_gray> <white>Set finish location</white></gray>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gray>/ec deletecourse <dark_gray>-</dark_gray> <white>Delete all course data for world</white></gray>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gray>/ec tools [player] <dark_gray>-</dark_gray> <white>Get tools</white></gray>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gray>/ec info <dark_gray>-</dark_gray> <white>View course info</white></gray>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gray>/ec reload <dark_gray>-</dark_gray> <white>Reload config</white></gray>"));
    }

    private void saveAndNotify(Player player, CourseData data, String msgPath, net.kyori.adventure.text.minimessage.tag.resolver.TagResolver... tags) {
        courseManager.saveCourse(player.getWorld().getName());
        player.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString(msgPath), tags));
    }

    private void sendInfo(Player player, CourseData data, String worldName) {
        Component prefix = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.prefix", ""));
        player.sendMessage(prefix.append(MiniMessage.miniMessage().deserialize("<yellow>Course Info for <aqua>" + worldName + "</aqua>:</yellow>")));
        player.sendMessage(Component.text(" Start Plate: " + (data.getStartPlate() != null ? LocationUtil.serialize(data.getStartPlate()) : "Not set")));
        player.sendMessage(Component.text(" Start Loc: " + (data.getStartLocation() != null ? "Set" : "Not set")));
        player.sendMessage(Component.text(" Reset Loc: " + (data.getResetLocation() != null ? "Set" : "Not set")));
        player.sendMessage(Component.text(" Finish Loc: " + (data.getFinishLocation() != null ? "Set" : "Not set")));
    }
}
