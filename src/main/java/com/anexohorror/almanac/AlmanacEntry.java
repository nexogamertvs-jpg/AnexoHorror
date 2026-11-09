package com.anexohorror.almanac;

import com.anexohorror.AnexoHorror;
import com.anexohorror.entity.HorrorEntity;
import com.anexohorror.entity.OtherEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

/**
 * Entradas del almanaque. El orden de las siete primeras coincide con HorrorVariant
 * (PEEKER..STALKER); OTHER es la entidad de jefe. El ordinal es el bit dentro de la mascara guardada.
 */
public enum AlmanacEntry {
    PEEKER("peeker", 37, 96, false),
    GRIN("grin", 60, 96, false),
    SMILER("smiler", 61, 96, false),
    DRAWN("drawn", 33, 96, false),
    CRAWLER("crawler", 98, 96, false),
    EYE("eye", 56, 96, false),
    STALKER("stalker", 42, 96, false),
    OTHER("other", 68, 96, true);

    public static final int COUNT = values().length;

    public final String id;
    public final ResourceLocation portrait;
    public final int portraitW;
    public final int portraitH;
    public final boolean boss;

    AlmanacEntry(String id, int portraitW, int portraitH, boolean boss) {
        this.id = id;
        this.portrait = new ResourceLocation(AnexoHorror.MODID, "textures/gui/almanac/" + id + ".png");
        this.portraitW = portraitW;
        this.portraitH = portraitH;
        this.boss = boss;
    }

    public Component displayName() { return Component.translatable("almanac.anexohorror." + id + ".name"); }
    public Component description() { return Component.translatable("almanac.anexohorror." + id + ".desc"); }

    /** Entrada del almanaque correspondiente a una entidad del mod, o null si no es una de ellas. */
    @Nullable
    public static AlmanacEntry of(Entity entity) {
        if (entity instanceof HorrorEntity he) return values()[he.getVariant().ordinal()];
        if (entity instanceof OtherEntity) return OTHER;
        return null;
    }
}
