package MCOasis.elytra.object;

import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Map;

public class RunState {
    private final long startTimeNanos;
    private final String worldName;
    private final ItemStack storedChestItem;
    private boolean isRunning;

    // Limits how often a specific boost block can boost the player (prevent multi-hits per tick/second)
    private final Map<Long, Long> lastBoostTime = new HashMap<>();

    public RunState(String worldName, ItemStack storedChestItem) {
        this.startTimeNanos = System.nanoTime();
        this.worldName = worldName;
        this.storedChestItem = storedChestItem;
        this.isRunning = true;
    }

    public long getStartTimeNanos() {
        return startTimeNanos;
    }

    public long getElapsedTimeMs() {
        return (System.nanoTime() - startTimeNanos) / 1_000_000;
    }

    public String getWorldName() {
        return worldName;
    }

    public ItemStack getStoredChestItem() {
        return storedChestItem;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public boolean canBoost(long blockHash, long cooldownMs) {
        long now = System.currentTimeMillis();
        long last = lastBoostTime.getOrDefault(blockHash, 0L);
        if (now - last > cooldownMs) {
            lastBoostTime.put(blockHash, now);
            return true;
        }
        return false;
    }
}

