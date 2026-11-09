package com.anexohorror.client;

import com.anexohorror.AnexoHorror;
import com.anexohorror.ClientConfig;
import com.anexohorror.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Flash blanco de la camara y visor analogico mientras se sostiene en la mano. */
@Mod.EventBusSubscriber(modid = AnexoHorror.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CameraFlash {
    private static long flashStart = -1L;
    private static long flashDurationMs = 980L;

    /** Lo llama el servidor al tomar una foto. */
    public static void trigger(int ticks) {
        flashStart = System.currentTimeMillis();
        flashDurationMs = Math.max(300L, ticks * 70L);
    }

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("camera", (gui, g, partialTick, w, h) -> render(g, w, h));
    }

    private static void render(GuiGraphics g, int w, int h) {
        renderFlash(g, w, h);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
        Item camera = ModItems.CAMERA.get();
        if (!mc.player.getMainHandItem().is(camera) && !mc.player.getOffhandItem().is(camera)) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;
        renderViewfinder(g, mc, camera, w, h);
    }

    private static void renderFlash(GuiGraphics g, int w, int h) {
        if (flashStart < 0L) return;
        long elapsed = System.currentTimeMillis() - flashStart;
        if (elapsed >= flashDurationMs) {
            flashStart = -1L;
            return;
        }
        float t = elapsed / (float) flashDurationMs;
        float a = t < 0.08F ? 1.0F : (float) Math.pow(1.0F - (t - 0.08F) / 0.92F, 2.2D);
        if (!ClientConfig.SCREEN_EFFECTS.get()) a *= 0.35F;
        g.fill(0, 0, w, h, argb(a, 0xFF, 0xFF, 0xFF));
    }

    private static void renderViewfinder(GuiGraphics g, Minecraft mc, Item camera, int w, int h) {
        int mx = (int) (w * 0.14F);
        int my = (int) (h * 0.14F);
        int len = Math.max(8, (int) (Math.min(w, h) * 0.06F));
        int th = Math.max(1, Math.round(h / 270.0F));
        int col = 0xCCE7E2DC;

        // esquinas del encuadre
        corner(g, mx, my, len, th, 1, 1, col);
        corner(g, w - mx, my, len, th, -1, 1, col);
        corner(g, mx, h - my, len, th, 1, -1, col);
        corner(g, w - mx, h - my, len, th, -1, -1, col);

        // cruz central
        int cx = w / 2;
        int cy = h / 2;
        int c = Math.max(3, len / 3);
        g.fill(cx - c, cy, cx + c + 1, cy + th, 0x99E7E2DC);
        g.fill(cx, cy - c, cx + th, cy + c + 1, 0x99E7E2DC);

        // REC parpadeante y estado del flash
        boolean blink = (System.currentTimeMillis() / 600L) % 2L == 0L;
        int tx = mx + len + 6;
        int ty = my + 4;
        g.drawString(mc.font, "REC", tx, ty, 0xFFD22A32, false);
        if (blink) g.drawString(mc.font, "\u25CF", tx + 22, ty, 0xFFD22A32, false);
        g.drawString(mc.font, "CAM-04", w - mx - len - 6 - mc.font.width("CAM-04"), ty, 0xFFB94B4B, false);

        boolean cooling = mc.player.getCooldowns().isOnCooldown(camera);
        Component status = Component.translatable(cooling ? "gui.anexohorror.camera.recharging" : "gui.anexohorror.camera.ready");
        g.drawString(mc.font, status, tx, h - my - 12, cooling ? 0xFF8B3036 : 0xFF9F9A94, false);
        Component hint = Component.translatable("gui.anexohorror.camera.hint");
        g.drawString(mc.font, hint, w - mx - len - 6 - mc.font.width(hint), h - my - 12, 0xFF77716C, false);
    }

    private static void corner(GuiGraphics g, int x, int y, int len, int th, int dx, int dy, int col) {
        int hx0 = dx > 0 ? x : x - len;
        int hx1 = dx > 0 ? x + len : x;
        int hy0 = dy > 0 ? y : y - th;
        int hy1 = dy > 0 ? y + th : y;
        g.fill(hx0, hy0, hx1, hy1, col);
        int vx0 = dx > 0 ? x : x - th;
        int vx1 = dx > 0 ? x + th : x;
        int vy0 = dy > 0 ? y : y - len;
        int vy1 = dy > 0 ? y + len : y;
        g.fill(vx0, vy0, vx1, vy1, col);
    }

    private static int argb(float alpha, int r, int gr, int b) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255.0F)));
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }

    private CameraFlash() {}
}
