package com.anexohorror.client;

import com.anexohorror.AnexoHorror;
import com.anexohorror.ClientConfig;
import com.anexohorror.registry.ModSounds;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.client.server.IntegratedServer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Reproduce el video de advertencia a pantalla completa: fotogramas JPEG empaquetados en video/intro.bin
 * y el audio en sounds/intro.ogg. Mientras suena el jugador es invulnerable (solo en un mundo propio).
 */
public class IntroVideoScreen extends Screen {
    private static final ResourceLocation TEX = new ResourceLocation(AnexoHorror.MODID, "intro_video_frame");

    private byte[] data;
    private int fps;
    private int vw;
    private int vh;
    private int frames;
    private int[] offsets;
    private int[] lengths;
    private DynamicTexture texture;
    private long startMs;
    private int lastIdx = -1;
    private SoundInstance sound;
    private boolean failed;
    private boolean protectedPlayer;
    private boolean wasInvulnerable;

    public IntroVideoScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        if (texture != null || failed) return;
        try {
            loadData();
            texture = new DynamicTexture(new NativeImage(NativeImage.Format.RGBA, vw, vh, false));
            this.minecraft.getTextureManager().register(TEX, texture);
            decode(0);
            lastIdx = 0;
            startMs = Util.getMillis();
            sound = SimpleSoundInstance.forUI(ModSounds.INTRO.get(), 1.0F, 1.0F);
            this.minecraft.getSoundManager().play(sound);
            protect(true);
        } catch (Exception e) {
            LogUtils.getLogger().error("AnexoHorror: no se pudo reproducir el video de introduccion", e);
            failed = true;
        }
    }

    private void loadData() throws Exception {
        ResourceManager rm = this.minecraft.getResourceManager();
        try (InputStream in = rm.open(new ResourceLocation(AnexoHorror.MODID, "video/intro.bin"))) {
            data = in.readAllBytes();
        }
        ByteBuffer bb = ByteBuffer.wrap(data);
        fps = bb.getInt();
        vw = bb.getInt();
        vh = bb.getInt();
        frames = bb.getInt();
        offsets = new int[frames];
        lengths = new int[frames];
        int off = 16 + 4 * frames;
        for (int i = 0; i < frames; i++) {
            lengths[i] = bb.getInt();
            offsets[i] = off;
            off += lengths[i];
        }
    }

    private void decode(int idx) throws Exception {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(data, offsets[idx], lengths[idx]));
        if (img == null) return;
        int w = Math.min(img.getWidth(), vw);
        int h = Math.min(img.getHeight(), vh);
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        NativeImage ni = texture.getPixels();
        if (ni == null) return;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = px[y * w + x];
                int r = (argb >> 16) & 255;
                int g = (argb >> 8) & 255;
                int b = argb & 255;
                ni.setPixelRGBA(x, y, 0xFF000000 | (b << 16) | (g << 8) | r);
            }
        }
        texture.upload();
    }

    private void protect(boolean on) {
        try {
            IntegratedServer s = this.minecraft.getSingleplayerServer();
            if (s == null || this.minecraft.player == null) return;
            ServerPlayer sp = s.getPlayerList().getPlayer(this.minecraft.player.getUUID());
            if (sp == null) return;
            if (on) {
                wasInvulnerable = sp.isInvulnerable();
                sp.setInvulnerable(true);
                protectedPlayer = true;
            } else if (protectedPlayer) {
                sp.setInvulnerable(wasInvulnerable);
                protectedPlayer = false;
            }
        } catch (Exception ignored) {
            // proteccion opcional
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xFF000000);
        if (failed || texture == null) {
            this.onClose();
            return;
        }
        long elapsed = Util.getMillis() - startMs;
        int idx = (int) (elapsed * fps / 1000L);
        if (idx >= frames) {
            this.onClose();
            return;
        }
        if (idx != lastIdx) {
            try {
                decode(idx);
            } catch (Exception e) {
                failed = true;
            }
            lastIdx = idx;
        }
        float s = Math.min(this.width / (float) vw, this.height / (float) vh);
        int dw = Math.round(vw * s);
        int dh = Math.round(vh * s);
        g.blit(TEX, (this.width - dw) / 2, (this.height - dh) / 2, dw, dh, 0.0F, 0.0F, vw, vh, vw, vh);
        if (ClientConfig.ALLOW_SKIP.get()) {
            g.drawString(this.font, "ESC", this.width - 28, this.height - 14, 0x80FFFFFF, false);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return ClientConfig.ALLOW_SKIP.get();
    }

    @Override
    public boolean isPauseScreen() {
        return false; // pausar silenciaria el audio
    }

    @Override
    public void removed() {
        try {
            if (sound != null) this.minecraft.getSoundManager().stop(sound);
            if (texture != null) this.minecraft.getTextureManager().release(TEX);
        } catch (Exception ignored) {
            // limpieza
        }
        texture = null;
        protect(false);
        super.removed();
    }
}
