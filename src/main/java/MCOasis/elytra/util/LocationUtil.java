package MCOasis.elytra.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationUtil {

    public static String serialize(Location loc) {
        if (loc == null) return null;
        return loc.getWorld().getName() + "," +
               limitDecimals(loc.getX()) + "," +
               limitDecimals(loc.getY()) + "," +
               limitDecimals(loc.getZ()) + "," +
               limitDecimals(loc.getYaw()) + "," +
               limitDecimals(loc.getPitch());
    }

    public static Location deserialize(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] parts = s.split(",");
        if (parts.length < 6) return null;

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null; // World not loaded or doesn't exist

        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String limitDecimals(double d) {
        return String.format("%.4f", d);
    }
    private static String limitDecimals(float f) {
        return String.format("%.2f", f);
    }
}

