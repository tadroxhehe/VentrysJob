package com.ventrys.job.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Progression temps réel des animaux d'élevage, y compris chunks déchargés.
 */
public final class LivestockProgressSavedData extends SavedData {

    private static final String DATA_NAME = "ventrysjob_livestock";

    public static final class Entry {
        public String entityTypeId;
        public double x;
        public double y;
        public double z;
        public boolean isMale;
        public int nutrition;
        public int hydration;
        public long reproductionProgressMs;
        /** Progression grossesse (après accouplement), 0 si pas enceinte. */
        public long pregnancyProgressMs;
        public boolean pregnant;
        public long lastNutritionDecreaseMs;
        public long lastHydrationDecreaseMs;
        public long lastRegenerationMs;
        public long lastProcessedWallMs;
        public int pendingHealHearts;

        public Entry copy() {
            Entry e = new Entry();
            e.entityTypeId = entityTypeId;
            e.x = x;
            e.y = y;
            e.z = z;
            e.isMale = isMale;
            e.nutrition = nutrition;
            e.hydration = hydration;
            e.reproductionProgressMs = reproductionProgressMs;
            e.pregnancyProgressMs = pregnancyProgressMs;
            e.pregnant = pregnant;
            e.lastNutritionDecreaseMs = lastNutritionDecreaseMs;
            e.lastHydrationDecreaseMs = lastHydrationDecreaseMs;
            e.lastRegenerationMs = lastRegenerationMs;
            e.lastProcessedWallMs = lastProcessedWallMs;
            e.pendingHealHearts = pendingHealHearts;
            return e;
        }
    }

    private final Map<UUID, Entry> entries = new HashMap<>();

    public static LivestockProgressSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            LivestockProgressSavedData::load,
            LivestockProgressSavedData::new,
            DATA_NAME
        );
    }

    public static LivestockProgressSavedData load(CompoundTag tag) {
        LivestockProgressSavedData data = new LivestockProgressSavedData();
        ListTag list = tag.getList("animals", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            UUID uuid = c.getUUID("uuid");
            Entry entry = new Entry();
            entry.entityTypeId = c.getString("type");
            entry.x = c.getDouble("x");
            entry.y = c.getDouble("y");
            entry.z = c.getDouble("z");
            entry.isMale = c.getBoolean("male");
            entry.nutrition = c.getInt("nutrition");
            entry.hydration = c.getInt("hydration");
            entry.reproductionProgressMs = c.getLong("reproMs");
            entry.pregnancyProgressMs = c.contains("pregMs") ? c.getLong("pregMs") : 0L;
            entry.pregnant = c.contains("pregnant") && c.getBoolean("pregnant");
            entry.lastNutritionDecreaseMs = c.getLong("lastNutrition");
            entry.lastHydrationDecreaseMs = c.getLong("lastHydration");
            entry.lastRegenerationMs = c.getLong("lastRegen");
            entry.lastProcessedWallMs = c.getLong("lastProcessed");
            entry.pendingHealHearts = c.getInt("pendingHeal");
            data.entries.put(uuid, entry);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Entry> e : entries.entrySet()) {
            Entry entry = e.getValue();
            CompoundTag c = new CompoundTag();
            c.putUUID("uuid", e.getKey());
            c.putString("type", entry.entityTypeId);
            c.putDouble("x", entry.x);
            c.putDouble("y", entry.y);
            c.putDouble("z", entry.z);
            c.putBoolean("male", entry.isMale);
            c.putInt("nutrition", entry.nutrition);
            c.putInt("hydration", entry.hydration);
            c.putLong("reproMs", entry.reproductionProgressMs);
            c.putLong("pregMs", entry.pregnancyProgressMs);
            c.putBoolean("pregnant", entry.pregnant);
            c.putLong("lastNutrition", entry.lastNutritionDecreaseMs);
            c.putLong("lastHydration", entry.lastHydrationDecreaseMs);
            c.putLong("lastRegen", entry.lastRegenerationMs);
            c.putLong("lastProcessed", entry.lastProcessedWallMs);
            c.putInt("pendingHeal", entry.pendingHealHearts);
            list.add(c);
        }
        tag.put("animals", list);
        return tag;
    }

    public Entry get(UUID uuid) {
        return entries.get(uuid);
    }

    public void put(UUID uuid, Entry entry) {
        entries.put(uuid, entry);
        setDirty();
    }

    public void remove(UUID uuid) {
        if (entries.remove(uuid) != null) {
            setDirty();
        }
    }

    public Map<UUID, Entry> allEntries() {
        return entries;
    }

    public int size() {
        return entries.size();
    }
}
