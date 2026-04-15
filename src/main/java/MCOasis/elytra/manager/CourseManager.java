package MCOasis.elytra.manager;

import MCOasis.elytra.Elytra;
import MCOasis.elytra.object.CourseData;
import MCOasis.elytra.storage.CourseStorage;
import org.bukkit.World;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CourseManager {

    private final Elytra plugin;
    private final CourseStorage storage;
    private final Map<String, CourseData> courses = new HashMap<>();

    // Player Best Times: UUID -> WorldName -> TimeMS
    private final Map<UUID, Map<String, Long>> playerBestTimes;

    public CourseManager(Elytra plugin) {
        this.plugin = plugin;
        this.storage = new CourseStorage(plugin);
        this.playerBestTimes = storage.loadPlayerTimes();
    }

    public CourseData getCourse(String worldName) {
        if (!courses.containsKey(worldName)) {
            courses.put(worldName, storage.loadCourse(worldName));
        }
        return courses.get(worldName);
    }

    public CourseData getCourse(World world) {
        return getCourse(world.getName());
    }

    public void saveCourse(String worldName) {
        if (courses.containsKey(worldName)) {
            storage.saveCourse(worldName, courses.get(worldName));
        }
    }

    public void recordTime(UUID player, String worldName, long timeMs) {
        Map<String, Long> times = playerBestTimes.computeIfAbsent(player, k -> new HashMap<>());
        long currentBest = times.getOrDefault(worldName, Long.MAX_VALUE);

        if (timeMs < currentBest) {
            times.put(worldName, timeMs);
            // Save on write to ensure persistence
            storage.savePlayerTimes(playerBestTimes);
        }
    }

    public long getBestTime(UUID player, String worldName) {
        Map<String, Long> times = playerBestTimes.get(player);
        if (times == null) return -1;
        return times.getOrDefault(worldName, -1L);
    }

    public List<Map.Entry<UUID, Long>> getLeaderboard(String worldName) {
        List<Map.Entry<UUID, Long>> list = new ArrayList<>();
        for (Map.Entry<UUID, Map<String, Long>> entry : playerBestTimes.entrySet()) {
            if (entry.getValue().containsKey(worldName)) {
                list.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().get(worldName)));
            }
        }
        list.sort(Map.Entry.comparingByValue());
        return list;
    }

    public void saveAll() {
        for (String world : courses.keySet()) {
            saveCourse(world);
        }
        storage.savePlayerTimes(playerBestTimes);
    }
}
