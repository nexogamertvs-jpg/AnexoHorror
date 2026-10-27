package com.anexohorror;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

/** Opciones solo del cliente: video de introduccion, flashbacks y efectos de pantalla. */
public final class ClientConfig {
    public enum IntroMode { NIGHTFALL, NEW_WORLD, OFF }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<IntroMode> INTRO_MODE;
    public static final ForgeConfigSpec.BooleanValue ALLOW_SKIP;
    public static final ForgeConfigSpec.BooleanValue FLASHBACKS;
    public static final ForgeConfigSpec.IntValue FLASH_MIN_MINUTES;
    public static final ForgeConfigSpec.IntValue FLASH_MAX_MINUTES;
    public static final ForgeConfigSpec.BooleanValue SCREEN_TEXT;
    public static final ForgeConfigSpec.BooleanValue SYMBOLS;
    public static final ForgeConfigSpec.BooleanValue GLOW;
    public static final ForgeConfigSpec.DoubleValue GLOW_STRENGTH;
    public static final ForgeConfigSpec.BooleanValue SCREEN_EFFECTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SEEN_WORLDS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("intro");
        INTRO_MODE = b.comment("NIGHTFALL = el video sale cada vez que anochece; NEW_WORLD = solo la primera vez en cada mundo/servidor; OFF = nunca.")
                .defineEnum("introMode", IntroMode.NIGHTFALL);
        ALLOW_SKIP = b.comment("Permite saltar el video con ESC.")
                .define("allowSkip", true);
        SEEN_WORLDS = b.comment("Mundos donde ya se mostro el video (se rellena solo).")
                .defineList("seenWorlds", new ArrayList<String>(), o -> o instanceof String);
        b.pop();
        b.push("flashbacks");
        FLASHBACKS = b.comment("Imagenes que cubren la pantalla de golpe de forma periodica. Pon false si eres sensible a destellos.")
                .define("enabled", true);
        FLASH_MIN_MINUTES = b.defineInRange("minMinutes", 4, 1, 60);
        FLASH_MAX_MINUTES = b.defineInRange("maxMinutes", 10, 1, 120);
        b.pop();
        b.push("screen");
        SCREEN_EFFECTS = b.comment("Tinte, vineta, escaneo y grano al mirar una presencia.").define("effects", true);
        SCREEN_TEXT = b.comment("Frases en pantalla cuando aparece una presencia.").define("text", true);
        SYMBOLS = b.comment("Signos extranos que aparecen de forma ocasional.").define("symbols", true);
        GLOW = b.comment("Aura luminosa suave alrededor de las presencias.").define("glow", true);
        GLOW_STRENGTH = b.comment("Intensidad del aura (1.0 = normal, 0.5 = mas tenue).").defineInRange("glowStrength", 1.0D, 0.0D, 3.0D);
        b.pop();
        SPEC = b.build();
    }

    private ClientConfig() {}
}
