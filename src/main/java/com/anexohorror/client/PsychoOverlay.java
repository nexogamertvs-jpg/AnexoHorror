package com.anexohorror.client;

import com.anexohorror.AnexoHorror;
import com.anexohorror.ClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Capa de terror analogico: tinte/vineta/escaneo/grano, frases rojas, signos extranos, flashbacks,
 * screamers del sueno, apagones, estatica y marcas de camara VHS. El estado lo actualiza PsychoClientTick.
 */
@Mod.EventBusSubscriber(modid = AnexoHorror.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PsychoOverlay {
    /** 0..1, cuanto estas mirando a una presencia. */
    public static volatile float glitch;
    /** Grano suave de fondo durante la noche. */
    static float ambient;

    // frase en pantalla
    static String text;
    static int textTicks;
    static int textTotal;
    static float textYFrac = 0.4F;

    // signos
    static final class Symbol {
        float x, y, scale;
        int ttl, total, color;
        String glyph;
    }
    static final List<Symbol> SYMBOL_LIST = new ArrayList<>();
    static final String GLYPHS = "ЖΨΞΩΣЯ∴∵※†‡¶§ᚦᛉᛟᚷᛞᛃᚨ☥╳░▒";

    // imagenes (flashbacks usan las 2 primeras; los screamers del sueno, todas)
    private static final String[] TEX_NAMES = {"flash1", "flash2", "sc1", "sc2", "sc3", "sc4", "sc5", "sc6"};
    private static final int[] TEX_W = {712, 567, 676, 548, 736, 626, 512, 722};
    private static final int[] TEX_H = {1080, 1080, 643, 604, 793, 760, 652, 1000};
    static final int TEX_COUNT = TEX_NAMES.length;
    private static final ResourceLocation[] TEX = new ResourceLocation[TEX_COUNT];
    static {
        for (int i = 0; i < TEX_COUNT; i++) {
            TEX[i] = new ResourceLocation(AnexoHorror.MODID, "textures/gui/" + TEX_NAMES[i] + ".png");
        }
    }

    // flashback periodico
    static int flashTicks;
    static int flashTotal;
    static int flashImage;

    // screamer del sueno
    static int scTicks;
    static int scTotal;
    static int scImage;
    static String scText;

    // mirada congelada (persecucion)
    static int lockTicks;
    static int lockTotal;
    static String lockText;

    // apagon y estatica
    static int blackTicks;
    static int blackTotal;
    static int staticTicks;
    static int staticTotal;

    private static final Random RNG = new Random();

    private static Boolean[] avail;

    /** true si la textura existe en el jar (evita el cuadro negro y morado si falta algun archivo). */
    static boolean available(int idx) {
        if (avail == null) {
            avail = new Boolean[TEX_COUNT];
        }
        if (avail[idx] == null) {
            avail[idx] = Minecraft.getInstance().getResourceManager().getResource(TEX[idx]).isPresent();
        }
        return avail[idx];
    }

    static void reset() {
        glitch = 0.0F;
        ambient = 0.0F;
        textTicks = 0;
        flashTicks = 0;
        scTicks = 0;
        lockTicks = 0;
        blackTicks = 0;
        staticTicks = 0;
        SYMBOL_LIST.clear();
    }

    /** Fuerza del temblor de camara durante un screamer. */
    public static float screamerShake() {
        return scTicks > 0 ? 1.0F : (lockTicks > 0 ? 0.5F : 0.0F);
    }

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("psycho", (gui, g, partialTick, w, h) -> render(g, w, h));
    }

    private static void render(GuiGraphics g, int w, int h) {
        if (Minecraft.getInstance().options.hideGui) return;
        renderGlitch(g, w, h);
        renderVhsHud(g, w, h);
        renderSymbols(g, w, h);
        renderText(g, w, h);
        renderStatic(g, w, h);
        renderFlash(g, w, h);
        renderLock(g, w, h);
        renderScreamer(g, w, h);
        renderBlackout(g, w, h);
    }

    // ---------------------------------------------------------------- tinte, vineta, escaneo, grano

    private static void renderGlitch(GuiGraphics g, int w, int h) {
        float k = Math.max(glitch, ambient);
        if (k < 0.02F || !ClientConfig.SCREEN_EFFECTS.get()) return;

        g.fill(0, 0, w, h, argb((int) (70 * k), 0x3A, 0x28, 0x30));

        int layers = 10;
        int bw = Math.max(2, w / 60);
        int bh = Math.max(2, h / 60);
        for (int i = 0; i < layers; i++) {
            int a = (int) (110 * k * (layers - i) / (float) layers);
            int col = argb(a, 0x10, 0x08, 0x0A);
            g.fill(i * bw, 0, (i + 1) * bw, h, col);
            g.fill(w - (i + 1) * bw, 0, w - i * bw, h, col);
            g.fill(0, i * bh, w, (i + 1) * bh, col);
            g.fill(0, h - (i + 1) * bh, w, h - i * bh, col);
        }

        int scan = argb((int) (38 * k), 0, 0, 0);
        for (int y = 0; y < h; y += 6) g.fill(0, y, w, y + 1, scan);

        int grains = (int) (20 + 40 * k);
        for (int i = 0; i < grains; i++) {
            int x = RNG.nextInt(w);
            int y = RNG.nextInt(h);
            int s = 1 + RNG.nextInt(2);
            int c = RNG.nextBoolean() ? 0xFF : 0x20;
            g.fill(x, y, x + s, y + s, argb((int) (60 * k), c, c, c));
        }

        if (RNG.nextFloat() < 0.04F + 0.12F * k) {
            int bandH = 4 + RNG.nextInt(14);
            int y = RNG.nextInt(Math.max(1, h - bandH));
            int off = 2 + RNG.nextInt(6);
            g.fill(0, y, w, y + bandH, argb((int) (34 * k), 0xB9, 0xAA, 0xB0));
            g.fill(off, y, w, y + 1, argb((int) (90 * k), 0xFF, 0x30, 0x40));
            g.fill(0, y + bandH - 1, w - off, y + bandH, argb((int) (90 * k), 0x30, 0xC0, 0xFF));
        }
    }

    // ---------------------------------------------------------------- marcas de camara VHS (solo de noche)

    private static void renderVhsHud(GuiGraphics g, int w, int h) {
        if (!ClientConfig.VHS_HUD.get() || scTicks > 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long dt = mc.level.getDayTime();
        long t = dt % 24000L; // siempre visible: de dia y de noche, del primer dia al ultimo

        Font font = mc.font;
        float s = Math.max(1.0F, h / 400.0F);
        float m = 10.0F * s;
        int jitter = RNG.nextInt(40) == 0 ? 1 + RNG.nextInt(3) : 0; // temblor ocasional de la cinta

        // REC parpadeante + PLAY
        boolean on = (mc.level.getGameTime() / 20L) % 2L == 0L;
        if (on) g.fill((int) m + jitter, (int) m + (int) (2 * s), (int) (m + 5 * s) + jitter, (int) (m + 7 * s),
                argb(230, 0xD0, 0x10, 0x18));
        vhsText(g, font, "REC", m + 8 * s + jitter, m, s);
        vhsText(g, font, "PLAY >", m + jitter, m + 12 * s, s);

        // contador de cinta (arriba a la derecha)
        long sec = mc.level.getGameTime() / 20L;
        String counter = String.format("SP %d:%02d:%02d", sec / 3600L, (sec / 60L) % 60L, sec % 60L);
        vhsText(g, font, counter, w - font.width(counter) * s - m - jitter, m, s);

        // fecha y hora del mundo (abajo a la izquierda)
        int hour = (int) ((t / 1000L + 6L) % 24L);
        int min = (int) ((t % 1000L) * 60L / 1000L);
        String stamp = String.format("D\u00cdA %d   %02d:%02d", dt / 24000L + 1L, hour, min);
        vhsText(g, font, stamp, m + jitter, h - m - 9 * s, s);
    }

    private static void vhsText(GuiGraphics g, Font font, String s, float x, float y, float scale) {
        drawScaled(g, font, s, x + 1.5F * scale, y, scale, argb(70, 0xFF, 0x30, 0x40)); // fantasma cromatico
        drawScaled(g, font, s, x, y, scale, argb(205, 0xE8, 0xE4, 0xDC));
    }

    // ---------------------------------------------------------------- signos extranos (pocos, sin saturar)

    private static void renderSymbols(GuiGraphics g, int w, int h) {
        if (SYMBOL_LIST.isEmpty()) return;
        Font font = Minecraft.getInstance().font;
        for (Symbol sy : SYMBOL_LIST) {
            if (RNG.nextInt(5) == 0) continue; // parpadeo
            float life = sy.ttl / (float) sy.total;
            float in = Math.min(1.0F, (sy.total - sy.ttl + 1) / 3.0F);
            int alpha = (int) (150 * Math.min(1.0F, life * 4.0F) * in);
            if (alpha < 8) continue;
            drawScaled(g, font, sy.glyph, sy.x * w, sy.y * h, sy.scale, (alpha << 24) | sy.color);
        }
    }

    // ---------------------------------------------------------------- frases

    private static void renderText(GuiGraphics g, int w, int h) {
        if (textTicks <= 0 || text == null) return;
        int age = textTotal - textTicks;
        float a = Math.min(1.0F, age / 3.0F) * Math.min(1.0F, textTicks / 18.0F); // entrada seca, salida suave
        horrorText(g, text, w, h, h * textYFrac, 0.62F, a, age);
    }

    /** Frase roja con banda oscura, aberracion cromatica, resplandor, parpadeo y letras que se corrompen. */
    private static void horrorText(GuiGraphics g, String str, int w, int h, float yTop, float widthFrac, float a, int age) {
        Font font = Minecraft.getInstance().font;
        if (RNG.nextFloat() < 0.07F) a *= 0.25F;
        String t = str;
        if (RNG.nextFloat() < 0.10F) {
            char[] c = t.toCharArray();
            int i = RNG.nextInt(c.length);
            if (c[i] != ' ') c[i] = GLYPHS.charAt(RNG.nextInt(GLYPHS.length()));
            t = new String(c);
        }
        int tw = font.width(t);
        float s = Mth.clamp(w * widthFrac / Math.max(1, tw), 1.5F, 8.0F);
        float x = (w - tw * s) / 2.0F + (RNG.nextFloat() - 0.5F) * (age < 6 ? 14.0F : 3.0F);
        float y = yTop + (RNG.nextFloat() - 0.5F) * 3.0F;

        g.fill(0, (int) (y - 6 * s * 0.5F), w, (int) (y + 9 * s + 6 * s * 0.5F), argb((int) (110 * a), 0, 0, 0));
        drawScaled(g, font, t, x + 4.0F, y, s, argb((int) (90 * a), 0x30, 0xD0, 0xFF));
        drawScaled(g, font, t, x - 4.0F, y, s, argb((int) (120 * a), 0xFF, 0x20, 0x40));
        for (int[] o : new int[][]{{-2, 0}, {2, 0}, {0, -2}, {0, 2}}) {
            drawScaled(g, font, t, x + o[0], y + o[1], s, argb((int) (60 * a), 0xC8, 0x10, 0x1E));
        }
        drawScaled(g, font, t, x, y, s, argb((int) (255 * a), 0xC8, 0x10, 0x1E));
    }

    private static void drawScaled(GuiGraphics g, Font font, String s, float x, float y, float scale, int color) {
        if (((color >>> 24) & 255) < 8) return;
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(font, s, 0, 0, color, false);
        g.pose().popPose();
    }

    // ---------------------------------------------------------------- imagen a pantalla completa (flashback / screamer)

    /** Dibuja la imagen ENTERA (sin recortar) con fantasmas rojo/cian, y devuelve nada. */
    private static void drawFullImage(GuiGraphics g, int idx, int w, int h, float shake, float ghost) {
        int tw = TEX_W[idx];
        int th = TEX_H[idx];
        float s = Math.min(w / (float) tw, h / (float) th);
        int dw = Math.round(tw * s);
        int dh = Math.round(th * s);
        int x = (w - dw) / 2 + (int) ((RNG.nextFloat() - 0.5F) * w * shake);
        int y = (h - dh) / 2 + (int) ((RNG.nextFloat() - 0.5F) * h * shake);
        int off = Math.max(3, Math.round(h * 0.008F * ghost));
        ResourceLocation rl = TEX[idx];
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1.0F, 0.15F, 0.2F, 0.45F);
        g.blit(rl, x - off, y, dw, dh, 0.0F, 0.0F, tw, th, tw, th);
        g.setColor(0.1F, 0.9F, 1.0F, 0.45F);
        g.blit(rl, x + off, y, dw, dh, 0.0F, 0.0F, tw, th, tw, th);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(rl, x, y, dw, dh, 0.0F, 0.0F, tw, th, tw, th);
    }

    private static void scanAndGrain(GuiGraphics g, int w, int h, int grains) {
        int scan = argb(40, 0, 0, 0);
        for (int yy = 0; yy < h; yy += 5) g.fill(0, yy, w, yy + 1, scan);
        for (int i = 0; i < grains; i++) {
            int gx = RNG.nextInt(w);
            int gy = RNG.nextInt(h);
            g.fill(gx, gy, gx + 2, gy + 2, argb(70, 0xFF, 0xFF, 0xFF));
        }
    }

    private static void renderFlash(GuiGraphics g, int w, int h) {
        if (flashTicks <= 0) return;
        int age = flashTotal - flashTicks;
        if (age == 4 || age == 9) return; // el destello "tartamudea"
        g.fill(0, 0, w, h, 0xFF000000);
        drawFullImage(g, flashImage, w, h, 0.012F, 1.0F);
        scanAndGrain(g, w, h, 60);
    }

    private static void renderScreamer(GuiGraphics g, int w, int h) {
        if (scTicks <= 0) return;
        int age = scTotal - scTicks;
        g.fill(0, 0, w, h, 0xFF000000);
        if (age != 7 && age != 15) { // dos cortes de un fotograma: tartamudeo
            drawFullImage(g, scImage, w, h, age < 6 ? 0.035F : 0.014F, 1.6F);
        }
        scanAndGrain(g, w, h, 90);
        // vineta roja que respira
        int a = (int) (90 + 40 * Math.sin(age * 0.6D));
        int bw = Math.max(4, w / 24);
        for (int i = 0; i < 6; i++) {
            int c = argb(a * (6 - i) / 6, 0x70, 0x00, 0x08);
            g.fill(i * bw / 2, 0, (i + 1) * bw / 2, h, c);
            g.fill(w - (i + 1) * bw / 2, 0, w - i * bw / 2, h, c);
        }
        if (age < 3) g.fill(0, 0, w, h, argb(210 - age * 70, 0xFF, 0xFF, 0xFF)); // destello inicial
        if (age >= 5 && scText != null) {
            float fade = Math.min(1.0F, (age - 4) / 3.0F) * Math.min(1.0F, scTicks / 10.0F);
            horrorText(g, scText, w, h, h * 0.74F, 0.82F, fade, age);
        }
    }

    // ---------------------------------------------------------------- mirada congelada

    private static void renderLock(GuiGraphics g, int w, int h) {
        if (lockTicks <= 0 || lockText == null) return;
        int age = lockTotal - lockTicks;
        float k = Math.min(1.0F, age / 12.0F);
        int bw = Math.max(4, w / 20);
        for (int i = 0; i < 8; i++) { // vineta que se cierra
            int c = argb((int) (150 * k * (8 - i) / 8.0F), 0x08, 0x00, 0x02);
            g.fill(i * bw / 2, 0, (i + 1) * bw / 2, h, c);
            g.fill(w - (i + 1) * bw / 2, 0, w - i * bw / 2, h, c);
            g.fill(0, i * bw / 2, w, (i + 1) * bw / 2, c);
            g.fill(0, h - (i + 1) * bw / 2, w, h - i * bw / 2, c);
        }
        horrorText(g, lockText, w, h, h * 0.78F, 0.86F, Math.min(1.0F, age / 4.0F) * Math.min(1.0F, lockTicks / 8.0F), age);
    }

    // ---------------------------------------------------------------- apagon y estatica

    private static void renderBlackout(GuiGraphics g, int w, int h) {
        if (blackTicks <= 0) return;
        int age = blackTotal - blackTicks;
        int a = (age < 2 || blackTicks <= 2) ? 150 : 255; // entra y sale con un parpadeo
        g.fill(0, 0, w, h, argb(a, 0, 0, 0));
    }

    private static void renderStatic(GuiGraphics g, int w, int h) {
        if (staticTicks <= 0) return;
        g.fill(0, 0, w, h, argb(150, 0x18, 0x18, 0x1C));
        for (int i = 0; i < 160; i++) {
            int x = RNG.nextInt(w);
            int y = RNG.nextInt(h);
            int rw = 4 + RNG.nextInt(Math.max(5, w / 12));
            int rh = 1 + RNG.nextInt(3);
            int c = 90 + RNG.nextInt(166);
            g.fill(x, y, Math.min(w, x + rw), y + rh, argb(170, c, c, c));
        }
        int band = RNG.nextInt(Math.max(1, h - 30));
        g.fill(0, band, w, band + 6 + RNG.nextInt(24), argb(60, 0xFF, 0xFF, 0xFF));
    }

    private static int argb(int a, int r, int gr, int b) {
        a = Math.max(0, Math.min(255, a));
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }

    private PsychoOverlay() {}
}
