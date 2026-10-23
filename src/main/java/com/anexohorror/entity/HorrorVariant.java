package com.anexohorror.entity;

import com.anexohorror.AnexoHorror;
import net.minecraft.resources.ResourceLocation;

/** Las seis presencias, una por imagen proporcionada. Dimensiones de textura = archivos reales. */
public enum HorrorVariant {
    PEEKER("peeker", 94, 100, 1.00F, 1.15F),    // cara asomada
    GRIN("grin", 221, 136, 0.90F, 1.10F),       // sonrisa brillante
    SMILER("smiler", 163, 252, 2.50F, 0.00F),   // figura palida de cuello largo
    DRAWN("drawn", 212, 252, 2.30F, 0.00F),     // sonrisa dibujada
    CRAWLER("crawler", 241, 439, 1.90F, 0.00F), // criatura de pasillo
    EYE("eye", 199, 213, 1.00F, 1.20F);         // ojo en la oscuridad

    public final ResourceLocation texture;
    public final float height;
    public final float hover;
    public final float aspect;

    HorrorVariant(String name, int pxW, int pxH, float height, float hover) {
        this.texture = new ResourceLocation(AnexoHorror.MODID, "textures/entity/" + name + ".png");
        this.height = height;
        this.hover = hover;
        this.aspect = pxW / (float) pxH;
    }

    public static HorrorVariant byId(int i) {
        HorrorVariant[] v = values();
        return v[Math.floorMod(i, v.length)];
    }
}
