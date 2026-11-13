package com.anexohorror.director;

import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Lugar de asomo: la entidad esta escondida tras una cobertura y sale hacia "lean" hasta quedar
 * en "shown". La cobertura se calcula desde el punto de vista del jugador al aparecer.
 */
public final class CoverSpot {
    /** Pies de la entidad en la pose asomada (la entidad permanece aqui; solo se dibuja desplazada). */
    public final Vec3 shown;
    /** Horizontal unitario desde el escondite hacia la pose asomada. */
    public final Vec3 lean;
    /** Distancia horizontal escondite -> pose asomada. 0 = sin desplazamiento (aparece inclinandose). */
    public final double hide;
    /** Borde donde apoya la mano (mundo) o null. */
    @Nullable public final Vec3 grip;
    public final float scale;
    /** Espera a que el jugador mire hacia alli antes de empezar a salir. */
    public final boolean gaze;

    public CoverSpot(Vec3 shown, Vec3 lean, double hide, @Nullable Vec3 grip, float scale, boolean gaze) {
        this.shown = shown;
        this.lean = lean;
        this.hide = hide;
        this.grip = grip;
        this.scale = scale;
        this.gaze = gaze;
    }

    /** Sin cobertura (pruebas por comando): aparece en el sitio y se inclina. */
    public static CoverSpot open(Vec3 feet, Vec3 lean) {
        return new CoverSpot(feet, lean, 0.0D, null, 1.0F, false);
    }
}
