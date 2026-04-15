package MCOasis.elytra.object;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CourseData {
    private Location startPlate;
    private Location startLocation;
    private Location resetLocation;
    private Location finishLocation;

    private final Set<Long> exceptionBlocks = new HashSet<>();
    private final Set<Long> winBlocks = new HashSet<>();
    private final Map<Long, Integer> boostBlocks = new HashMap<>();

    public CourseData() {}

    public Location getStartPlate() { return startPlate; }
    public void setStartPlate(Location l) { this.startPlate = l; }

    public Location getStartLocation() { return startLocation; }
    public void setStartLocation(Location l) { this.startLocation = l; }

    public Location getResetLocation() { return resetLocation; }
    public void setResetLocation(Location l) { this.resetLocation = l; }

    public Location getFinishLocation() { return finishLocation; }
    public void setFinishLocation(Location l) { this.finishLocation = l; }

    public boolean isExceptionBlock(long pos) { return exceptionBlocks.contains(pos); }
    public void addExceptionBlock(long pos) { exceptionBlocks.add(pos); }
    public void removeExceptionBlock(long pos) { exceptionBlocks.remove(pos); }
    public Set<Long> getExceptionBlocks() { return exceptionBlocks; }

    public boolean isWinBlock(long pos) { return winBlocks.contains(pos); }
    public void addWinBlock(long pos) { winBlocks.add(pos); }
    public void removeWinBlock(long pos) { winBlocks.remove(pos); }
    public Set<Long> getWinBlocks() { return winBlocks; }

    public int getBoostStrength(long pos) { return boostBlocks.getOrDefault(pos, 0); }
    public void addBoostBlock(long pos, int strength) { boostBlocks.put(pos, strength); }
    public void removeBoostBlock(long pos) { boostBlocks.remove(pos); }
    public Map<Long, Integer> getBoostBlocks() { return boostBlocks; }

    public void clear() {
        startPlate = null;
        startLocation = null;
        resetLocation = null;
        finishLocation = null;
        exceptionBlocks.clear();
        winBlocks.clear();
        boostBlocks.clear();
    }

    public boolean isConfigured() {
        return startPlate != null && startLocation != null
                && resetLocation != null && finishLocation != null;
    }
}
