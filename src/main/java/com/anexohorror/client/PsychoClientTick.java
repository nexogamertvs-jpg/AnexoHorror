package com.anexohorror.client;

import com.anexohorror.AnexoHorror;
import com.anexohorror.ClientConfig;
import com.anexohorror.entity.HorrorEntity;
import com.anexohorror.entity.HorrorMode;
import com.anexohorror.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Random;

/** Estado de cliente: mirada hacia las presencias, frases, signos, flashbacks, screamers, apagones y estatica. */
@Mod.EventBusSubscriber(modid = AnexoHorror.MODID, value = Dist.CLIENT)
public final class PsychoClientTick {
    private static final String[] PHRASES = {
            "NO TE ESCONDAS", "TE PUEDO VER", "NO EST\u00c1S SOLO", "EL FINAL EST\u00c1 CERCA",
            "FALTA POCO", "HUELO TU MUERTE", "DIOS NO LLEGA AQU\u00cd"};

    private static final Random RNG = new Random();
    private static final ArrayDeque<Integer> RECENT = new ArrayDeque<>();
    private static final ArrayDeque<Integer> RECENT_SC = new ArrayDeque<>();

    private static float intensity;
    private static int stare;
    private static int beat;
    private static boolean wasInView;
    private static int sinceText = 1000;
    private static int flashCountdown = -1;
    private static int lastFlash = -1;
    private static int tinnitusIn = -1;
    private static int signalBurstCooldown = -1;

    // mirada congelada
    private static int lockEntityId = -1;
    private static int lockWait;
    private static boolean lockActive;
    private static float lockYaw;
    private static float lockPitch;

