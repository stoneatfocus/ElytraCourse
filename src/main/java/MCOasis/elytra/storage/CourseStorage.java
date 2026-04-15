package MCOasis.elytra.storage;

import MCOasis.elytra.Elytra;
import MCOasis.elytra.object.CourseData;
import MCOasis.elytra.util.LocationUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class CourseStorage {

    private final Elytra plugin;
    private final File dataFolder;

    public CourseStorage(Elytra plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "worlds");
        if (!dataFolder.exists()) dataFolder.mkdirs();
    }

    public CourseData loadCourse(String worldName) {
        File file = new File(dataFolder, worldName + ".yml");
        if (!file.exists()) return new CourseData();

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        CourseData data = new CourseData();

        data.setStartPlate(LocationUtil.deserialize(config.getString("locations.start-plate")));
        data.setStartLocation(LocationUtil.deserialize(config.getString("locations.start-spawn")));
        data.setResetLocation(LocationUtil.deserialize(config.getString("locations.reset")));
        data.setFinishLocation(LocationUtil.deserialize(config.getString("locations.finish")));

        for (Long l : config.getLongList("blocks.exceptions")) data.addExceptionBlock(l);
        for (Long l : config.getLongList("blocks.wins")) data.addWinBlock(l);

        ConfigurationSection boostSec = config.getConfigurationSection("blocks.boosts");
        if (boostSec != null) {
            for (String key : boostSec.getKeys(false)) {
                try {
                    long packed = Long.parseLong(key);
                    int strength = boostSec.getInt(key);
                    data.addBoostBlock(packed, strength);
                } catch (NumberFormatException ignored) {}
            }
        }

        return data;
    }

    public void saveCourse(String worldName, CourseData data) {
        File file = new File(dataFolder, worldName + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set("locations.start-plate", LocationUtil.serialize(data.getStartPlate()));
        config.set("locations.start-spawn", LocationUtil.serialize(data.getStartLocation()));
        config.set("locations.reset", LocationUtil.serialize(data.getResetLocation()));
        config.set("locations.finish", LocationUtil.serialize(data.getFinishLocation()));

        config.set("blocks.exceptions", new ArrayList<>(data.getExceptionBlocks()));
        config.set("blocks.wins", new ArrayList<>(data.getWinBlocks()));

        config.set("blocks.boosts", null);
        for (Map.Entry<Long, Integer> entry : data.getBoostBlocks().entrySet()) {
            config.set("blocks.boosts." + entry.getKey(), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save course data for world " + worldName, e);
        }
    }

    public Map<UUID, Map<String, Long>> loadPlayerTimes() {
        File file = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!file.exists()) return new HashMap<>();

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<UUID, Map<String, Long>> allData = new HashMap<>();

        for (String uuidStr : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection sec = config.getConfigurationSection(uuidStr);
                Map<String, Long> worldTimes = new HashMap<>();
                if (sec != null) {
                    for (String w : sec.getKeys(false)) worldTimes.put(w, sec.getLong(w));
                }
                allData.put(uuid, worldTimes);
            } catch (Exception ignored) {}
        }
        return allData;
    }

    public void savePlayerTimes(Map<UUID, Map<String, Long>> allData) {
        File file = new File(plugin.getDataFolder(), "playerdata.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (Map.Entry<UUID, Map<String, Long>> entry : allData.entrySet()) {
            String uid = entry.getKey().toString();
            for (Map.Entry<String, Long> w : entry.getValue().entrySet()) {
                config.set(uid + "." + w.getKey(), w.getValue());
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player data", e);
        }
    }
}
