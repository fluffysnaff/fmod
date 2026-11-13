package addon.fmod.modules.courier;

/** The final death-command gate, evaluated again immediately before sending the command. */
public final class CourierSafety {
    private CourierSafety() {}

    public record ReturnCheck(boolean alive, boolean atDestination, boolean shipmentStored,
                              boolean recallPrepared, boolean mainEmpty, boolean equipmentEmpty,
                              boolean craftingEmpty, boolean cursorEmpty, boolean menuClosed) {
        public boolean mayDie() {
            return alive && atDestination && shipmentStored && recallPrepared && mainEmpty
                && equipmentEmpty && craftingEmpty && cursorEmpty && menuClosed;
        }
    }

    public record Position(String dimension, double x, double y, double z) {
        public boolean within(Position other, double radius) {
            if (!dimension.equals(other.dimension)) return false;
            double dx = x - other.x, dy = y - other.y, dz = z - other.z;
            return dx * dx + dy * dy + dz * dz <= radius * radius;
        }
    }
}
