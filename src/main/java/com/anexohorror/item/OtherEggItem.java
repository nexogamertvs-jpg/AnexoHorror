package com.anexohorror.item;

import com.anexohorror.director.HorrorEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Huevo de "El Otro": inicia el combate de jefe (120 s) donde clicas. */
public class OtherEggItem extends Item {
    public OtherEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel sl) || !(ctx.getPlayer() instanceof ServerPlayer sp)) return InteractionResult.PASS;
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        Vec3 at = new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        if (HorrorEvents.spawnOther(sp, sl, at) != null && !sp.getAbilities().instabuild) {
            ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.anexohorror.egg_other.tooltip").withStyle(ChatFormatting.RED));
    }
}