    public static boolean isLockActive() { return lockActive; }
    public static float lockYaw() { return lockYaw; }
    public static float lockPitch() { return lockPitch; }

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
            tinnitusIn = -1;
            signalBurstCooldown = -1;
            lockActive = false;
            lockEntityId = -1;
            PsychoOverlay.reset();
            return;
        }
        if (mc.isPaused()) return;

        // ---- senal analogica permanente: apenas perceptible de dia, degradada de noche
        long dayT = mc.level.getDayTime() % 24000L;
        boolean night = dayT >= 13000L && dayT < 23000L;
        PsychoOverlay.ambient = ClientConfig.NIGHT_GRAIN.get() ? (night ? 0.085F : 0.028F) : 0.0F;
        if (signalBurstCooldown < 0) signalBurstCooldown = 900 + RNG.nextInt(1100);
        else if (--signalBurstCooldown <= 0) {
            // Interferencia breve, no un screamer: la cinta falla y vuelve a estabilizarse.
            if (night && mc.screen == null && RNG.nextFloat() < 0.78F) {
                triggerStatic(3 + RNG.nextInt(5));
            }
            signalBurstCooldown = night ? 1500 + RNG.nextInt(2400) : 2600 + RNG.nextInt(4200);
        }

        // ---- mirada hacia presencias
        float best = 0.0F;
        float hunt = 0.0F;
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        for (Entity en : mc.level.entitiesForRendering()) {
            if (!(en instanceof HorrorEntity he)) continue;
            if (he.getMode() == HorrorMode.PERIPHERAL) continue;
            double targetY = he.getMode() == HorrorMode.PEEK
                    ? he.spriteHover() + HorrorEntity.HEAD_CENTER
                    : he.spriteHover() + he.spriteHeight() * 0.6D;
            Vec3 to = he.position().add(0.0D, targetY, 0.0D).subtract(eye);
            double d = to.length();
            if (d > 64.0D || d < 0.5D) continue;
            if (he.getMode() == HorrorMode.HUNT) hunt = Math.max(hunt, (float) (1.0D - d / 45.0D)); // te persigue: el miedo sube aunque no mires
            double dot = look.dot(to.scale(1.0D / d));
            if (dot < 0.6D || !p.hasLineOfSight(he)) continue;
            float prox = (float) (1.0D - d / 64.0D);
            float strength = (float) ((dot - 0.6D) / 0.4D) * (0.35F + 0.65F * prox);
            best = Math.max(best, strength);
        }
        best = Math.max(best, hunt * 0.9F);
        intensity += (best - intensity) * 0.08F;
        stare = best > 0.15F ? Math.min(200, stare + 1) : Math.max(0, stare - 2);
        float g = Math.min(1.0F, intensity * (0.4F + 0.6F * stare / 200.0F));
        PsychoOverlay.glitch = g;

        // latido: cuanto mas miras, mas rapido y agudo
        int period = 34 - (int) (14 * g);
        if (g > 0.35F && ++beat % period == 0) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.HEARTBEAT.get(), 1.0F + 0.25F * g, 0.5F * g));
        }

        // ---- frase al aparecer una presencia en pantalla
        boolean inView = best > 0.12F;
        sinceText++;
        if (ClientConfig.SCREEN_TEXT.get()) {
            // Las frases son un suceso independiente y poco frecuente: muchas apariciones
            // deben ocurrir en silencio para no revelar el patron al jugador.
            if (inView && !wasInView && sinceText > 160 && RNG.nextFloat() < 0.27F) triggerText();
            else if (inView && stare > 150 && sinceText > 500 && RNG.nextFloat() < 0.004F) triggerText();
        }
        wasInView = inView;
        if (PsychoOverlay.textTicks > 0) PsychoOverlay.textTicks--;

        // ---- mirada congelada: la camara queda fija en la presencia
        lockActive = false;
        if (PsychoOverlay.lockTicks > 0) {
            Entity ent = mc.level.getEntity(lockEntityId);
            if (ent instanceof HorrorEntity he && mc.screen == null) {
                Vec3 target = he.position().add(0.0D, he.spriteHover() + he.spriteHeight() * 0.75D, 0.0D);
                Vec3 d = target.subtract(p.getEyePosition());
                double hd = Math.sqrt(d.x * d.x + d.z * d.z);
                float yaw = (float) (net.minecraft.util.Mth.atan2(d.z, d.x) * (180.0D / Math.PI)) - 90.0F;
                float pitch = (float) -(net.minecraft.util.Mth.atan2(d.y, hd) * (180.0D / Math.PI));
                lockYaw = net.minecraft.util.Mth.rotLerp(0.45F, p.getYRot(), yaw);
                lockPitch = net.minecraft.util.Mth.lerp(0.45F, p.getXRot(), pitch);
                p.setYRot(lockYaw);
                p.setXRot(lockPitch);
                p.yRotO = lockYaw;
                p.xRotO = lockPitch;
                lockActive = true;
                PsychoOverlay.lockTicks--;
                if (PsychoOverlay.lockTicks % 20 == 0) play(ModSounds.HEARTBEAT.get(), 0.9F, 0.9F);
            } else if (++lockWait > 40) {
                PsychoOverlay.lockTicks = 0; // la entidad no llego: se cancela
            }
        }

        // ---- titulo del dia con sangre
        if (PsychoOverlay.titleTicks > 0) {
            int age = PsychoOverlay.titleTotal - PsychoOverlay.titleTicks;
            PsychoOverlay.titleTicks--;
            if (age < 100 && PsychoOverlay.DRIPS.size() < 64 && RNG.nextFloat() < 0.6F) {
                PsychoOverlay.Drip d = new PsychoOverlay.Drip();
                boolean fromText = RNG.nextFloat() < 0.45F;
                d.x = fromText ? 0.2F + RNG.nextFloat() * 0.6F : RNG.nextFloat();
                d.y0 = fromText ? 0.34F + RNG.nextFloat() * 0.10F : 0.0F;
                d.y = d.y0;
                d.speed = 0.002F + RNG.nextFloat() * 0.006F;
                d.w = 1.5F + RNG.nextFloat() * 3.5F;
                d.delay = RNG.nextInt(7);
                PsychoOverlay.DRIPS.add(d);
            }
        }
        if (PsychoOverlay.bloodTicks > 0) {
            PsychoOverlay.bloodTicks--;
            for (int i = PsychoOverlay.DRIPS.size() - 1; i >= 0; i--) {
                PsychoOverlay.Drip d = PsychoOverlay.DRIPS.get(i);
                if (d.delay > 0) { d.delay--; continue; }
                d.speed = Math.min(d.speed * 1.015F, 0.02F);
                d.y += d.speed;
                if (d.y > 1.08F) PsychoOverlay.DRIPS.remove(i);
            }
        } else if (!PsychoOverlay.DRIPS.isEmpty()) {
            PsychoOverlay.DRIPS.clear();
            PsychoOverlay.SPLATS.clear();
        }

        // ---- temporizadores de efectos
        if (PsychoOverlay.scTicks > 0) PsychoOverlay.scTicks--;
        if (PsychoOverlay.blackTicks > 0) PsychoOverlay.blackTicks--;
        if (PsychoOverlay.redTicks > 0) PsychoOverlay.redTicks--;
        if (PsychoOverlay.staticTicks > 0) PsychoOverlay.staticTicks--;
        if (tinnitusIn >= 0 && --tinnitusIn < 0) play(ModSounds.TINNITUS.get(), 1.0F, 0.55F);

        // ---- signos extranos: pocos y efimeros
        for (int i = PsychoOverlay.SYMBOL_LIST.size() - 1; i >= 0; i--) {
            if (--PsychoOverlay.SYMBOL_LIST.get(i).ttl <= 0) PsychoOverlay.SYMBOL_LIST.remove(i);
        }
        boolean active = PsychoOverlay.textTicks > 0 || g > 0.25F || PsychoOverlay.flashTicks > 0 || PsychoOverlay.scTicks > 0;
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
            if (night) minutes *= 0.7D; // de noche, mas seguidos
            flashCountdown = (int) (minutes * 1200.0D);
        }
    }

    private static void play(SoundEvent ev, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, volume));
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
        play(ModSounds.CRACK.get(), 0.9F + RNG.nextFloat() * 0.2F, 1.0F);
    }

    /** Screamer al intentar dormir: imagen entera + mensaje + chillido, impacto y acufeno. image = -1 -> al azar. */
    public static void triggerScreamer(int image, String message) {
        int img = image;
        if (img < 0 || img >= PsychoOverlay.TEX_COUNT || !PsychoOverlay.available(img)) {
            int tries = 0;
            do {
                img = RNG.nextInt(PsychoOverlay.TEX_COUNT);
            } while ((RECENT_SC.contains(img) || !PsychoOverlay.available(img)) && ++tries < 40);
            if (!PsychoOverlay.available(img)) img = 0; // ultimo recurso
        }
        RECENT_SC.addLast(img);
        while (RECENT_SC.size() > 3) RECENT_SC.removeFirst();
        PsychoOverlay.scImage = img;
        PsychoOverlay.scText = (message == null || message.isEmpty()) ? "NO ESCAPAR\u00c1S DE ESTA REALIDAD" : message;
        PsychoOverlay.scTicks = PsychoOverlay.scTotal = 58;
        play(ModSounds.SCREAMER.get(), 0.95F + RNG.nextFloat() * 0.1F, 1.0F);
        play(ModSounds.IMPACT.get(), 1.0F, 0.9F);
        tinnitusIn = 45;
    }

    /** Al empezar cada noche (menos la primera): "DIA N" en rojo con gotas de sangre. */
    public static void triggerNightTitle(int day) {
        triggerTitle("D\u00cdA " + day);
    }

    /** Titulo rojo con salpicaduras y gotas de sangre. */
    public static void triggerTitle(String text) {
        PsychoOverlay.titleText = text;
        PsychoOverlay.titleTicks = PsychoOverlay.titleTotal = 150;
        PsychoOverlay.bloodTicks = 210;
        PsychoOverlay.DRIPS.clear();
        PsychoOverlay.SPLATS.clear();
        for (int i = 0; i < 7; i++) {
            PsychoOverlay.Splat sp = new PsychoOverlay.Splat();
            sp.x = RNG.nextFloat();
            sp.y = RNG.nextFloat() * 0.9F;
            sp.size = 0.8F + RNG.nextFloat() * 1.6F;
            for (int k = 0; k < sp.ox.length; k++) {
                sp.ox[k] = RNG.nextInt(23) - 11;
                sp.oy[k] = RNG.nextInt(23) - 11;
                sp.os[k] = 2 + RNG.nextInt(6);
            }
            PsychoOverlay.SPLATS.add(sp);
        }
        play(ModSounds.IMPACT.get(), 0.9F, 0.7F);
        play(ModSounds.HEARTBEAT.get(), 0.9F, 0.8F);
    }

    /** La mirada del jugador queda congelada en la entidad 'entityId' durante 'ticks' con un mensaje. */
    public static void triggerLock(int entityId, int ticks, String message) {
        lockEntityId = entityId;
        lockWait = 0;
        PsychoOverlay.lockTotal = PsychoOverlay.lockTicks = Math.max(20, ticks);
        PsychoOverlay.lockText = (message == null || message.isEmpty()) ? "NO ESCAPAR\u00c1S NUNCA M\u00c1S DE M\u00cd" : message;
        play(ModSounds.IMPACT.get(), 0.8F, 1.0F);
    }

    /** Destello rojo y brillo cuando El Otro aparece o golpea. */
    public static void triggerBloodFlash(int ticks) {
        PsychoOverlay.redTotal = PsychoOverlay.redTicks = Math.max(3, ticks);
    }

    public static void triggerBlackout(int ticks) {
        PsychoOverlay.blackTicks = PsychoOverlay.blackTotal = Math.max(4, ticks);
        play(ModSounds.TAPE_STOP.get(), 1.0F, 0.7F);
    }

    public static void triggerStatic(int ticks) {
        PsychoOverlay.staticTicks = PsychoOverlay.staticTotal = Math.max(4, ticks);
        play(ModSounds.STATIC.get(), 0.9F + RNG.nextFloat() * 0.2F, 0.6F);
    }

    private PsychoClientTick() {}
}
