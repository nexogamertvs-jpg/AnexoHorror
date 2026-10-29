package com.anexohorror.entity;

public enum HorrorMode {
    STARE, PEEK, PERIPHERAL, LOW, HUNT;

    public static HorrorMode byId(int i) {
        HorrorMode[] v = values();
        return v[Math.floorMod(i, v.length)];
    }
}
