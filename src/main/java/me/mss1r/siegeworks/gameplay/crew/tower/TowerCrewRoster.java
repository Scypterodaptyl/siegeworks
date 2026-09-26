package me.mss1r.siegeworks.gameplay.crew.tower;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TowerCrewRoster {
    private static final String TAG_DRIVER = "Driver";
    private static final String TAG_SEATS = "TowerSeats";
    private static final String TAG_PUSHERS = "TowerPushers";
    private static final String TAG_MEMBER = "Player";
    private static final String TAG_FLOOR = "Floor";
    private static final String TAG_SLOT = "Slot";

    private final Host host;
    private final Map<UUID, Seat> interiorSeats = new HashMap<>();
    private final Map<UUID, Integer> pusherSlots = new HashMap<>();
    private final Map<UUID, Seat> clientInteriorSeats = new HashMap<>();
    private final Map<UUID, Integer> clientPusherSlots = new HashMap<>();
    private UUID driverUuid;
    private UUID clientDriverUuid;
    private String cachedClientData = "";

    public TowerCrewRoster(Host host) {
        this.host = host;
    }

    public void load(CompoundTag tag) {
        interiorSeats.clear();
        pusherSlots.clear();
        driverUuid = tag.hasUUID(TAG_DRIVER) ? tag.getUUID(TAG_DRIVER) : null;

        ListTag savedSeats = tag.getList(TAG_SEATS, Tag.TAG_COMPOUND);
        for (int index = 0; index < savedSeats.size(); index++) {
            CompoundTag savedSeat = savedSeats.getCompound(index);
            if (!savedSeat.hasUUID(TAG_MEMBER)) {
                continue;
            }
            int floor = savedSeat.getInt(TAG_FLOOR);
            int slot = savedSeat.getInt(TAG_SLOT);
            if (validSeat(floor, slot)) {
                interiorSeats.put(savedSeat.getUUID(TAG_MEMBER), new Seat(floor, slot));
            }
        }

        ListTag savedPushers = tag.getList(TAG_PUSHERS, Tag.TAG_COMPOUND);
        for (int index = 0; index < savedPushers.size(); index++) {
            CompoundTag savedPusher = savedPushers.getCompound(index);
            if (!savedPusher.hasUUID(TAG_MEMBER)) {
                continue;
            }
            int slot = savedPusher.getInt(TAG_SLOT);
            if (validPusherSlot(slot)) {
                pusherSlots.put(savedPusher.getUUID(TAG_MEMBER), slot);
            }
        }
        synchronize();
    }

    public void save(CompoundTag tag) {
        if (driverUuid != null) {
            tag.putUUID(TAG_DRIVER, driverUuid);
        }

        ListTag savedSeats = new ListTag();
        for (Map.Entry<UUID, Seat> entry : interiorSeats.entrySet()) {
            CompoundTag savedSeat = new CompoundTag();
            savedSeat.putUUID(TAG_MEMBER, entry.getKey());
            savedSeat.putInt(TAG_FLOOR, entry.getValue().floor());
            savedSeat.putInt(TAG_SLOT, entry.getValue().slot());
            savedSeats.add(savedSeat);
        }
        tag.put(TAG_SEATS, savedSeats);

        ListTag savedPushers = new ListTag();
        for (Map.Entry<UUID, Integer> entry : pusherSlots.entrySet()) {
            CompoundTag savedPusher = new CompoundTag();
            savedPusher.putUUID(TAG_MEMBER, entry.getKey());
            savedPusher.putInt(TAG_SLOT, entry.getValue());
            savedPushers.add(savedPusher);
        }
        tag.put(TAG_PUSHERS, savedPushers);
    }

    public int capacity() {
        int capacity = 1 + TowerPassengerLayout.pusherCapacity();
        for (int floorCapacity : TowerPassengerLayout.floorCapacities()) {
            capacity += floorCapacity;
        }
        return capacity;
    }

    public UUID driverUuid() {
        if (!host.clientSide()) {
            return driverUuid;
        }
        refreshClientMirror();
        return clientDriverUuid;
    }

    public void assignDriver(UUID member) {
        interiorSeats.remove(member);
        pusherSlots.remove(member);
        driverUuid = member;
        synchronize();
    }

    public void clearDriver() {
        if (driverUuid != null) {
            driverUuid = null;
            synchronize();
        }
    }

    public boolean isDriver(Entity entity) {
        UUID driver = driverUuid();
        return driver != null && driver.equals(entity.getUUID());
    }

    public boolean driverSlotAvailable(UUID candidate) {
        return driverUuid == null || driverUuid.equals(candidate) || driverPassenger() == null;
    }

    public Entity driverPassenger() {
        UUID driver = driverUuid;
        if (driver == null) {
            return null;
        }
        for (Entity passenger : host.passengers()) {
            if (passenger.getUUID().equals(driver)) {
                return passenger;
            }
        }
        return null;
    }

    public Seat seat(Entity entity) {
        if (!host.clientSide()) {
            return interiorSeats.get(entity.getUUID());
        }
        refreshClientMirror();
        return clientInteriorSeats.get(entity.getUUID());
    }

    public Seat serverSeat(Entity entity) {
        return interiorSeats.get(entity.getUUID());
    }

    public Seat serverSeat(UUID member) {
        return interiorSeats.get(member);
    }

    public Integer pusherSlot(Entity entity) {
        if (!host.clientSide()) {
            return pusherSlots.get(entity.getUUID());
        }
        refreshClientMirror();
        return clientPusherSlots.get(entity.getUUID());
    }

    public boolean hasPusher(UUID member) {
        return pusherSlots.containsKey(member);
    }

    public boolean hasSeat(UUID member) {
        return interiorSeats.containsKey(member);
    }

    public int firstFreePusherSlot() {
        for (int slot = 0; slot < TowerPassengerLayout.pusherCapacity(); slot++) {
            if (!pusherSlots.containsValue(slot)) {
                return slot;
            }
        }
        return -1;
    }

    public Seat firstFreeInteriorSeat() {
        for (int floor = TowerPassengerLayout.FLOOR_ONE;
             floor <= TowerPassengerLayout.FLOOR_THREE; floor++) {
            int slot = firstFreeSlot(floor);
            if (slot >= 0) {
                return new Seat(floor, slot);
            }
        }
        return null;
    }

    public int firstFreeSlot(int floor) {
        if (floor < TowerPassengerLayout.FLOOR_ONE || floor > TowerPassengerLayout.FLOOR_THREE) {
            return -1;
        }
        for (int slot = 0; slot < TowerPassengerLayout.floorCapacity(floor); slot++) {
            if (isSeatFree(floor, slot)) {
                return slot;
            }
        }
        return -1;
    }

    public void assignPusher(UUID member, int slot) {
        if (!validPusherSlot(slot)) {
            throw new IllegalArgumentException("Invalid tower pusher slot: " + slot);
        }
        interiorSeats.remove(member);
        pusherSlots.put(member, slot);
        synchronize();
    }

    public void assignSeat(UUID member, int floor, int slot) {
        if (!validSeat(floor, slot)) {
            throw new IllegalArgumentException("Invalid tower interior seat: " + floor + ":" + slot);
        }
        pusherSlots.remove(member);
        interiorSeats.put(member, new Seat(floor, slot));
        synchronize();
    }

    public boolean removePusher(UUID member) {
        boolean changed = pusherSlots.remove(member) != null;
        if (changed) {
            synchronize();
        }
        return changed;
    }

    public boolean removeSeat(UUID member) {
        boolean changed = interiorSeats.remove(member) != null;
        if (changed) {
            synchronize();
        }
        return changed;
    }

    public boolean clearReservation(UUID member) {
        boolean changed = pusherSlots.remove(member) != null;
        changed |= interiorSeats.remove(member) != null;
        if (changed) {
            synchronize();
        }
        return changed;
    }

    public void synchronize() {
        if (host.clientSide()) {
            return;
        }

        List<Map.Entry<UUID, Seat>> orderedSeats = new ArrayList<>(interiorSeats.entrySet());
        orderedSeats.sort(Comparator
                .comparingInt((Map.Entry<UUID, Seat> entry) -> entry.getValue().floor())
                .thenComparingInt(entry -> entry.getValue().slot()));
        List<Map.Entry<UUID, Integer>> orderedPushers = new ArrayList<>(pusherSlots.entrySet());
        orderedPushers.sort(Comparator.comparingInt(Map.Entry::getValue));

        StringBuilder serialized = new StringBuilder("driver=");
        if (driverUuid != null) {
            serialized.append(driverUuid);
        }
        serialized.append("|pushers=");
        for (Map.Entry<UUID, Integer> entry : orderedPushers) {
            serialized.append(entry.getKey()).append(':').append(entry.getValue()).append(';');
        }
        serialized.append("|seats=");
        for (Map.Entry<UUID, Seat> entry : orderedSeats) {
            serialized.append(entry.getKey()).append(':')
                    .append(entry.getValue().floor()).append(':')
                    .append(entry.getValue().slot()).append(';');
        }
        host.setSynchronizedData(serialized.toString());
    }

    private void refreshClientMirror() {
        String serialized = host.synchronizedData();
        if (serialized.equals(cachedClientData)) {
            return;
        }

        cachedClientData = serialized;
        clientInteriorSeats.clear();
        clientPusherSlots.clear();
        clientDriverUuid = null;

        String[] sections = serialized.split("\\|", -1);
        if (sections.length > 0 && sections[0].startsWith("driver=")) {
            clientDriverUuid = parseUuid(sections[0].substring("driver=".length()));
        }

        for (int index = 1; index < sections.length; index++) {
            if (sections[index].startsWith("pushers=")) {
                parsePushers(sections[index].substring("pushers=".length()));
            } else if (sections[index].startsWith("seats=")) {
                parseSeats(sections[index].substring("seats=".length()));
            }
        }
    }

    private void parseSeats(String serializedSeats) {
        for (String entry : serializedSeats.split(";")) {
            String[] parts = entry.split(":");
            if (parts.length != 3) {
                continue;
            }
            try {
                UUID member = UUID.fromString(parts[0]);
                int floor = Integer.parseInt(parts[1]);
                int slot = Integer.parseInt(parts[2]);
                if (validSeat(floor, slot)) {
                    clientInteriorSeats.put(member, new Seat(floor, slot));
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void parsePushers(String serializedPushers) {
        for (String entry : serializedPushers.split(";")) {
            String[] parts = entry.split(":");
            if (parts.length != 2) {
                continue;
            }
            try {
                UUID member = UUID.fromString(parts[0]);
                int slot = Integer.parseInt(parts[1]);
                if (validPusherSlot(slot)) {
                    clientPusherSlots.put(member, slot);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static UUID parseUuid(String value) {
        if (value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean isSeatFree(int floor, int slot) {
        for (Seat seat : interiorSeats.values()) {
            if (seat.floor() == floor && seat.slot() == slot) {
                return false;
            }
        }
        return true;
    }

    private boolean validSeat(int floor, int slot) {
        return TowerPassengerLayout.validSeat(floor, slot);
    }

    private boolean validPusherSlot(int slot) {
        return TowerPassengerLayout.validPusherSlot(slot);
    }

    public record Seat(int floor, int slot) {
    }

    public interface Host {
        boolean clientSide();

        List<Entity> passengers();

        String synchronizedData();

        void setSynchronizedData(String data);
    }
}
