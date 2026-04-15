package MCOasis.elytra.manager;

import MCOasis.elytra.Elytra;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ToolManager {

    private final Elytra plugin;
    private final NamespacedKey TYPE_KEY;
    private final NamespacedKey STRENGTH_KEY;

    // Tool type constants
    public static final String TOOL_EXCEPTION = "EXCEPTION";
    public static final String TOOL_BOOST     = "BOOST";
    public static final String TOOL_WIN       = "WIN";

    public ToolManager(Elytra plugin) {
        this.plugin = plugin;
        this.TYPE_KEY     = new NamespacedKey(plugin, "tool_type");
        this.STRENGTH_KEY = new NamespacedKey(plugin, "boost_strength");
    }

    public void giveTools(Player player) {
        player.getInventory().addItem(buildExceptionTool());
        player.getInventory().addItem(buildBoostTool(1));
        player.getInventory().addItem(buildWinTool());
    }

    // --- builders ---

    private ItemStack buildExceptionTool() {
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm("<green><bold>Exception Block Tool</bold></green>"));
        meta.lore(List.of(
            mm("<gray>Right-click <white>→ Mark exception block</white>"),
            mm("<gray>Left-click  <white>→ Unmark exception block</white>")
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        tag(meta, TOOL_EXCEPTION);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildBoostTool(int strength) {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm("<gold><bold>Boost Block Tool</bold></gold>"));
        meta.lore(List.of(
            mm("<gray>Right-click       <white>→ Mark boost block</white>"),
            mm("<gray>Sneak+Right-click <white>→ Unmark boost block</white>"),
            mm("<gray>Left-click        <white>→ Cycle strength</white>"),
            mm("<gray>Strength: <yellow>" + strength + "</yellow>  <dark_gray>(" + strengthLabel(strength) + ")</dark_gray>")
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        tag(meta, TOOL_BOOST);
        meta.getPersistentDataContainer().set(STRENGTH_KEY, PersistentDataType.INTEGER, strength);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildWinTool() {
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm("<aqua><bold>Win Block Tool</bold></aqua>"));
        meta.lore(List.of(
            mm("<gray>Right-click <white>→ Mark win block</white>"),
            mm("<gray>Left-click  <white>→ Unmark win block</white>")
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        tag(meta, TOOL_WIN);
        item.setItemMeta(meta);
        return item;
    }

    // --- public API ---

    public String getToolType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(TYPE_KEY, PersistentDataType.STRING);
    }

    public int getBoostStrength(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 1;
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(STRENGTH_KEY, PersistentDataType.INTEGER, 1);
    }

    /** Cycles strength 1→10→1 and refreshes lore on the item in hand. */
    public void cycleBoostStrength(ItemStack item, Player player) {
        if (!TOOL_BOOST.equals(getToolType(item))) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int next = (pdc.getOrDefault(STRENGTH_KEY, PersistentDataType.INTEGER, 1) % 10) + 1;
        pdc.set(STRENGTH_KEY, PersistentDataType.INTEGER, next);

        meta.lore(List.of(
            mm("<gray>Right-click       <white>→ Mark boost block</white>"),
            mm("<gray>Sneak+Right-click <white>→ Unmark boost block</white>"),
            mm("<gray>Left-click        <white>→ Cycle strength</white>"),
            mm("<gray>Strength: <yellow>" + next + "</yellow>  <dark_gray>(" + strengthLabel(next) + ")</dark_gray>")
        ));
        item.setItemMeta(meta);

        player.sendActionBar(MiniMessage.miniMessage().deserialize(
                plugin.getConfig().getString("messages.tool-strength-changed",
                        "<green>Boost strength: <strength></green>"),
                Placeholder.parsed("strength", String.valueOf(next))
        ));
    }

    // --- helpers ---

    private void tag(ItemMeta meta, String type) {
        meta.getPersistentDataContainer().set(TYPE_KEY, PersistentDataType.STRING, type);
    }

    private Component mm(String s) {
        return MiniMessage.miniMessage().deserialize(s);
    }

    private String strengthLabel(int s) {
        if (s <= 2) return "Light Blue";
        if (s <= 4) return "Cyan";
        if (s <= 6) return "Lime";
        if (s <= 8) return "Yellow";
        return "Orange";
    }
}
