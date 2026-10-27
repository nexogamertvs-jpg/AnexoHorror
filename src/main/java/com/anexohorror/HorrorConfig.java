package com.anexohorror;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuracion comun del director de terror. Los valores son leidos en servidor;
 * los efectos de pantalla se configuran por separado en ClientConfig.
 */
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
    public static final ForgeConfigSpec.BooleanValue DAYTIME_PRESENCES;
    public static final ForgeConfigSpec.BooleanValue ARCHIVE_MESSAGES;
    public static final ForgeConfigSpec.BooleanValue HOME_ANOMALIES;
    public static final ForgeConfigSpec.BooleanValue ADAPTIVE_DIRECTOR;
    public static final ForgeConfigSpec.BooleanValue ENCOUNTER_MEMORY;
    public static final ForgeConfigSpec.IntValue ARCHIVE_COOLDOWN_SECONDS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("general");
        ENABLED = b.comment("Activa o desactiva todas las apariciones y eventos.")
                .define("enabled", true);
        INTENSITY = b.comment("Multiplicador de frecuencia de eventos (1.0 = normal; recomendado 1.0-1.25).")
                .defineInRange("intensity", 1.1D, 0.1D, 5.0D);
        MAX_ENTITIES = b.comment("Maximo de entidades visibles simultaneas por jugador durante la noche.")
                .defineInRange("maxEntities", 3, 1, 8);
        MIN_DIST_DAY = b.comment("Distancia minima de aparicion durante el dia.")
                .defineInRange("minDistanceDay", 22, 8, 64);
        MIN_DIST_NIGHT = b.comment("Distancia minima de aparicion durante la noche.")
                .defineInRange("minDistanceNight", 10, 6, 48);
        DAYTIME_PRESENCES = b.comment("Permite apariciones diurnas excepcionales; de dia nunca aparece mas de una presencia por jugador.")
                .define("daytimePresences", true);
        ADAPTIVE_DIRECTOR = b.comment("Adapta los pesos de los eventos a la hora, la oscuridad, el entorno y la reaccion del jugador.")
                .define("adaptiveDirector", true);
        ENCOUNTER_MEMORY = b.comment("Conserva miedo, audacia, nerviosismo y progreso narrativo entre sesiones.")
                .define("encounterMemory", true);
        b.pop();

        b.push("psychological");
        FAKE_SOUNDS = b.comment("Sonidos posicionales falsos sin entidad.")
                .define("fakeSounds", true);
        SILENCE_EVENTS = b.comment("Silencios inquietantes; respeta el control del cliente sobre audio y efectos.")
                .define("silenceEvents", true);
        ESCALATION = b.comment("Una presencia puede reaparecer algo mas cerca al apartar la vista, pero nunca persigue.")
                .define("escalation", true);
        HOME_ANOMALIES = b.comment("Permite golpes, crujidos y sonidos extraños en puertas o refugios.")
                .define("homeAnomalies", true);
        ARCHIVE_MESSAGES = b.comment("Activa mensajes narrativos fragmentados y expedientes anómalos.")
                .define("archiveMessages", true);
        ARCHIVE_COOLDOWN_SECONDS = b.comment("Tiempo minimo entre mensajes de archivo para evitar saturar el chat.")
                .defineInRange("archiveCooldownSeconds", 240, 60, 1800);
        b.pop();
        SPEC = b.build();
    }

    private HorrorConfig() {}
}
