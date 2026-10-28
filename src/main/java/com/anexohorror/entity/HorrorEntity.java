package com.anexohorror.entity;

import com.anexohorror.director.HorrorDirector;
import com.anexohorror.director.HorrorUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
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

    public float spriteHeight() { return getVariant().height * getVisualScale(); }
    public float spriteWidth() { return spriteHeight() * getVariant().aspect; }
    public float spriteHover() { return hoverOf(getVariant(), getMode(), getVisualScale()); }

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

    public void vanish(VanishReason reason) {
        if (this.isRemoved()) return;
        if (ownerId != null && !this.level().isClientSide) HorrorDirector.onVanish(ownerId, this, reason);
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && !this.isRemoved()) vanish(VanishReason.HURT);
        return false;
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
