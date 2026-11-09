package com.anexohorror.client;

import com.anexohorror.AnexoHorror;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.net.URI;

/** Pantalla de bienvenida y creditos que aparece al entrar a un mundo o servidor. */
public final class CreditsScreen extends Screen {
    private static final ResourceLocation FACE =
            new ResourceLocation(AnexoHorror.MODID, "textures/gui/credits_face.png");
    private static final String CHANNEL_URL = "https://www.youtube.com/@NexoGamer-t5h";
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public CreditsScreen() {
        super(Component.literal("ANEXOHORROR // CREDITOS"));
    }

    @Override
    protected void init() {
        panelW = Math.min(540, this.width - 28);
        panelH = 246;
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int buttonY = panelY + panelH - 43;
        int contentX = panelX + 154;
        int available = panelW - 174;
        int linkWidth = Math.min(210, Math.max(150, available - 118));
        addRenderableWidget(Button.builder(Component.literal("CANAL DE YOUTUBE"), b -> {
            try {
                Util.getPlatform().openUri(URI.create(CHANNEL_URL));
            } catch (Exception ignored) {
                // El jugador conserva el enlace visible en pantalla si el sistema no puede abrirlo.
            }
        }).bounds(contentX, buttonY, linkWidth, 20).build());

        addRenderableWidget(Button.builder(Component.literal("CONTINUAR"), b -> onClose())
                .bounds(panelX + panelW - 108, buttonY, 92, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xE9000000);

        // Interferencias discretas alrededor del panel, como una tarjeta de cinta recuperada.
        for (int y = 9; y < this.height; y += 17) {
            g.fill(0, y, this.width, y + 1, 0x1600B9C7);
        }
        long tick = System.currentTimeMillis() / 170L;
        if (tick % 5L == 0L) {
            int bandY = (int) (tick * 13L % Math.max(1, this.height));
            g.fill(0, bandY, this.width, bandY + 2, 0x20C8D0D0);
        }

        // Marco de archivo.
        g.fill(panelX - 3, panelY - 3, panelX + panelW + 3, panelY + panelH + 3, 0xFF090909);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF20A0A0B);
        g.fill(panelX, panelY, panelX + panelW, panelY + 2, 0xFFD22A32);
        g.fill(panelX, panelY + panelH - 2, panelX + panelW, panelY + panelH, 0xFF682027);
        g.fill(panelX + 12, panelY + 38, panelX + panelW - 12, panelY + 39, 0x665D5D5D);

        g.drawString(this.font, "ANEXO 07  //  ARCHIVO RECUPERADO", panelX + 20, panelY + 16,
                0xFFB94B4B, false);
        g.drawString(this.font, "CAM-04     REC ●", panelX + panelW - 115, panelY + 16,
                0xFFB94B4B, false);

        int faceX = panelX + 24;
        int faceY = panelY + 66;
        g.fill(faceX - 4, faceY - 4, faceX + 116, faceY + 116, 0xFF171719);
        g.blit(FACE, faceX, faceY, 0, 0, 112, 112, 112, 112);
        g.fill(faceX - 4, faceY - 4, faceX + 116, faceY - 2, 0xFF7C252B);

        int textX = panelX + 154;
        g.drawString(this.font, "ANEXOHORROR", textX, panelY + 55, 0xFFE7E2DC, false);
        g.drawString(this.font, "VERSION " + AnexoHorror.VERSION, textX, panelY + 78, 0xFFD65A5A, false);
        g.drawString(this.font, "MINECRAFT FORGE 1.20.1", textX, panelY + 96, 0xFF9F9A94, false);

        g.drawString(this.font, "MOD CREADO POR", textX, panelY + 128, 0xFF8B8580, false);
        g.drawString(this.font, "NEXOGAMERTVS", textX, panelY + 145, 0xFFE5E0DA, false);
        g.drawString(this.font, "youtube.com/@NexoGamer-t5h", textX, panelY + 163, 0xFFB94B4B, false);

        g.drawString(this.font, "NO ESTAS SOLO. NO MIRES ATRAS.", panelX + 20, panelY + panelH - 66,
                0xFF77716C, false);
        g.drawString(this.font, "SEÑAL INESTABLE  //  TRANSMISION LOCAL", panelX + 20,
                panelY + panelH - 25, 0xFF55514E, false);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
