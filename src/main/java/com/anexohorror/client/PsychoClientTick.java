package com.anexohorror.client;

import com.anexohorror.AnexoHorror;
import com.anexohorror.ClientConfig;
import com.anexohorror.entity.HorrorEntity;
import com.anexohorror.entity.HorrorMode;
import com.anexohorror.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Random;

/** Estado de cliente: mirada hacia las presencias, frases, signos y flashbacks periodicos. */
@Mod.EventBusSubscriber(modid = AnexoHorror.MODID, value = Dist.CLIENT)
public final class PsychoClientTick {
    private static final String[] PHRASES = {
            "NO TE ESCONDAS", "TE PUEDO VER", "NO ESTÁS SOLO", "EL FINAL ESTÁ CERCA",
            "FALTA POCO", "HUELO TU MUERTE", "DIOS NO LLEGA AQUÍ"};

    private static final Random RNG = new Random();
    private static final ArrayDeque<Integer> RECENT = new ArrayDeque<>();

    private static float intensity;
    private static int stare;
    private static int beat;
    private static boolean wasInView;
    private static int sinceText = 1000;
    private static int flashCountdown = -1;
    private static int lastFlash = -1;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            intensity = 0.0F;
            stare = 0;
            wasInView = false;
            flashCountdown = -1;
            PsychoOverlay.reset();
            return;
        }
        if (mc.isPaused()) return;

        // ---- mirada hacia presencias
        float best = 0.0F;
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        for (Entity en : mc.level.entitiesForRendering()) {
            if (!(en instanceof HorrorEntity he)) continue;
            if (he.getMode() == HorrorMode.PERIPHERAL) continue;
            Vec3 to = he.position().add(0.0D, he.spriteHover() + he.spriteHeight() * 0.6D, 0.0D).subtract(eye);
            double d = to.length();
            if (d > 64.0D || d < 0.5D) continue;
            double dot = look.dot(to.scale(1.0D / d));
            if (dot < 0.6D || !p.hasLineOfSight(he)) continue;
            float prox = (float) (1.0D - d / 64.0D);
            float strength = (float) ((dot - 0.6D) / 0.4D) * (0.35F + 0.65F * prox);
            if (he.getVariant().model) strength *= 1.3F;
            best = Math.max(best, strength);
        }
        intensity += (best - intensity) * 0.08F;
        stare = best > 0.15F ? Math.min(200, stare + 1) : Math.max(0, stare - 2);
        float g = Math.min(1.0F, intensity * (0.4F + 0.6F * stare / 200.0F));
        PsychoOverlay.glitch = g;

        if (g > 0.35F && ++beat % 34 == 0) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.HEARTBEAT.get(), 1.0F, 0.5F * g));
        }

        // ---- frase al aparecer una presencia en pantalla
        boolean inView = best > 0.12F;
        sinceText++;
        if (ClientConfig.SCREEN_TEXT.get()) {
            if (inView && !wasInView && sinceText > 160) triggerText();
            else if (inView && stare > 150 && sinceText > 500) triggerText();
        }
        wasInView = inView;
        if (PsychoOverlay.textTicks > 0) PsychoOverlay.textTicks--;

        // ---- signos extranos: pocos y efimeros
        for (int i = PsychoOverlay.SYMBOL_LIST.size() - 1; i >= 0; i--) {
            if (--PsychoOverlay.SYMBOL_LIST.get(i).ttl <= 0) PsychoOverlay.SYMBOL_LIST.remove(i);
        }
        boolean active = PsychoOverlay.textTicks > 0 || g > 0.25F || PsychoOverlay.flashTicks > 0;
        if (ClientConfig.SYMBOLS.get() && active && PsychoOverlay.SYMBOL_LIST.size() < 6 && RNG.nextFloat() < 0.12F) {
            PsychoOverlay.Symbol sy = new PsychoOverlay.Symbol();
            sy.x = 0.03F + RNG.nextFloat() * 0.92F;
            sy.y = 0.05F + RNG.nextFloat() * 0.88F;
            sy.scale = 1.0F + RNG.nextFloat() * 2.5F;
            sy.total = sy.ttl = 8 + RNG.nextInt(18);
            sy.color = new int[]{0xB0101C, 0xB9AAB0, 0x7A5A80, 0x8A1020}[RNG.nextInt(4)];
            sy.glyph = String.valueOf(PsychoOverlay.GLYPHS.charAt(RNG.nextInt(PsychoOverlay.GLYPHS.length())));
            PsychoOverlay.SYMBOL_LIST.add(sy);
        }

        // ---- flashbacks periodicos
        if (PsychoOverlay.flashTicks > 0) PsychoOverlay.flashTicks--;
        if (!ClientConfig.FLASHBACKS.get()) return;
        if (flashCountdown < 0) flashCountdown = 3600 + RNG.nextInt(2400); // primero, tras 3-5 min
        if (mc.screen == null && p.isAlive() && --flashCountdown <= 0) {
            triggerFlash();
            int min = ClientConfig.FLASH_MIN_MINUTES.get();
            int max = Math.max(min, ClientConfig.FLASH_MAX_MINUTES.get());
            double minutes = min + RNG.nextDouble() * (max - min);
            long t = mc.level.getDayTime() % 24000L;
            if (t >= 13000L && t < 23000L) minutes *= 0.7D; // de noche, mas seguidos
            flashCountdown = (int) (minutes * 1200.0D);
        }
    }

    public static void triggerText() {
        int idx;
        int tries = 0;
        do {
            idx = RNG.nextInt(PHRASES.length);
        } while (RECENT.contains(idx) && ++tries < 20);
        RECENT.addLast(idx);
        while (RECENT.size() > 3) RECENT.removeFirst();
        PsychoOverlay.text = PHRASES[idx];
        PsychoOverlay.textTicks = PsychoOverlay.textTotal = 70;
        PsychoOverlay.textYFrac = 0.18F + RNG.nextFloat() * 0.50F;
        sinceText = 0;
    }

    public static void triggerFlash() {
        int img = RNG.nextInt(2);
        if (img == lastFlash && RNG.nextBoolean()) img = 1 - img;
        lastFlash = img;
        PsychoOverlay.flashImage = img;
        PsychoOverlay.flashTicks = PsychoOverlay.flashTotal = 8 + RNG.nextInt(9);
        // una sola fractura limpia en el momento del golpe
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(ModSounds.CRACK.get(), 0.9F + RNG.nextFloat() * 0.2F, 1.0F));
    }

    private PsychoClientTick() {}
}
