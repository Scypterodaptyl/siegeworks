package me.mss1r.siegeworks.gameplay.crew.tower;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class TowerPassengerLayout {
    public static final int FLOOR_ONE = 1;
    public static final int FLOOR_TWO = 2;
    public static final int FLOOR_THREE = 3;

    private static final Vec3 DRIVER = seat(50.0D, 0.0D, 66.6D);
    private static final List<Vec3> PUSHERS = seats(new double[][]{
            {30, 0, 66.6}, {10, 0, 66.6}, {-10, 0, 66.6},
            {-30, 0, 66.6}, {-50, 0, 66.6}
    });
    private static final List<Vec3> FLOOR_ONE_SEATS = seats(new double[][]{
            {-48, 19, -46}, {-24, 19, -46}, {0, 19, -46}, {24, 19, -46}, {48, 19, -46},
            {-48, 19, -14}, {-24, 19, -14}, {0, 19, -14}, {24, 19, -14}, {48, 19, -14},
            {-48, 19, 18}, {-24, 19, 18}, {0, 19, 18}
    });
    private static final List<Vec3> FLOOR_TWO_SEATS = seats(new double[][]{
            {-48, 160, -46}, {-24, 160, -46}, {0, 160, -46}, {24, 160, -46}, {48, 160, -46},
            {-48, 160, -14}, {-24, 160, -14}, {0, 160, -14}, {24, 160, -14}, {48, 160, -14},
            {-48, 160, 18}, {-24, 160, 18}, {0, 160, 18}
    });
    private static final List<Vec3> FLOOR_THREE_SEATS = seats(new double[][]{
            {-48, 275, -54}, {-24, 275, -54}, {0, 275, -54}, {24, 275, -54}, {48, 275, -54},
            {-24, 275, -22}, {0, 275, -22}, {-48, 275, -6}, {48, 275, -6}
    });

    private TowerPassengerLayout() {
    }

    public static Vec3 driverOffset() {
        return DRIVER;
    }

    public static int pusherCapacity() {
        return PUSHERS.size();
    }

    public static boolean validPusherSlot(int slot) {
        return slot >= 0 && slot < PUSHERS.size();
    }

    public static Vec3 pusherOffset(int slot) {
        return validPusherSlot(slot) ? PUSHERS.get(slot) : Vec3.ZERO;
    }

    public static int[] floorCapacities() {
        return new int[]{FLOOR_ONE_SEATS.size(), FLOOR_TWO_SEATS.size(), FLOOR_THREE_SEATS.size()};
    }

    public static int floorCapacity(int floor) {
        return seatsForFloor(floor).size();
    }

    public static boolean validSeat(int floor, int slot) {
        List<Vec3> seats = seatsForFloor(floor);
        return floor >= FLOOR_ONE && floor <= FLOOR_THREE && slot >= 0 && slot < seats.size();
    }

    public static Vec3 seatOffset(int floor, int slot) {
        return validSeat(floor, slot) ? seatsForFloor(floor).get(slot) : Vec3.ZERO;
    }

    public static Vec3 clampedSeatOffset(int floor, int slot) {
        List<Vec3> seats = seatsForFloor(floor);
        return seats.get(Mth.clamp(slot, 0, seats.size() - 1));
    }

    private static List<Vec3> seatsForFloor(int floor) {
        return switch (floor) {
            case FLOOR_TWO -> FLOOR_TWO_SEATS;
            case FLOOR_THREE -> FLOOR_THREE_SEATS;
            default -> FLOOR_ONE_SEATS;
        };
    }

    private static List<Vec3> seats(double[][] coordinates) {
        return java.util.Arrays.stream(coordinates)
                .map(point -> seat(point[0], point[1], point[2]))
                .toList();
    }

    private static Vec3 seat(double x, double y, double z) {
        return new Vec3(x / 16.0D, y / 16.0D, z / 16.0D);
    }
}
