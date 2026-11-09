package com.anexohorror.almanac;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Registro de entidades fotografiadas. Se guarda como mascara de bits en los datos persistentes
 * del jugador (PlayerPersisted), que Forge conserva al morir y al cambiar de dimension.
 */
public final class Almanac {
    private static final String KEY = "anexohorror_almanac";

    public static int getMask(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getInt(KEY);
    }

    /** Desbloquea la entrada; devuelve true solo si era nueva. */
    public static boolean unlock(Player player, AlmanacEntry entry) {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        int mask = persisted.getInt(KEY);
        int bit = 1 << entry.ordinal();
        if ((mask & bit) != 0) return false;
        persisted.putInt(KEY, mask | bit);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
        return true;
    }

    public static boolean has(int mask, AlmanacEntry entry) {
        return (mask & (1 << entry.ordinal())) != 0;
    }

    public static int count(int mask) {
        return Integer.bitCount(mask & ((1 << AlmanacEntry.COUNT) - 1));
    }

    private Almanac() {}
}
