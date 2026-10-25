package com.anexohorror.entity;

import com.anexohorror.AnexoHorror;
import net.minecraft.resources.ResourceLocation;

/** Las presencias: seis planos 2D (imagenes originales) y un modelo 3D (.obj). */
public enum HorrorVariant {
    PEEKER("peeker", 94, 100, 1.00F, 1.15F),    // cara asomada
    GRIN("grin", 221, 136, 0.90F, 1.10F),       // sonrisa brillante
    SMILER("smiler", 163, 252, 2.50F, 0.00F),   // figura palida de cuello largo
    DRAWN("drawn", 212, 252, 2.30F, 0.00F),     // sonrisa dibujada
    CRAWLER("crawler", 241, 439, 1.90F, 0.00F), // criatura de pasillo
    EYE("eye", 199, 213, 1.00F, 1.20F),         // ojo en la oscuridad
    SLENDER("slender", 3.00F, 0.00F, 0.70F);    // modelo 3D: criatura de piernas largas sentada en su silla

    public final ResourceLocation texture;
    public final float height;
    public final float hover;
    public final float aspect;
    /** true = se dibuja con el modelo .obj en lugar de un plano. */
    public final boolean model;

    HorrorVariant(String name, int pxW, int pxH, float height, float hover) {
        this.texture = new ResourceLocation(AnexoHorror.MODID, "textures/entity/" + name + ".png");
        this.height = height;
        this.hover = hover;
        this.aspect = pxW / (float) pxH;
        this.model = false;
    }

    /** Variante 3D: aspect = ancho aproximado / alto, usado solo para comprobar visibilidad. */
    HorrorVariant(String name, float height, float hover, float aspect) {
        this.texture = new ResourceLocation(AnexoHorror.MODID, "textures/entity/" + name + ".png");
        this.height = height;
        this.hover = hover;
        this.aspect = aspect;
        this.model = true;
    }

    public static HorrorVariant byId(int i) {
        HorrorVariant[] v = values();
        return v[Math.floorMod(i, v.length)];
    }
}
