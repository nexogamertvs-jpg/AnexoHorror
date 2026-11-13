package com.anexohorror.item;

import com.anexohorror.almanac.Almanac;
import com.anexohorror.almanac.AlmanacEntry;
import com.anexohorror.entity.HorrorEntity;
import com.anexohorror.entity.HorrorMode;
import com.anexohorror.entity.OtherEntity;
import com.anexohorror.net.ClientEffectPacket;
import com.anexohorror.net.ModNetwork;
import com.anexohorror.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Camara fotografica. Clic derecho: foto con flash (si hay una entidad del mod en el encuadre, la registra
 * en el almanaque). Agachado + clic derecho: abre el almanaque.
 */
public class CameraItem extends Item {
    /** Alcance maximo de la foto, en bloques. */
    private static final double RANGE = 48.0D;
    /** Coseno del semiangulo del encuadre (~28 grados). */
    private static final double FRAME_COS = 0.88D;
    private static final int COOLDOWN_TICKS = 30;

    public CameraItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (player instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
            if (sp.isShiftKeyDown()) {
                ModNetwork.send(sp, ClientEffectPacket.OPEN_ALMANAC, Almanac.getMask(sp), 0, "");
            } else {
                if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.consume(stack);
                takePhoto(sl, sp);
                sp.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            }
        }
        return InteractionResultHolder.consume(stack);
    }

    private static void takePhoto(ServerLevel level, ServerPlayer sp) {
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.PLAYERS, 1.0F, 1.6F);
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), ModSounds.STATIC.get(), SoundSource.PLAYERS, 0.35F, 1.4F);
        ModNetwork.send(sp, ClientEffectPacket.CAMERA_FLASH, 14, 0, "");

        Set<AlmanacEntry> framed = findFramed(level, sp);
        if (framed.isEmpty()) {
            sp.displayClientMessage(Component.translatable("item.anexohorror.camera.empty").withStyle(ChatFormatting.GRAY), true);
            return;
        }

        List<AlmanacEntry> fresh = new ArrayList<>();
        for (AlmanacEntry entry : framed) {
            if (Almanac.unlock(sp, entry)) fresh.add(entry);
        }

        if (fresh.isEmpty()) {
            sp.displayClientMessage(Component.translatable("item.anexohorror.camera.known", names(framed))
                    .withStyle(ChatFormatting.GRAY), true);
        } else {
            int count = Almanac.count(Almanac.getMask(sp));
            sp.displayClientMessage(Component.translatable("item.anexohorror.camera.new", names(fresh), count, AlmanacEntry.COUNT)
                    .withStyle(ChatFormatting.RED), true);
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), ModSounds.IMPACT.get(), SoundSource.PLAYERS, 0.4F, 0.6F);
        }
    }

    private static MutableComponent names(Iterable<AlmanacEntry> entries) {
        MutableComponent out = Component.empty();
        boolean first = true;
        for (AlmanacEntry e : entries) {
            if (!first) out.append(Component.literal(", "));
            out.append(e.displayName());
            first = false;
        }
        return out;
    }

    /** Entidades del mod dentro del cono del encuadre y con linea de vision despejada. */
    private static Set<AlmanacEntry> findFramed(ServerLevel level, ServerPlayer sp) {
        Set<AlmanacEntry> found = EnumSet.noneOf(AlmanacEntry.class);
        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        AABB area = sp.getBoundingBox().inflate(RANGE);
        List<Entity> candidates = level.getEntities(sp, area,
                e -> e.isAlive() && (e instanceof HorrorEntity || e instanceof OtherEntity));
        for (Entity e : candidates) {
            AlmanacEntry entry = AlmanacEntry.of(e);
            if (entry == null || found.contains(entry)) continue;
            for (Vec3 point : samplePoints(e)) {
                Vec3 to = point.subtract(eye);
                double dist = to.length();
                if (dist < 0.5D || dist > RANGE) continue;
                if (look.dot(to.scale(1.0D / dist)) < FRAME_COS) continue;
                if (!visible(level, sp, eye, point)) continue;
                found.add(entry);
                break;
            }
        }
        return found;
    }

    /** Puntos del cuerpo que se comprueban; los asomos solo muestran la cabeza, asi que se prueba ahi primero. */
    private static Vec3[] samplePoints(Entity e) {
        if (e instanceof HorrorEntity he) {
            double hover = he.spriteHover();
            double h = he.spriteHeight();
            Vec3 base = he.position();
            if (he.getMode() == HorrorMode.PEEK) {
                // solo cuenta mientras la cabeza esta realmente asomada
                if (he.isPeekGone() || he.getPeekProgress() < 0.25F) return new Vec3[0];
                return new Vec3[]{he.headWorldPoint(he.getPeekProgress())};
            }
            return new Vec3[]{
                    base.add(0.0D, hover + h * 0.6D, 0.0D),
                    base.add(0.0D, hover + h * 0.85D, 0.0D),
                    base.add(0.0D, hover + h * 0.3D, 0.0D)};
        }
        AABB box = e.getBoundingBox();
        return new Vec3[]{box.getCenter(), new Vec3(e.getX(), e.getEyeY(), e.getZ())};
    }

    private static boolean visible(ServerLevel level, ServerPlayer sp, Vec3 eye, Vec3 point) {
        BlockHitResult hit = level.clip(new ClipContext(eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
        return hit.getType() == HitResult.Type.MISS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.anexohorror.camera.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.anexohorror.camera.tooltip2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
