package com.anexohorror.entity;

import com.anexohorror.HorrorConfig;
import com.anexohorror.director.CoverFinder;
import com.anexohorror.director.CoverSpot;
import com.anexohorror.director.HorrorDirector;
import com.anexohorror.director.HorrorUtil;
import com.anexohorror.net.ClientEffectPacket;
import com.anexohorror.net.ModNetwork;
import com.anexohorror.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Presencia de acecho. En los modos normales no tiene IA, no se mueve ni ataca:
 * observa y desaparece cuando el jugador la mira y se aparta, se acerca o se cansa de ella.
 * HUNT es la excepcion especial que se desplaza y persigue al jugador.
 */
public class HorrorEntity extends Mob {
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(HorrorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(HorrorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(HorrorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_HAND =
            SynchedEntityData.defineId(HorrorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_HAND_X =
            SynchedEntityData.defineId(HorrorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HAND_Y =
            SynchedEntityData.defineId(HorrorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HAND_Z =
            SynchedEntityData.defineId(HorrorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HAND_PROGRESS =
            SynchedEntityData.defineId(HorrorEntity.class, EntityDataSerializers.FLOAT);
    /** Amplitud (0.7..1.2) de la inclinacion y el ladeo de la cabeza en un asomo. */
    private static final EntityDataAccessor<Float> DATA_PEEK_LEAN =
            SynchedEntityData.defineId(HorrorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PEEK_PROGRESS =
            SynchedEntityData.defineId(HorrorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_PEEK_GONE =
            SynchedEntityData.defineId(HorrorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_LEAN_X =
            SynchedEntityData.defineId(HorrorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LEAN_Z =
            SynchedEntityData.defineId(HorrorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HIDE =
            SynchedEntityData.defineId(HorrorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_GRIP =
            SynchedEntityData.defineId(HorrorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_GRIP_X =
            SynchedEntityData.defineId(HorrorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_GRIP_Y =
            SynchedEntityData.defineId(HorrorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_GRIP_Z =
            SynchedEntityData.defineId(HorrorEntity.class, EntityDataSerializers.FLOAT);

    // Estado solo de servidor (la entidad no se guarda en disco: noSave()).
    private UUID ownerId;
    private int life;
    private int maxLife = 400;
    private int stareLimit = 200;
    private int grace = 10;
    private int glimpseDelay = 3;
    private double vanishDist = 6.0D;
    private int chainLeft;
    private int seenTicks;
    private int unseenTicks;
    private int glimpseTicks;
    private boolean hasBeenSeen;
    private boolean visibleCache;
    private boolean allowDay;

    // Asomo real (PEEK): el servidor gobierna las fases y sincroniza peekProgress (0 oculto .. 1 asomado);
    // el cliente deforma el sprite con ese valor (ver HorrorRenderer / PeekPose).
    public static final float HEAD_CENTER = 1.55F; // historico: los asomos usan headWorldPoint()
    private static final int PEEK_WAIT = 0, PEEK_OUT = 1, PEEK_HOLD = 2, PEEK_BACK = 3, PEEK_AWAY = 4, PEEK_SETTLE = 5;
    private boolean peekReady, peekGaze, handQueued, retreatFast;
    private int peekPhase, peekTimer, peekMinWait = 12, peekMaxWait = 160, outSteps = 5, stepTimer;
    private int holdTicks, peeksLeft, relocDelay, relocFails, handHold, handQueueDuration, revealTicks = 60;
    private float peekProgress, outTarget;
    private VanishReason retreatReason = VanishReason.EXPIRED;
    private float peekRender, peekRenderPrev; // suavizado solo de cliente

    // persecucion: 0 = no, 1 = mirada congelada, 2 = cazando
    private int huntPhase;
    private int huntTimer;
    private int huntLostSightTicks;
    private boolean aggressiveHunt;
    // El STALKER se detiene cuando el jugador lo enfoca y avanza a pequenos saltos
    // solo cuando deja de mirarlo, siguiendo la mecanica de acecho de Bedrock.
    private int stalkerStepCooldown = 10;

    // Mano: ataque escenico sincronizado con el cliente, limitado a siete bloques.
    private int handTicks;
    private int handDuration = 90;
    private boolean handAttack;
    private boolean handResolved;

    public HorrorEntity(EntityType<? extends HorrorEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setNoAi(true);
        this.xpReward = 0;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT, 0);
        this.entityData.define(DATA_MODE, 0);
        this.entityData.define(DATA_SCALE, 1.0F);
        this.entityData.define(DATA_HAND, false);
        this.entityData.define(DATA_HAND_X, 0.0F);
        this.entityData.define(DATA_HAND_Y, 0.0F);
        this.entityData.define(DATA_HAND_Z, 0.0F);
        this.entityData.define(DATA_HAND_PROGRESS, 0.0F);
        this.entityData.define(DATA_PEEK_LEAN, 1.0F);
        this.entityData.define(DATA_PEEK_PROGRESS, 0.0F);
        this.entityData.define(DATA_PEEK_GONE, false);
        this.entityData.define(DATA_LEAN_X, 1.0F);
        this.entityData.define(DATA_LEAN_Z, 0.0F);
        this.entityData.define(DATA_HIDE, 0.0F);
        this.entityData.define(DATA_GRIP, false);
        this.entityData.define(DATA_GRIP_X, 0.0F);
        this.entityData.define(DATA_GRIP_Y, 0.0F);
        this.entityData.define(DATA_GRIP_Z, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 160.0D) // 80 corazones: supera al golem de hierro (100 HP)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    public void setup(UUID owner, HorrorVariant variant, HorrorMode mode, float scale,
                      int maxLife, int stareLimit, int grace, int glimpseDelay, double vanishDist, int chainLeft) {
        this.ownerId = owner;
        this.entityData.set(DATA_VARIANT, variant.ordinal());
        this.entityData.set(DATA_MODE, mode.ordinal());
        this.entityData.set(DATA_SCALE, scale);
        this.maxLife = variant == HorrorVariant.STALKER ? Math.max(maxLife, 1400) : maxLife;
        this.stareLimit = stareLimit;
        this.grace = grace;
        this.glimpseDelay = glimpseDelay;
        this.vanishDist = vanishDist;
        this.chainLeft = chainLeft;
        if (mode == HorrorMode.PEEK) this.entityData.set(DATA_PEEK_GONE, true); // no se dibuja hasta setPeekCover()
        this.setHealth(this.getMaxHealth());
    }

    public HorrorVariant getVariant() { return HorrorVariant.byId(this.entityData.get(DATA_VARIANT)); }
    public HorrorMode getMode() { return HorrorMode.byId(this.entityData.get(DATA_MODE)); }
    public float getVisualScale() { return this.entityData.get(DATA_SCALE); }
    public UUID getOwnerId() { return ownerId; }
    public void setAllowDay(boolean allow) { this.allowDay = allow; }
    public int getChainLeft() { return chainLeft; }
    public boolean wasSeen() { return hasBeenSeen; }
    public int getSeenTicks() { return seenTicks; }
    public boolean hasHandAttack() { return this.entityData.get(DATA_HAND); }
    public float getHandX() { return this.entityData.get(DATA_HAND_X); }
    public float getHandY() { return this.entityData.get(DATA_HAND_Y); }
    public float getHandZ() { return this.entityData.get(DATA_HAND_Z); }
    public float getHandProgress() { return this.entityData.get(DATA_HAND_PROGRESS); }
    public float getPeekAmp() { return this.entityData.get(DATA_PEEK_LEAN); }

    /** Inicia la mano. El extremo nunca se sincroniza a mas de 7 bloques del origen. */
    public void beginHandAttack(ServerPlayer target, int duration) {
        if (this.level().isClientSide || target == null || !target.isAlive()) return;
        Vec3 delta = target.getEyePosition().subtract(this.position());
        double distance = delta.length();
        if (distance < 0.1D) return;
        double reach = Math.min(7.0D, distance);
        Vec3 end = delta.normalize().scale(reach);
        this.entityData.set(DATA_HAND_X, (float) end.x);
        this.entityData.set(DATA_HAND_Y, (float) end.y);
        this.entityData.set(DATA_HAND_Z, (float) end.z);
        this.entityData.set(DATA_HAND_PROGRESS, 0.0F);
        this.entityData.set(DATA_HAND, true);
        this.handTicks = 0;
        this.handDuration = Math.max(40, duration);
        this.handAttack = true;
        this.handResolved = false;
    }

    public static float hoverOf(HorrorVariant v, HorrorMode m, float scale) {
        return m == HorrorMode.LOW ? 0.0F : v.hover * scale;
    }

    /** PEEK conserva la silueta completa: el bloque oculta torso y parte del cuerpo. */
    public float spriteHeight() { return getVariant().height * getVisualScale(); }

    public float spriteWidth() { return getVariant().height * getVisualScale() * getVariant().aspect; }

    public float spriteHover() { return hoverOf(getVariant(), getMode(), getVisualScale()); }

    // ------------------------------------------------------------------ asomo real (PEEK)

    public float getPeekProgress() { return this.entityData.get(DATA_PEEK_PROGRESS); }
    public boolean isPeekGone() { return this.entityData.get(DATA_PEEK_GONE); }
    public float getLeanX() { return this.entityData.get(DATA_LEAN_X); }
    public float getLeanZ() { return this.entityData.get(DATA_LEAN_Z); }
    public float getHideDist() { return this.entityData.get(DATA_HIDE); }
    public boolean hasGrip() { return this.entityData.get(DATA_GRIP); }
    /** Punto de agarre relativo al origen de la entidad (ejes del mundo). */
    public Vec3 getGripRel() {
        return new Vec3(this.entityData.get(DATA_GRIP_X), this.entityData.get(DATA_GRIP_Y), this.entityData.get(DATA_GRIP_Z));
    }
    public int getRevealTicks() { return revealTicks; }
    /** Progreso suavizado para dibujar (solo cliente). */
    public float getRenderPeekProgress(float partial) { return Mth.lerp(partial, peekRenderPrev, peekRender); }

    /** Centro de la cabeza en el mundo para un progreso dado (servidor y cliente). */
    public Vec3 headWorldPoint(float e) {
        float amp = getPeekAmp();
        float[] off = PeekPose.headOffset(getVariant(), getVisualScale(),
                PeekPose.leanRad(e, amp), PeekPose.tiltRad(e, amp, 1.0F));
        double lateral = off[0] - getHideDist() * (1.0F - e);
        return new Vec3(getX() + getLeanX() * lateral, getY() + spriteHover() + off[1], getZ() + getLeanZ() * lateral);
    }

    private void applyCover(Vec3 lean, double hide, @Nullable Vec3 grip) {
        double len = Math.sqrt(lean.x * lean.x + lean.z * lean.z);
        float lx = len > 1.0E-4D ? (float) (lean.x / len) : 1.0F;
        float lz = len > 1.0E-4D ? (float) (lean.z / len) : 0.0F;
        this.entityData.set(DATA_LEAN_X, lx);
        this.entityData.set(DATA_LEAN_Z, lz);
        this.entityData.set(DATA_HIDE, (float) hide);
        if (grip != null) {
            Vec3 rel = grip.subtract(this.position());
            this.entityData.set(DATA_GRIP_X, (float) rel.x);
            this.entityData.set(DATA_GRIP_Y, (float) rel.y);
            this.entityData.set(DATA_GRIP_Z, (float) rel.z);
            this.entityData.set(DATA_GRIP, true);
        } else {
            this.entityData.set(DATA_GRIP, false);
        }
    }

    /**
     * Prepara el asomo tras una cobertura. La entidad debe estar ya colocada en la pose asomada.
     * Empieza escondida; sale a pasos cuando el jugador mira hacia alli (si gaze), se queda observando,
     * se esconde si la miran fijamente o si se aparta, y se recoloca mas cerca (cycles veces).
     */
    public void setPeekCover(Vec3 lean, double hide, @Nullable Vec3 grip, float amp, boolean gaze, int cycles) {
        applyCover(lean, hide, grip);
        this.entityData.set(DATA_PEEK_LEAN, amp);
        this.peekGaze = gaze;
        this.peeksLeft = Math.max(0, cycles);
        this.peekReady = true;
        this.peekPhase = PEEK_WAIT;
        this.peekTimer = 0;
        this.peekProgress = 0.0F;
        this.peekMinWait = 10 + this.random.nextInt(20);
        this.peekMaxWait = 150 + this.random.nextInt(60);
        this.revealTicks = 40 + this.random.nextInt(35);
        this.entityData.set(DATA_PEEK_PROGRESS, 0.0F);
        this.entityData.set(DATA_PEEK_GONE, false);
    }

    /** Tras revelarse del todo y esperar holdTicks, extiende el brazo hacia el jugador. */
    public void queueHandAttack(int holdTicks, int duration) {
        this.handQueued = true;
        this.handHold = holdTicks;
        this.handQueueDuration = duration;
        this.stareLimit = Math.max(this.stareLimit, 400); // no se esconde antes de alcanzar
        this.grace = Math.max(this.grace, 200);
    }

    /** Mirada congelada (lockTicks) y despues persecucion frenetica hasta alcanzar al jugador. */
    public void beginHunt(ServerPlayer target, int lockTicks) {
        this.huntPhase = 1;
        this.huntTimer = lockTicks;
        this.huntLostSightTicks = 0;
        int nights = target.getPersistentData().getCompound("PlayerPersisted").getInt("anexohorror_nights");
        // La persecucion evoluciona con las noches; al principio aun puede perder el rastro.
        float aggressionChance = Mth.clamp((nights - 2) * 0.075F, 0.0F, 0.68F);
        this.aggressiveHunt = this.random.nextFloat() < aggressionChance;
    }

    public boolean isHunting() { return huntPhase != 0; }

    @Override
    public void tick() {
        super.tick();
        this.noPhysics = true;
        if (this.level().isClientSide) {
            // suaviza el progreso del asomo: cada paso del servidor se ve como un deslizamiento corto
            peekRenderPrev = peekRender;
            float target = this.entityData.get(DATA_PEEK_PROGRESS);
            peekRender += (target - peekRender) * 0.55F;
            if (Math.abs(target - peekRender) < 0.002F) peekRender = target;
            return; // sin particulas alrededor: la unica luz propia de la presencia son sus ojos
        }
        if (!(this.level() instanceof ServerLevel sl)) return;
        if (ownerId == null) { this.discard(); return; }

        ServerPlayer p = sl.getServer().getPlayerList().getPlayer(ownerId);
        if (p == null || p.level() != sl || !p.isAlive() || p.isSpectator()) {
            this.discard();
            return;
        }
        life++;
        if (handAttack) {
            tickHand(p);
            if (handAttack) return;
        }
        if (huntPhase != 0) { tickHunt(sl, p); return; }
        if (!allowDay && life % 20 == 0 && !HorrorUtil.isNightTime(sl)) { // amanece: se esfuma
            if (getMode() == HorrorMode.PEEK && peekReady) {
                peeksLeft = 0;
                beginRetreat(p, VanishReason.EXPIRED); // los asomos se esconden en vez de desaparecer a la vista
            } else {
                vanish(VanishReason.EXPIRED);
                return;
            }
        }

        // Siempre orientada hacia el jugador (el render ademas hace billboard).
        double dx = p.getX() - getX();
        double dz = p.getZ() - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.setYHeadRot(yaw);

        if (getMode() == HorrorMode.PEEK) { tickPeek(sl, p); return; }

        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < vanishDist) { vanish(VanishReason.TOO_CLOSE); return; }
        if (life > maxLife) { vanish(VanishReason.EXPIRED); return; }

        Vec3 eye = p.getEyePosition();
        float h = spriteHeight();
        float sampleHover = spriteHover();
        float sampleHeight = h;
        float sampleWidth = spriteWidth();
        double centerOffset = sampleHover + h * 0.6D;
        Vec3 center = new Vec3(getX(), getY() + centerOffset, getZ());
        double cos = p.getLookAngle().dot(center.subtract(eye).normalize());

        if (life % 2 == 0) {
            visibleCache = HorrorUtil.visibleSamples(sl, eye, p, position(),
                    sampleHover, sampleHeight, sampleWidth) > 0;
        }

        boolean inView = cos > 0.74D && visibleCache;

        // Mecánica propia del STALKER: no camina mientras está bajo la mirada directa.
        // Cuando el jugador aparta la cámara, gana terreno lentamente y sin sonidos de pasos.
        if (getVariant() == HorrorVariant.STALKER) {
            if (inView) {
                hasBeenSeen = true;
                unseenTicks = 0;
                stalkerStepCooldown = 9 + this.random.nextInt(12);
            } else {
                unseenTicks++;
                if (--stalkerStepCooldown <= 0) {
                    stalkerStepCooldown = 9 + this.random.nextInt(12);
                    creepTowardPlayer(sl, p, dist);
                }
            }
            return;
        }

        if (getMode() == HorrorMode.PERIPHERAL) {
            // Las demas presencias perifericas se esfuman si el jugador las enfoca.
            if (cos > 0.90D && visibleCache) {
                if (++glimpseTicks >= glimpseDelay) vanish(VanishReason.GLIMPSED);
            }
            return;
        }

        if (inView) {
            hasBeenSeen = true;
            unseenTicks = 0;
            if (++seenTicks > stareLimit) vanish(VanishReason.STARE_DONE);
        } else if (hasBeenSeen) {
            if (++unseenTicks > grace) vanish(VanishReason.LOOKED_AWAY);
        }
    }

    // ------------------------------------------------------------------ fases del asomo

    private void tickPeek(ServerLevel sl, ServerPlayer p) {
        if (!peekReady) { // sin cobertura asignada no hay asomo posible
            if (life > 60) vanish(VanishReason.EXPIRED);
            return;
        }
        Vec3 eye = p.getEyePosition();
        double dx = p.getX() - getX();
        double dz = p.getZ() - getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        // ¿Mira el jugador la cabeza asomada?
        boolean inView = false;
        if (peekPhase != PEEK_AWAY && peekPhase != PEEK_SETTLE) {
            Vec3 head = headWorldPoint(peekProgress);
            Vec3 to = head.subtract(eye);
            double len = to.length();
            double cos = len > 1.0E-4D ? p.getLookAngle().dot(to.scale(1.0D / len)) : 1.0D;
            if (life % 2 == 0) {
                visibleCache = HorrorUtil.headSamples(sl, eye, p, head,
                        Math.max(0.12D, getVariant().headWidth(getVisualScale()) * 0.3D)) >= 2;
            }
            inView = cos > 0.74D && visibleCache && peekProgress > 0.15F;
        }
        if (inView) {
            hasBeenSeen = true;
            unseenTicks = 0;
            seenTicks++;
        } else if (hasBeenSeen) {
            unseenTicks++;
        }

        if (peekPhase == PEEK_WAIT) {
            peekTimer++;
            if (dist < vanishDist) { vanish(VanishReason.TOO_CLOSE); return; }
            if (life > maxLife) { vanish(VanishReason.EXPIRED); return; }
            boolean go = peekTimer >= peekMinWait;
            if (go && peekGaze && peekTimer < peekMaxWait) {
                // espera a que el jugador mire hacia alli: asi el movimiento se nota
                Vec3 to = headWorldPoint(1.0F).subtract(eye);
                double len = to.length();
                go = len < 1.0E-4D || p.getLookAngle().dot(to.scale(1.0D / len)) > 0.28D;
            }
            if (go) {
                peekPhase = PEEK_OUT;
                outSteps = 4 + this.random.nextInt(3);
                outTarget = 0.0F;
                stepTimer = 0;
                scrape(p, 0.45F);
            }
            return;
        }

        if (peekPhase == PEEK_OUT || peekPhase == PEEK_HOLD) {
            if (dist < vanishDist) { beginRetreat(p, VanishReason.TOO_CLOSE); return; }
            if (life > maxLife) { beginRetreat(p, VanishReason.EXPIRED); return; }
            if (seenTicks > stareLimit) { beginRetreat(p, VanishReason.STARE_DONE); return; }
            if (hasBeenSeen && unseenTicks > grace + 6 && peekProgress > 0.3F) {
                beginRetreat(p, VanishReason.LOOKED_AWAY);
                return;
            }
        }

        if (peekPhase == PEEK_OUT) {
            // sale a pasos cortos con pausas; si lo miran, se queda congelado
            if (!inView && --stepTimer <= 0) {
                outTarget = Math.min(1.0F, outTarget + (1.0F / outSteps) * (0.7F + this.random.nextFloat() * 0.7F));
                stepTimer = 3 + this.random.nextInt(7);
            }
            peekProgress += (outTarget - peekProgress) * 0.4F;
            if (outTarget >= 1.0F && peekProgress > 0.985F) {
                peekProgress = 1.0F;
                peekPhase = PEEK_HOLD;
                holdTicks = 0;
            }
            this.entityData.set(DATA_PEEK_PROGRESS, peekProgress);
            return;
        }

        if (peekPhase == PEEK_HOLD) {
            holdTicks++;
            if (handQueued && holdTicks >= handHold) {
                handQueued = false;
                if (p.position().distanceTo(this.position()) <= 7.0D) {
                    beginHandAttack(p, handQueueDuration);
                    HorrorUtil.sound(p, ModSounds.DISTANT_SOUND.get(), SoundSource.HOSTILE,
                            this.position(), 0.95F, 0.82F + this.random.nextFloat() * 0.18F);
                }
            }
            return;
        }

        if (peekPhase == PEEK_BACK) {
            peekProgress = Math.max(0.0F, peekProgress - (retreatFast ? 0.34F : 0.17F));
            this.entityData.set(DATA_PEEK_PROGRESS, peekProgress);
            if (peekProgress <= 0.0F) finishRetreat();
            return;
        }

        if (peekPhase == PEEK_AWAY) { // escondida: espera y se recoloca mas cerca del jugador
            if (peeksLeft <= 0 || life > maxLife) { vanish(retreatReason); return; }
            if (++peekTimer < relocDelay) return;
            double minD = Math.max(vanishDist + 2.0D, 4.5D);
            double hi = dist - 1.0D;
            if (hi < minD + 0.5D) { vanish(retreatReason); return; }
            CoverSpot spot = CoverFinder.find(p, sl, getVariant(), getVisualScale(), minD, hi, 15, 90);
            if (spot == null) {
                if (++relocFails > 5) vanish(retreatReason);
                else peekTimer = relocDelay - 20;
                return;
            }
            this.moveTo(spot.shown.x, spot.shown.y, spot.shown.z, this.getYRot(), 0.0F);
            applyCover(spot.lean, spot.hide, spot.grip);
            peekProgress = 0.0F;
            this.entityData.set(DATA_PEEK_PROGRESS, 0.0F);
            peeksLeft--;
            seenTicks = 0;
            unseenTicks = 0;
            hasBeenSeen = false;
            visibleCache = false;
            peekPhase = PEEK_SETTLE;
            peekTimer = 0;
            return;
        }

        if (peekPhase == PEEK_SETTLE) { // invisible unos ticks para que el cliente asiente la posicion
            if (++peekTimer >= 5) {
                this.entityData.set(DATA_PEEK_GONE, false);
                peekPhase = PEEK_WAIT;
                peekTimer = 0;
                peekMinWait = 8 + this.random.nextInt(16);
                peekMaxWait = 150;
            }
        }
    }

    private void beginRetreat(ServerPlayer p, VanishReason reason) {
        if (peekPhase == PEEK_BACK || peekPhase == PEEK_AWAY || peekPhase == PEEK_SETTLE) return;
        retreatReason = reason;
        retreatFast = reason == VanishReason.TOO_CLOSE;
        peekPhase = PEEK_BACK;
        if (!retreatFast) scrape(p, 0.35F);
    }

    private void finishRetreat() {
        peekProgress = 0.0F;
        if (retreatReason == VanishReason.TOO_CLOSE || retreatReason == VanishReason.EXPIRED || peeksLeft <= 0) {
            vanish(retreatReason);
            return;
        }
        peekPhase = PEEK_AWAY;
        peekTimer = 0;
        relocDelay = 25 + this.random.nextInt(60);
        relocFails = 0;
        this.entityData.set(DATA_PEEK_GONE, true);
    }

    /** Arañazo de garras sobre el borde: aviso sonoro de que algo se agarra o se suelta. */
    private void scrape(ServerPlayer p, float volume) {
        HorrorUtil.sound(p, ModSounds.SCRAPE.get(), SoundSource.HOSTILE, this.position(),
                volume, 0.8F + this.random.nextFloat() * 0.3F);
    }

    /** Avance discreto solo cuando el jugador no lo mira; nunca atraviesa bloques. */
    private void creepTowardPlayer(ServerLevel sl, ServerPlayer p, double distance) {
        if (distance <= vanishDist + 0.75D) return;
        double dx = p.getX() - getX();
        double dz = p.getZ() - getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 1.0E-4D) return;
        double step = Math.min(0.13D + this.random.nextDouble() * 0.08D,
                Math.max(0.0D, horizontal - vanishDist - 0.35D));
        if (step <= 0.005D) return;
        double mx = dx / horizontal * step;
        double mz = dz / horizontal * step;
        AABB next = this.getBoundingBox().move(mx, 0.0D, mz);
        if (sl.noCollision(this, next)) {
            this.setPos(getX() + mx, getY(), getZ() + mz);
        }
    }

    private void tickHand(ServerPlayer p) {
        handTicks++;
        float progress = Mth.clamp(handTicks / (float) handDuration, 0.0F, 1.0F);
        this.entityData.set(DATA_HAND_PROGRESS, progress);
        if (progress >= 1.0F && !handResolved) {
            handResolved = true;
            double actualDistance = p.position().distanceTo(this.position());
            Vec3 end = this.position().add(getHandX(), getHandY(), getHandZ());
            // Solo alcanza si el jugador sigue dentro del radio y no se aparto del extremo.
            if (actualDistance <= 7.15D && p.getEyePosition().distanceTo(end) <= 1.65D) {
                ModNetwork.send(p, ClientEffectPacket.SCREAMER, -1, 0, "NO DEBISTE ABRIR");
                p.hurt(p.damageSources().magic(), 1000.0F);
                if (p.isAlive()) p.kill();
            }
        }
        if (handTicks > handDuration + 12) {
            handAttack = false;
            this.entityData.set(DATA_HAND, false);
            this.entityData.set(DATA_HAND_PROGRESS, 0.0F);
        }
    }

    private void tickHunt(ServerLevel sl, ServerPlayer p) {
        double dx = p.getX() - getX();
        double dz = p.getZ() - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.setYHeadRot(yaw);

        if (huntPhase == 1) { // mirada congelada: se queda quieta, mirandote
            if (--huntTimer <= 0) {
                huntPhase = 2;
                huntTimer = aggressiveHunt ? 3000 : 1800; // la variante agresiva persiste 150 s
                this.entityData.set(DATA_MODE, HorrorMode.HUNT.ordinal());
                HorrorUtil.sound(p, ModSounds.HUNT_SCREAM.get(), SoundSource.HOSTILE,
                        p.position(), 2.6F, 0.92F);
            }
            return;
        }
        if (--huntTimer <= 0) { vanish(VanishReason.EXPIRED); return; }

        double hd = Math.sqrt(dx * dx + dz * dz);
        double dy = p.getY() - getY();
        // Caza normal: si el jugador se aleja y mantiene la linea de vision rota, el rastro se pierde.
        // La variante agresiva (mas comun tras varias noches) no abandona la persecucion.
        if (!aggressiveHunt && hd > 18.0D && !p.hasLineOfSight(this)) {
            if (++huntLostSightTicks >= 120) { vanish(VanishReason.EXPIRED); return; }
        } else {
            huntLostSightTicks = 0;
        }
        if (hd < 1.8D && Math.abs(dy) < 2.6D) { // alcanzado: screamer, sin dano adicional
            ModNetwork.send(p, ClientEffectPacket.SCREAMER, 3, 0, "TE ALCANZ\u00d3");
            vanish(VanishReason.TOO_CLOSE);
            return;
        }
        double speed = HorrorConfig.HUNT_SPEED.get();
        double step = Math.min(speed, hd);
        double nx = hd > 1.0E-3D ? getX() + dx / hd * step : getX();
        double nz = hd > 1.0E-3D ? getZ() + dz / hd * step : getZ();
        double ny = getY() + Mth.clamp(dy, -0.32D, 0.32D); // trepa y baja siguiendo al jugador
        breakBlocks(sl, nx, ny, nz);
        this.setPos(nx, ny, nz);
        if (life % 5 == 0) {
            sl.playSound(null, nx, ny, nz, SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 1.6F, 0.8F);
        }
    }

    /** Rompe lo que haya en su camino (respeta mobGriefing y la config). Sin soltar objetos para no inundar el mundo. */
    private void breakBlocks(ServerLevel sl, double x, double y, double z) {
        if (!HorrorConfig.HUNT_BREAKS_BLOCKS.get() || !sl.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;
        BlockPos base = BlockPos.containing(x, y, z);
        int broken = 0;
        for (int i = 0; i <= 2 && broken < 3; i++) {
            BlockPos bp = base.above(i);
            BlockState bs = sl.getBlockState(bp);
            if (bs.isAir() || bs.getDestroySpeed(sl, bp) < 0.0F || !bs.getFluidState().isEmpty()) continue;
            sl.destroyBlock(bp, false, this);
            broken++;
        }
    }

    public void vanish(VanishReason reason) {
        if (this.isRemoved()) return;
        if (ownerId != null && !this.level().isClientSide) HorrorDirector.onVanish(ownerId, this, reason);
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || this.isRemoved() || amount <= 0.0F) return false;
        // Las presencias ahora tienen vida real: reciben dano y solo desaparecen al morir.
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && !this.isRemoved() && ownerId != null) {
            HorrorDirector.onVanish(ownerId, this, VanishReason.HURT);
        }
        super.die(source);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CRACK.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.IMPACT.get();
    }

    @Override public boolean isPushable() { return false; }
    @Override public boolean isPickable() { return !isPeekGone(); }
    @Override public boolean canBeCollidedWith() { return true; }
    @Override public void push(Entity entity) { }
    @Override public boolean shouldShowName() { return false; }

    @Override
    public AABB getBoundingBoxForCulling() {
        double r = getMode() == HorrorMode.PEEK ? 2.6D : 1.6D;
        AABB box = new AABB(getX() - r, getY() - 0.5D, getZ() - r, getX() + r, getY() + 3.6D, getZ() + r);
        return hasHandAttack() ? box.inflate(8.0D) : box; // el brazo estirado llega a siete bloques
    }
}
