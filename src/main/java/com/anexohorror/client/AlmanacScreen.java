package com.anexohorror.client;

import com.anexohorror.almanac.Almanac;
import com.anexohorror.almanac.AlmanacEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Random;

/**
 * Almanaque de entidades: todas aparecen bloqueadas hasta que se fotografian con la camara.
 * Mismo lenguaje visual que CreditsScreen (archivo recuperado, escaneo, rojo apagado).
 */
public final class AlmanacScreen extends Screen {
    private static final int COLS = 4;
    private static final int GAP = 6;

    private final int mask;
    private int panelX, panelY, panelW, panelH;
    private int gridX, gridY, cardW, cardH, stripY;
    private int selected = -1;

    public AlmanacScreen(int mask) {
        super(Component.translatable("gui.anexohorror.almanac.title"));
        this.mask = mask;
    }

    public static void open(int mask) {
        Minecraft.getInstance().setScreen(new AlmanacScreen(mask));
    }

    @Override
    protected void init() {
        panelW = Math.min(560, this.width - 20);
        panelH = Math.min(300, this.height - 10);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int headerH = 36;
        int stripH = 40;
        int footerH = 28;
        gridX = panelX + 14;
        int gridW = panelW - 28;
        cardW = (gridW - GAP * (COLS - 1)) / COLS;
        gridY = panelY + headerH + 4;
        int rows = (AlmanacEntry.COUNT + COLS - 1) / COLS;
        int availH = panelH - headerH - 4 - stripH - footerH - 8;
        cardH = Math.max(40, (availH - GAP * (rows - 1)) / rows);
        stripY = gridY + rows * cardH + GAP * (rows - 1) + 4;

        addRenderableWidget(Button.builder(Component.translatable("gui.anexohorror.almanac.close"), b -> onClose())
                .bounds(panelX + panelW - 100, panelY + panelH - 24, 86, 18).build());
    }

    private int cardX(int i) { return gridX + (i % COLS) * (cardW + GAP); }
    private int cardY(int i) { return gridY + (i / COLS) * (cardH + GAP); }

