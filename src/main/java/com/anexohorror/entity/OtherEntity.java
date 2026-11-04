package com.anexohorror.entity;

import com.anexohorror.HorrorConfig;
import com.anexohorror.director.HorrorUtil;
import com.anexohorror.net.ClientEffectPacket;
import com.anexohorror.net.ModNetwork;
import com.anexohorror.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.UUID;

/**
 * "El Otro": jefe final. Muy poco comun. Tienes 120 s (configurable) para matarlo antes de que la grieta lo reclame.
 * Ataca de verdad, aguanta mucho y se detiene a hablar por el chat cuando algo le duele.
 * Al morir suelta una estrella del Nether llamada "El Indice".
 */
public class OtherEntity extends Monster {
    private static final float[] THRESH = {0.90F, 0.75F, 0.60F, 0.45F, 0.30F, 0.15F};
    private static final String[][] PHASE_LINES = {
            {"Duele. Qué extraño. Hacía mucho que nada dolía."},
            {"Yo también escribí registros, ¿sabes? Con buena letra.", "Nadie los lee hasta el final."},
            {"¿De verdad crees que el Índice te pertenece?"},
            {"La grieta me lo pidió. Yo no quería quedarme.", "Nadie quiere."},
            {"Si me matas, alguien tiene que ocupar este rincón."},
            {"Hazlo. Pero ella seguirá abriéndose.", "Siempre se abre."}};

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.literal("EL OTRO"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    private UUID ownerId;
    private int fightTicks;
    private int phase;
    private boolean warned;
    private boolean talking;
    private int talkCooldown;
    private boolean deathHandled;
    private final ArrayDeque<String> queue = new ArrayDeque<>();

    // combate por teletransporte: desaparece en un destello de sangre y reaparece junto al jugador
    private static final EntityDataAccessor<Boolean> DATA_WINDUP =
            SynchedEntityData.defineId(OtherEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BLINK =
            SynchedEntityData.defineId(OtherEntity.class, EntityDataSerializers.INT);
    private int actionTicks = 40;
    private boolean windup;

    public OtherEntity(EntityType<? extends OtherEntity> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_WINDUP, false);
        this.entityData.define(DATA_BLINK, -1000);
    }

    public boolean isWindup() { return this.entityData.get(DATA_WINDUP); }
    public int getBlinkTick() { return this.entityData.get(DATA_BLINK); }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 350.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    /** Sentado en el suelo: no camina ni persigue. Ataca desapareciendo y reapareciendo junto al jugador. */
    @Override
    protected void registerGoals() {
    }

    /** Empieza el combate: cuenta atras, vida/dano de la config y primeras palabras. */
    public void startFight(ServerPlayer owner) {
        this.ownerId = owner.getUUID();
        this.fightTicks = HorrorConfig.OTHER_SECONDS.get() * 20;
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HorrorConfig.OTHER_HEALTH.get());
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(HorrorConfig.OTHER_DAMAGE.get());
        this.setHealth(this.getMaxHealth());
        this.setTarget(owner);
        this.bossEvent.addPlayer(owner);
        this.actionTicks = 30;
        beginTalk("No debías encontrarme.", "Siéntate. El suelo todavía está tibio.");
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    // ------------------------------------------------------------------ habla

    private void say(String msg) {
        Component c = Component.literal("<El Otro> ").withStyle(ChatFormatting.DARK_RED)
                .append(Component.literal(msg).withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        for (Player p : this.level().players()) {
            if (p.distanceToSqr(this) < 80.0D * 80.0D) p.sendSystemMessage(c);
        }
        this.level().playSound(null, getX(), getY() + 1.5D, getZ(), ModSounds.TAPE_VOICE.get(),
                SoundSource.HOSTILE, 2.5F, 0.7F);
    }

    /** Se detiene (sin IA) y suelta sus lineas una a una; despues vuelve a atacar. */
    private void beginTalk(String... lines) {
        for (String l : lines) queue.add(l);
        if (!talking) {
            talking = true;
            talkCooldown = 0;
            this.setNoAi(true);
            this.getNavigation().stop();
        }
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel sl)) return;
        if (ownerId == null) { this.discard(); return; }

        ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || owner.level() != sl || owner.isSpectator()) { endQuietly(); return; }
        if (!owner.isAlive()) {
            say("Uno más para la grieta.");
            endQuietly();
            return;
        }

