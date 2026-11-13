package com.anexohorror.entity;

/**
 * Geometria compartida (servidor y cliente) de un asomo: la figura se dibuja como una cinta de
 * ROWS tiras cuya columna vertebral se curva hacia el lado por el que sale y cuya cabeza se ladea
 * aparte, por encima del cuello. Sin dependencias de Minecraft.
 */
public final class PeekPose {
    public static final int ROWS = 24;
    /** Inclinacion maxima del cuerpo (extremo superior) y ladeo extra de la cabeza, en grados. */
    public static final float LEAN_DEG = 21.0F;
    public static final float TILT_DEG = 27.0F;

    private static final float[] POW = new float[ROWS + 1];
    static {
        for (int j = 0; j <= ROWS; j++) POW[j] = (float) Math.pow(j / (double) ROWS, 1.7D);
    }

    public static float smooth(float a, float b, float x) {
        float t = Math.max(0.0F, Math.min(1.0F, (x - a) / (b - a)));
        return t * t * (3.0F - 2.0F * t);
    }

    public static float leanRad(float e, float amp) {
        return (float) Math.toRadians(LEAN_DEG) * amp * e;
    }

    public static float tiltRad(float e, float amp, float twitch) {
        return (float) Math.toRadians(TILT_DEG) * amp * smooth(0.25F, 0.9F, e) * twitch;
    }

    /**
     * Rellena ROWS+1 nodos de la columna: posicion (x lateral, y vertical) y angulo phi.
     * height ya incluye la escala; neckFromTop es la fraccion superior que ocupa la cabeza.
     */
    public static void spine(float height, float neckFromTop, float leanRad, float tiltRad,
                             float[] x, float[] y, float[] phi) {
        float neck = 1.0F - neckFromTop;
        for (int j = 0; j <= ROWS; j++) {
            float s = j / (float) ROWS;
            phi[j] = leanRad * POW[j] + tiltRad * smooth(neck - 0.12F, neck + 0.05F, s);
        }
        float dh = height / ROWS;
        x[0] = 0.0F;
        y[0] = 0.0F;
        for (int j = 0; j < ROWS; j++) {
            float mid = (phi[j] + phi[j + 1]) * 0.5F;
            x[j + 1] = x[j] + (float) Math.sin(mid) * dh;
            y[j + 1] = y[j] + (float) Math.cos(mid) * dh;
        }
    }

    /** Desplazamiento {lateral, vertical} del centro de la cabeza respecto al origen de la entidad. */
    public static float[] headOffset(HorrorVariant v, float scale, float leanRad, float tiltRad) {
        float[] x = new float[ROWS + 1];
        float[] y = new float[ROWS + 1];
        float[] phi = new float[ROWS + 1];
        spine(v.height * scale, v.headV1, leanRad, tiltRad, x, y, phi);
        float s = (1.0F - v.headCenterV()) * ROWS;
        int j = Math.max(0, Math.min(ROWS - 1, (int) s));
        float f = s - j;
        return new float[]{x[j] + (x[j + 1] - x[j]) * f, y[j] + (y[j + 1] - y[j]) * f};
    }

    private PeekPose() {}
}
