package com.anexohorror.entity;

import com.anexohorror.HorrorConfig;
import com.anexohorror.director.HorrorDirector;
import com.anexohorror.director.HorrorUtil;
import com.anexohorror.net.ClientEffectPacket;
import com.anexohorror.net.ModNetwork;
import com.anexohorror.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Presencia inmovil. No tiene IA, no se mueve, no ataca ni persigue.
 * Solo observa, y desaparece cuando el jugador la mira y se aparta, se acerca o se cansa de ella.
 */
public class HorrorEntity extends Mob {
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(HorrorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(HorrorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SCALE =
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

    // animacion de asomarse (solo cabeza): sale lentamente desde un punto oculto hasta su posicion final
    public static final float HEAD_CENTER = 1.55F;
    private boolean peekAnim;
    private double peekBx, peekBy, peekBz;
    private float peekDx, peekDy, peekDz;
    private int revealTicks = 1;

    // persecucion: 0 = no, 1 = mirada congelada, 2 = cazando
    private int huntPhase;
    private int huntTimer;

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
    }

    public void setup(UUID owner, HorrorVariant variant, HorrorMode mode, float scale,
                      int maxLife, int stareLimit, int grace, int glimpseDelay, double vanishDist, int chainLeft) {
        this.ownerId = owner;
        this.entityData.set(DATA_VARIANT, variant.ordinal());
        this.entityData.set(DATA_MODE, mode.ordinal());
        this.entityData.set(DATA_SCALE, scale);
        this.maxLife = maxLife;
        this.stareLimit = stareLimit;
        this.grace = grace;
        this.glimpseDelay = glimpseDelay;
        this.vanishDist = vanishDist;
        this.chainLeft = chainLeft;
    }

    public HorrorVariant getVariant() { return HorrorVariant.byId(this.entityData.get(DATA_VARIANT)); }
    public HorrorMode getMode() { return HorrorMode.byId(this.entityData.get(DATA_MODE)); }
    public float getVisualScale() { return this.entityData.get(DATA_SCALE); }
    public UUID getOwnerId() { return ownerId; }
    public void setAllowDay(boolean allow) { this.allowDay = allow; }
    public int getChainLeft() { return chainLeft; }
    public boolean wasSeen() { return hasBeenSeen; }
    public int getSeenTicks() { return seenTicks; }

    public static float hoverOf(HorrorVariant v, HorrorMode m, float scale) {
        return m == HorrorMode.LOW ? 0.0F : v.hover * scale;
    }

    /** Modo asomarse en un plano 2D: solo se dibuja la cabeza, a la altura de los ojos. */
    public boolean isHeadOnly() { return getMode() == HorrorMode.PEEK && !getVariant().model; }

    public float spriteHeight() {
        return isHeadOnly() ? getVariant().headHeight(getVisualScale()) : getVariant().height * getVisualScale();
    }

    public float spriteWidth() {
        return isHeadOnly() ? getVariant().headWidth(getVisualScale()) : getVariant().height * getVisualScale() * getVariant().aspect;
    }

    public float spriteHover() {
        return isHeadOnly() ? HEAD_CENTER - spriteHeight() / 2.0F : hoverOf(getVariant(), getMode(), getVisualScale());
    }

    /** Posicion final (bx,by,bz) y vector hacia donde esta oculta (dx,dy,dz); sale en reveal ticks. */
    public void setPeekAnim(double bx, double by, double bz, float dx, float dy, float dz, int reveal) {
        this.peekAnim = true;
        this.peekBx = bx; this.peekBy = by; this.peekBz = bz;
        this.peekDx = dx; this.peekDy = dy; this.peekDz = dz;
        this.revealTicks = Math.max(1, reveal);
    }

    public int getRevealTicks() { return revealTicks; }

    /** Mirada congelada (lockTicks) y despues persecucion frenetica hasta alcanzar al jugador. */
    public void beginHunt(int lockTicks) {
        this.huntPhase = 1;
        this.huntTimer = lockTicks;
    }

    public boolean isHunting() { return huntPhase != 0; }

    @Override
    public void tick() {
        super.tick();
        this.noPhysics = true;
        if (this.level().isClientSide) {
            if (this.tickCount % 4 == 0) { // ceniza lenta alrededor: atmosfera, sin molestar
                this.level().addParticle(ParticleTypes.ASH,
                        getX() + (this.random.nextDouble() - 0.5D) * spriteWidth() * 0.8D,
                        getY() + spriteHover() + this.random.nextDouble() * spriteHeight(),
                        getZ() + (this.random.nextDouble() - 0.5D) * 0.6D, 0.0D, -0.015D, 0.0D);
            }
            return;
        }
        if (!(this.level() instanceof ServerLevel sl)) return;
        if (ownerId == null) { this.discard(); return; }

        ServerPlayer p = sl.getServer().getPlayerList().getPlayer(ownerId);
        if (p == null || p.level() != sl || !p.isAlive() || p.isSpectator()) {
            this.discard();
            return;
        }
        life++;
        if (huntPhase != 0) { tickHunt(sl, p); return; }
        if (!allowDay && life % 20 == 0 && !HorrorUtil.isNightTime(sl)) { // amanece: se esfuma
            vanish(VanishReason.EXPIRED);
            return;
        }

        // Siempre orientada hacia el jugador (el render ademas hace billboard).
        double dx = p.getX() - getX();
        double dz = p.getZ() - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.setYHeadRot(yaw);

        if (peekAnim) { // sale despacio desde detras del obstaculo (ease in-out)
            float t = Mth.clamp((life - 8) / (float) revealTicks, 0.0F, 1.0F);
            float e = t * t * (3.0F - 2.0F * t);
            float k = 1.0F - e;
            this.setPos(peekBx + peekDx * k, peekBy + peekDy * k, peekBz + peekDz * k);
        }

        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < vanishDist) { vanish(VanishReason.TOO_CLOSE); return; }
        if (life > maxLife) { vanish(VanishReason.EXPIRED); return; }

        Vec3 eye = p.getEyePosition();
        float h = spriteHeight();
        Vec3 center = new Vec3(getX(), getY() + spriteHover() + h * 0.6D, getZ());
        double cos = p.getLookAngle().dot(center.subtract(eye).normalize());

        if (life % 2 == 0) {
            visibleCache = HorrorUtil.visibleSamples(sl, eye, p, position(),
                    spriteHover(), h, spriteWidth()) > 0;
        }

        if (getMode() == HorrorMode.PERIPHERAL) {
            // Solo se ve de reojo: en cuanto el jugador gira y la mira, se esfuma.
            if (cos > 0.90D && visibleCache) {
                if (++glimpseTicks >= glimpseDelay) vanish(VanishReason.GLIMPSED);
            }
            return;
        }

        boolean inView = cos > 0.74D && visibleCache;
        if (inView) {
            hasBeenSeen = true;
            unseenTicks = 0;
            if (++seenTicks > stareLimit) vanish(VanishReason.STARE_DONE);
        } else if (hasBeenSeen) {
            if (++unseenTicks > grace) vanish(VanishReason.LOOKED_AWAY);
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
                huntTimer = 1800; // si en 90 s no llega, desaparece
                this.entityData.set(DATA_MODE, HorrorMode.HUNT.ordinal());
                sl.playSound(null, getX(), getY(), getZ(), ModSounds.CREATURE_CALL.get(), SoundSource.HOSTILE, 3.0F, 1.0F);
            }
            return;
        }
        if (--huntTimer <= 0) { vanish(VanishReason.EXPIRED); return; }

        double hd = Math.sqrt(dx * dx + dz * dz);
        double dy = p.getY() - getY();
        if (hd < 1.8D && Math.abs(dy) < 2.6D) { // alcanzado: nada de dano, solo el screamer
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
        if (huntPhase == 0 && !this.level().isClientSide && !this.isRemoved()) vanish(VanishReason.HURT);
        return false; // durante la caza nada la detiene
    }

    @Override public boolean isPushable() { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public void push(Entity entity) { }
    @Override public boolean shouldShowName() { return false; }

    @Override
    public AABB getBoundingBoxForCulling() {
        return new AABB(getX() - 1.6D, getY() - 0.5D, getZ() - 1.6D, getX() + 1.6D, getY() + 3.6D, getZ() + 1.6D);
    }
}