        faceOwner(owner);
        if (!talking && (this.getTarget() == null || !this.getTarget().isAlive())) this.setTarget(owner);

        // cuenta atras
        fightTicks--;
        if (this.tickCount % 5 == 0) {
            int s = Math.max(0, fightTicks / 20);
            bossEvent.setProgress(Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0F, 1.0F));
            bossEvent.setName(Component.literal(String.format("EL OTRO    %d:%02d", s / 60, s % 60)));
        }
        if (!warned && fightTicks <= 600) {
            warned = true;
            beginTalk("Se acaba el tiempo. Para ti.");
        }
        if (fightTicks <= 0) {
            say("La grieta me reclama. Vuelve cuando quieras morir.");
            ModNetwork.send(owner, ClientEffectPacket.TITLE, 0, 0, "LA GRIETA LO RECLAM\u00d3");
            endQuietly();
            return;
        }

        // fases de dolor: se detiene a hablar
        if (phase < THRESH.length && this.getHealth() / this.getMaxHealth() <= THRESH[phase]) {
            beginTalk(PHASE_LINES[phase]);
            phase++;
        }
        if (talking) {
            if (talkCooldown > 0) {
                talkCooldown--;
            } else if (!queue.isEmpty()) {
                say(queue.poll());
                talkCooldown = 55;
            } else {
                talking = false;
                this.setNoAi(false);
                this.setTarget(owner);
            }
        } else {
            combat(sl, owner);
        }
        if (this.tickCount % 10 == 0) { // gotas de sangre flotando a su alrededor
            sl.sendParticles(new DustParticleOptions(new Vector3f(0.55F, 0.02F, 0.06F), 1.2F),
                    getX(), getY() + 1.4D, getZ(), 3, 0.9D, 1.3D, 0.9D, 0.01D);
        }
    }

    private void faceOwner(ServerPlayer owner) {
        double dx = owner.getX() - getX();
        double dz = owner.getZ() - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
    }

    /** Ciclo: espera -> destello y reaparece junto al jugador (carga) -> golpe -> espera (mas rapido con poca vida). */
    private void combat(ServerLevel sl, ServerPlayer owner) {
        if (actionTicks > 0) { actionTicks--; return; }
        if (!windup) {
            blinkNear(sl, owner);
            windup = true;
            this.entityData.set(DATA_WINDUP, true);
            actionTicks = 14;
        } else {
            strike(owner);
            windup = false;
            this.entityData.set(DATA_WINDUP, false);
            float ratio = Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0F, 1.0F);
            actionTicks = 36 + (int) (44.0F * ratio) + this.random.nextInt(14);
        }
    }

    private void blinkNear(ServerLevel sl, ServerPlayer owner) {
        Vec3 spot = pickSpot(sl, owner);
        if (spot == null) return;
        burst(sl, getX(), getY() + 1.4D, getZ());
        this.moveTo(spot.x, spot.y, spot.z, this.getYRot(), 0.0F);
        this.getNavigation().stop();
        faceOwner(owner);
        this.entityData.set(DATA_BLINK, (int) sl.getGameTime());
        burst(sl, spot.x, spot.y + 1.4D, spot.z);
        ModNetwork.send(owner, ClientEffectPacket.BLOOD_FLASH, 6, 0, "");
    }

    private Vec3 pickSpot(ServerLevel sl, ServerPlayer owner) {
        for (int i = 0; i < 16; i++) {
            double ang = this.random.nextDouble() * Math.PI * 2.0D;
            double d = 2.8D + this.random.nextDouble();
            int x = Mth.floor(owner.getX() + Math.cos(ang) * d);
            int z = Mth.floor(owner.getZ() + Math.sin(ang) * d);
            Optional<BlockPos> g = HorrorUtil.ground(sl, x, z, owner.getBlockY(), 3, 4);
            if (g.isEmpty()) continue;
            Vec3 f = new Vec3(g.get().getX() + 0.5D, g.get().getY(), g.get().getZ() + 0.5D);
            AABB box = this.getDimensions(this.getPose()).makeBoundingBox(f);
            if (sl.noCollision(this, box)) return f;
        }
        return null;
    }

    private void strike(ServerPlayer owner) {
        this.swing(InteractionHand.MAIN_HAND);
        if (owner.distanceToSqr(this) < 6.0D * 6.0D) {   // si el jugador se alejo, falla
            this.doHurtTarget(owner);
            ModNetwork.send(owner, ClientEffectPacket.BLOOD_FLASH, 10, 0, "");
        }
    }

    /** Explosion de sangre y brillo (al desaparecer y al aparecer). */
    private void burst(ServerLevel sl, double x, double y, double z) {
        sl.sendParticles(new DustParticleOptions(new Vector3f(0.75F, 0.02F, 0.06F), 2.2F), x, y, z, 70, 0.9D, 1.2D, 0.9D, 0.2D);
        sl.sendParticles(new DustParticleOptions(new Vector3f(0.40F, 0.0F, 0.04F), 1.6F), x, y, z, 45, 1.3D, 1.5D, 1.3D, 0.25D);
        sl.sendParticles(ParticleTypes.END_ROD, x, y, z, 28, 0.8D, 1.1D, 0.8D, 0.14D);
        sl.sendParticles(ParticleTypes.CRIMSON_SPORE, x, y, z, 30, 1.0D, 1.2D, 1.0D, 0.05D);
        sl.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        sl.playSound(null, x, y, z, ModSounds.IMPACT.get(), SoundSource.HOSTILE, 1.4F, 1.2F);
    }

    private void endQuietly() {
        this.bossEvent.removeAllPlayers();
        this.discard();
    }

    // ------------------------------------------------------------------ combate

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok && target instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
        }
        return ok;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // solo le hacen dano quienes lo golpean (nada de trampas de lava, cactus o ahogarlo)
        if (source.getEntity() == null && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && !deathHandled) {
            deathHandled = true;
            ItemEntity drop = new ItemEntity(this.level(), getX(), getY() + 1.0D, getZ(), createIndex());
            drop.setUnlimitedLifetime();
            drop.setInvulnerable(true);
            drop.setDeltaMovement(0.0D, 0.25D, 0.0D);
            this.level().addFreshEntity(drop);
            say("...gracias.");
            say("La grieta cada vez se fractura más.");
            for (Player p : this.level().players()) {
                if (p instanceof ServerPlayer sp && p.distanceToSqr(this) < 80.0D * 80.0D) {
                    ModNetwork.send(sp, ClientEffectPacket.TITLE, 0, 0, "EL OTRO HA CA\u00cdDO");
                }
            }
            if (source.getEntity() instanceof ServerPlayer killer) {
                CompoundTag d = killer.getPersistentData();
                CompoundTag t = d.getCompound("PlayerPersisted");
                t.putInt("anexohorror_others_killed", t.getInt("anexohorror_others_killed") + 1);
                d.put("PlayerPersisted", t);
            }
            this.bossEvent.removeAllPlayers();
        }
        super.die(source);
    }

    /** La estrella del Nether "El Indice". */
    public static ItemStack createIndex() {
        ItemStack star = new ItemStack(Items.NETHER_STAR);
        star.setHoverName(Component.literal("El \u00cdndice")
                .withStyle(s -> s.withItalic(false).withBold(true).withColor(ChatFormatting.DARK_PURPLE)));
        CompoundTag display = star.getOrCreateTagElement("display");
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("La grieta cada vez se fractura m\u00e1s")
                .withStyle(s -> s.withItalic(false).withColor(ChatFormatting.GRAY)))));
        display.put("Lore", lore);
        star.getOrCreateTag().putBoolean("anexohorror_index", true);
        return star;
    }

    @Override
    protected SoundEvent getAmbientSound() { return ModSounds.BREATH.get(); }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return ModSounds.CRACK.get(); }

    @Override
    protected SoundEvent getDeathSound() { return ModSounds.IMPACT.get(); }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }
}