    private int cardAt(double mx, double my) {
        for (int i = 0; i < AlmanacEntry.COUNT; i++) {
            int x = cardX(i);
            int y = cardY(i);
            if (mx >= x && mx < x + cardW && my >= y && my < y + cardH) return i;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int i = cardAt(mx, my);
            if (i >= 0) {
                selected = i;
                Minecraft.getInstance().getSoundManager()
                        .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xE9000000);

        // interferencias discretas
        for (int y = 9; y < this.height; y += 17) g.fill(0, y, this.width, y + 1, 0x1600B9C7);
        long tick = System.currentTimeMillis() / 170L;
        if (tick % 5L == 0L) {
            int bandY = (int) (tick * 13L % Math.max(1, this.height));
            g.fill(0, bandY, this.width, bandY + 2, 0x20C8D0D0);
        }

        // marco de archivo
        g.fill(panelX - 3, panelY - 3, panelX + panelW + 3, panelY + panelH + 3, 0xFF090909);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF20A0A0B);
        g.fill(panelX, panelY, panelX + panelW, panelY + 2, 0xFFD22A32);
        g.fill(panelX, panelY + panelH - 2, panelX + panelW, panelY + panelH, 0xFF682027);
        g.fill(panelX + 12, panelY + 31, panelX + panelW - 12, panelY + 32, 0x665D5D5D);

        g.drawString(this.font, this.title, panelX + 20, panelY + 12, 0xFFB94B4B, false);
        Component count = Component.translatable("gui.anexohorror.almanac.count", Almanac.count(mask), AlmanacEntry.COUNT);
        g.drawString(this.font, count, panelX + panelW - 20 - this.font.width(count), panelY + 12, 0xFFB94B4B, false);

        int hovered = cardAt(mouseX, mouseY);
        for (int i = 0; i < AlmanacEntry.COUNT; i++) {
            renderCard(g, i, i == hovered || i == selected);
        }
        renderStrip(g, hovered >= 0 ? hovered : selected);

        g.drawString(this.font, Component.translatable("gui.anexohorror.almanac.footer"),
                panelX + 20, panelY + panelH - 19, 0xFF55514E, false);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderCard(GuiGraphics g, int i, boolean highlight) {
        AlmanacEntry entry = AlmanacEntry.values()[i];
        boolean unlocked = Almanac.has(mask, entry);
        int x = cardX(i);
        int y = cardY(i);
        int border = highlight ? 0xFFD22A32 : (unlocked ? 0xFF5B2328 : 0xFF2A2A2D);
        g.fill(x, y, x + cardW, y + cardH, border);
        g.fill(x + 1, y + 1, x + cardW - 1, y + cardH - 1, unlocked ? 0xFF121214 : 0xFF0B0B0C);

        int cx = x + cardW / 2;
        int nameY = y + cardH - 11;
        if (unlocked) {
            int areaW = cardW - 10;
            int areaH = cardH - 20;
            float s = Math.min(areaW / (float) entry.portraitW, areaH / (float) entry.portraitH);
            int dw = Math.max(1, (int) (entry.portraitW * s));
            int dh = Math.max(1, (int) (entry.portraitH * s));
            int dx = cx - dw / 2;
            int dy = y + 5 + (areaH - dh) / 2;
            g.blit(entry.portrait, dx, dy, dw, dh, 0.0F, 0.0F, entry.portraitW, entry.portraitH,
                    entry.portraitW, entry.portraitH);
            for (int yy = y + 2; yy < y + cardH - 2; yy += 3) g.fill(x + 1, yy, x + cardW - 1, yy + 1, 0x22000000);
            g.drawCenteredString(this.font, entry.displayName(), cx, nameY, 0xFFE7E2DC);
        } else {
            // ruido de senal perdida
            Random r = new Random(i * 7919L + System.currentTimeMillis() / 140L);
            for (int k = 0; k < 5; k++) {
                int ly = y + 3 + r.nextInt(Math.max(1, cardH - 6));
                int lx = x + 3 + r.nextInt(Math.max(1, cardW / 2));
                int lw = 6 + r.nextInt(Math.max(1, cardW / 2 - 6));
                g.fill(lx, ly, Math.min(lx + lw, x + cardW - 3), ly + 1, 0x24C8D0D0);
            }
            g.drawString(this.font, String.format("ID-%02d", i + 1), x + 5, y + 5, 0xFF3A3A3D, false);
            g.pose().pushPose();
            g.pose().translate(cx, y + (cardH - 12) / 2.0F, 0.0F);
            g.pose().scale(3.0F, 3.0F, 1.0F);
            g.drawCenteredString(this.font, "?", 0, -4, 0xFF3A3A3D);
            g.pose().popPose();
            g.drawCenteredString(this.font, Component.translatable("gui.anexohorror.almanac.locked_short"), cx, nameY, 0xFF5A5A5E);
        }
    }

    private void renderStrip(GuiGraphics g, int index) {
        int x = panelX + 14;
        int w = panelW - 28;
        g.fill(x, stripY, x + w, stripY + 40, 0xFF2A2A2D);
        g.fill(x + 1, stripY + 1, x + w - 1, stripY + 39, 0xFF0E0E10);
        if (index < 0) {
            g.drawString(this.font, Component.translatable("gui.anexohorror.almanac.prompt"), x + 8, stripY + 16, 0xFF77716C, false);
            return;
        }
        AlmanacEntry entry = AlmanacEntry.values()[index];
        boolean unlocked = Almanac.has(mask, entry);
        Component kind = Component.translatable(entry.boss ? "gui.anexohorror.almanac.kind.boss" : "gui.anexohorror.almanac.kind.presence");
        if (unlocked) {
            g.drawString(this.font, entry.displayName(), x + 8, stripY + 5, 0xFFE7E2DC, false);
            g.drawString(this.font, kind, x + w - 8 - this.font.width(kind), stripY + 5, 0xFFD65A5A, false);
            List<FormattedCharSequence> lines = this.font.split(entry.description(), w - 16);
            for (int l = 0; l < Math.min(2, lines.size()); l++) {
                g.drawString(this.font, lines.get(l), x + 8, stripY + 18 + l * 10, 0xFF9F9A94, false);
            }
        } else {
            g.drawString(this.font, "???", x + 8, stripY + 5, 0xFF5A5A5E, false);
            g.drawString(this.font, kind, x + w - 8 - this.font.width(kind), stripY + 5, 0xFF3A3A3D, false);
            List<FormattedCharSequence> lines = this.font.split(Component.translatable("gui.anexohorror.almanac.locked_hint"), w - 16);
            for (int l = 0; l < Math.min(2, lines.size()); l++) {
                g.drawString(this.font, lines.get(l), x + 8, stripY + 18 + l * 10, 0xFF77716C, false);
            }
        }
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
