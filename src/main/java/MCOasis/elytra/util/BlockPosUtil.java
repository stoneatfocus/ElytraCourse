package MCOasis.elytra.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class BlockPosUtil {

    // Pack x, y, z into a long.
    // X: 26 bits, Z: 26 bits, Y: 12 bits
    // Supports world border limits (+-30M) and usually Y range (-2048 to 2047)
    public static long asLong(int x, int y, int z) {
        return ((long)(x & 0x3FFFFFF) << 38) | ((long)(z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    public static long asLong(Location loc) {
        return asLong(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public static long asLong(Block block) {
        return asLong(block.getX(), block.getY(), block.getZ());
    }

    public static int getX(long packed) {
        int x = (int) ((packed >> 38) & 0x3FFFFFF);
        if (x >= 0x2000000) x -= 0x4000000; // Sign extension
        return x;
    }

    public static int getY(long packed) {
        int y = (int) (packed & 0xFFF);
        if (y >= 0x800) y -= 0x1000; // Sign extension
        return y;
    }

    public static int getZ(long packed) {
        int z = (int) ((packed >> 12) & 0x3FFFFFF);
        if (z >= 0x2000000) z -= 0x4000000; // Sign extension
        return z;
    }

    public static Location toLocation(long packed, World world) {
        return new Location(world, getX(packed), getY(packed), getZ(packed));
    }
}

