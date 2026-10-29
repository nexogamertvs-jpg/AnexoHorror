package com.anexohorror;

import net.minecraftforge.common.ForgeConfigSpec;

public final class HorrorConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue INTENSITY;
    public static final ForgeConfigSpec.IntValue MAX_ENTITIES;
    public static final ForgeConfigSpec.IntValue MIN_DIST_DAY;
    public static final ForgeConfigSpec.IntValue MIN_DIST_NIGHT;
    public static final ForgeConfigSpec.BooleanValue FAKE_SOUNDS;
    public static final ForgeConfigSpec.BooleanValue SILENCE_EVENTS;
    public static final ForgeConfigSpec.BooleanValue ESCALATION;
    public static final ForgeConfigSpec.BooleanValue BLOCK_SLEEP;
    public static final ForgeConfigSpec.BooleanValue BED_SETS_SPAWN;
    public static final ForgeConfigSpec.BooleanValue NIGHT_ESCALATION;
    public static final ForgeConfigSpec.BooleanValue HUNT_ENABLED;
    public static final ForgeConfigSpec.DoubleValue HUNT_SPEED;
    public static final ForgeConfigSpec.BooleanValue HUNT_BREAKS_BLOCKS;
    public static final ForgeConfigSpec.IntValue HUNT_MIN_NIGHT;
    public static final ForgeConfigSpec.IntValue HUNT_COOLDOWN_MIN;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("general");
        ENABLED = b.comment("Activa o desactiva todas las apariciones y eventos.")
                .define("enabled", true);
        INTENSITY = b.comment("Multiplicador de frecuencia de eventos (1.0 = normal).")
                .defineInRange("intensity", 1.0D, 0.1D, 5.0D);
        MAX_ENTITIES = b.comment("Maximo de entidades visibles simultaneas por jugador (de noche).")
                .defineInRange("maxEntities", 3, 1, 8);
        MIN_DIST_DAY = b.comment("Distancia minima de aparicion de dia (bloques).")
                .defineInRange("minDistanceDay", 18, 8, 64);
        MIN_DIST_NIGHT = b.comment("Distancia minima de aparicion de noche (bloques).")
                .defineInRange("minDistanceNight", 10, 6, 48);
        b.pop();
        b.push("psychological");
        FAKE_SOUNDS = b.comment("Sonidos falsos posicionales (susurros, golpes, pasos) sin entidad.")
                .define("fakeSounds", true);
        SILENCE_EVENTS = b.comment("Silencios inquietantes (corta musica/ambiente unos segundos).")
                .define("silenceEvents", true);
        ESCALATION = b.comment("De noche, una presencia puede reaparecer algo mas cerca al apartar la vista (nunca persigue).")
                .define("escalation", true);
        b.pop();
        b.push("nights");
        BLOCK_SLEEP = b.comment("Impide dormir: cada intento provoca un screamer.").define("blockSleep", true);
        BED_SETS_SPAWN = b.comment("Aunque no puedas dormir, la cama sigue fijando tu punto de reaparicion.").define("bedSetsSpawn", true);
        NIGHT_ESCALATION = b.comment("Cada noche vivida aumenta un poco la frecuencia de eventos (hasta +96%).").define("nightEscalation", true);
        b.pop();
        b.push("hunt");
        HUNT_ENABLED = b.comment("Evento de persecucion: una presencia congela tu mirada y despues te persigue hasta alcanzarte (sin dano, solo screamer).")
                .define("enabled", true);
        HUNT_SPEED = b.comment("Velocidad de la persecucion en bloques por tick (0.40 = 8 bloques/s; correr es ~5.6).")
                .defineInRange("speed", 0.40D, 0.10D, 1.0D);
        HUNT_BREAKS_BLOCKS = b.comment("Rompe bloques en su camino (solo si la regla mobGriefing esta activa).")
                .define("breakBlocks", true);
        HUNT_MIN_NIGHT = b.comment("A partir de que noche vivida puede ocurrir.").defineInRange("minNight", 2, 1, 50);
        HUNT_COOLDOWN_MIN = b.comment("Minutos minimos entre persecuciones.").defineInRange("cooldownMinutes", 6, 1, 120);
        b.pop();
        SPEC = b.build();
    }

    private HorrorConfig() {}
}
