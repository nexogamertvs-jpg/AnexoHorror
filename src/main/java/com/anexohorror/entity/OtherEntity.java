package com.anexohorror.entity;

import com.anexohorror.HorrorConfig;
import com.anexohorror.net.ClientEffectPacket;
import com.anexohorror.net.ModNetwork;
import com.anexohorror.registry.ModSounds;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
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
            {"Si me matas, alguien tiene que ocupar la silla."},
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

    public OtherEntity(EntityType<? extends OtherEntity> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 350.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
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
        beginTalk("No debías encontrarme.", "Siéntate. La silla todavía está tibia.");
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
        }
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
