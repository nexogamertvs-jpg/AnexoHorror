package com.anexohorror.entity;

import com.anexohorror.AnexoHorror;
import net.minecraft.resources.ResourceLocation;

/** Las presencias: seis planos 2D (imagenes recortadas, sin fondo) y un modelo 3D (.obj). Los nombres de variante se conservan por compatibilidad. */
public enum HorrorVariant {
    PEEKER("peeker", 172, 448, 2.90F, 0.00F),   // figura negra alta con mascara palida (se asoma por esquinas)
    GRIN("grin", 279, 448, 2.40F, 0.00F),       // esqueletica de mandibula abierta (resalta en la oscuridad)
    SMILER("smiler", 286, 448, 3.20F, 0.00F),   // muchos brazos y sombrero
    DRAWN("drawn", 156, 448, 2.70F, 0.00F),     // hombre alto y encorvado
    CRAWLER("crawler", 448, 441, 2.00F, 0.00F), // criatura con cuernos y garras
    EYE("eye", 263, 448, 2.60F, 0.00F),         // gritador de espinas
    SLENDER("slender", 3.00F, 0.00F, 0.70F),    // modelo 3D: criatura de piernas largas sentada en su silla
    STALKER("stalker", 194, 448, 3.00F, 0.00F); // silueta negra de garras largas

    public final ResourceLocation texture;
    public final float height;
    public final float hover;
    public final float aspect;
    /** true = se dibuja con el modelo .obj en lugar de un plano. */
    public final boolean model;
    /** Aura luminosa: textura y tamano relativos al sprite. */
    public final ResourceLocation glowTexture;
    public final float glowScaleX;
    public final float glowScaleY;
    public final float glowPadY;

    HorrorVariant(String name, int pxW, int pxH, float height, float hover) {
        this.texture = new ResourceLocation(AnexoHorror.MODID, "textures/entity/" + name + ".png");
        this.height = height;
        this.hover = hover;
        this.aspect = pxW / (float) pxH;
        this.model = false;
        int pad = Math.round(Math.max(pxW, pxH) * 0.16F); // igual que al generar *_glow.png
        this.glowTexture = new ResourceLocation(AnexoHorror.MODID, "textures/entity/" + name + "_glow.png");
        this.glowScaleX = (pxW + 2 * pad) / (float) pxW;
        this.glowScaleY = (pxH + 2 * pad) / (float) pxH;
        this.glowPadY = pad / (float) pxH;
    }

    /** Variante 3D: aspect = ancho aproximado / alto, usado solo para comprobar visibilidad. */
    HorrorVariant(String name, float height, float hover, float aspect) {
        this.texture = new ResourceLocation(AnexoHorror.MODID, "textures/entity/" + name + ".png");
        this.height = height;
        this.hover = hover;
        this.aspect = aspect;
        this.model = true;
        this.glowTexture = new ResourceLocation(AnexoHorror.MODID, "textures/entity/aura.png");
        this.glowScaleX = 1.7F;
        this.glowScaleY = 1.25F;
        this.glowPadY = 0.08F;
    }

    public static HorrorVariant byId(int i) {
        HorrorVariant[] v = values();
        return v[Math.floorMod(i, v.length)];
    }
}
