package com.anexohorror.director;

import com.anexohorror.AnexoHorror;
import com.anexohorror.HorrorConfig;
import com.anexohorror.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

/**
 * Cofres aleatorios con la historia del Anexo. Cada jugador recibe los registros en orden:
 * el siguiente cofre solo aparece cuando abre el anterior (y pasan unos minutos).
 */
@Mod.EventBusSubscriber(modid = AnexoHorror.MODID)
public final class StoryChests {
    private static final String PERSISTED = "PlayerPersisted";
    private static final String K_IDX = "anexo_story";       // siguiente registro (0..COUNT)
    private static final String K_POS = "anexo_chest";       // posicion del cofre pendiente
    private static final String K_TIME = "anexo_chest_time"; // cuando se coloco
    private static final String K_NEXT = "anexo_chest_next"; // no colocar otro antes de este tick

    private static final Item[] LOOT = {Items.BREAD, Items.TORCH, Items.IRON_INGOT, Items.ARROW, Items.BONE,
            Items.COAL, Items.COOKED_BEEF, Items.GOLDEN_CARROT, Items.STICK, Items.PAPER, Items.CANDLE};

    private static CompoundTag data(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        CompoundTag t = d.getCompound(PERSISTED);
        d.put(PERSISTED, t);
        return t;
    }

    public static int found(ServerPlayer p) {
        return data(p).getInt(K_IDX);
    }

    /** Se llama cada 200 ticks desde PlayerState (solo en el Overworld). */
    static void tick(ServerPlayer p, ServerLevel l) {
        if (!HorrorConfig.STORY_CHESTS.get()) return;
        CompoundTag t = data(p);
        int idx = t.getInt(K_IDX);
        if (idx >= StoryEntries.COUNT) return; // historia completa
        long now = l.getGameTime();

        if (t.contains(K_POS)) {
            BlockPos cp = BlockPos.of(t.getLong(K_POS));
            boolean loaded = l.hasChunkAt(cp);
            boolean exists = !loaded || l.getBlockState(cp).is(Blocks.CHEST);
            double d2 = p.blockPosition().distSqr(cp);
            if (!exists || (now - t.getLong(K_TIME) > 36000L && d2 > 150.0D * 150.0D)) {
                if (loaded && exists) l.removeBlock(cp, false); // cofre lejano sin abrir: se retira y se coloca otro
                t.remove(K_POS);
            } else {
                if (loaded && d2 < 48.0D * 48.0D && p.getRandom().nextInt(4) == 0) { // pista sonora
                    HorrorUtil.sound(p, ModSounds.WHISPER.get(), SoundSource.HOSTILE, Vec3.atCenterOf(cp), 1.1F, 0.8F);
                }
                return;
            }
        }
        if (PlayerState.nights(p) < 1) return;   // la historia empieza tras la primera noche
        if (now < t.getLong(K_NEXT)) return;
        placeChest(p, l, t, idx);
    }

    private static void placeChest(ServerPlayer p, ServerLevel l, CompoundTag t, int idx) {
        RandomSource r = p.getRandom();
        for (int i = 0; i < 40; i++) {
            double ang = r.nextDouble() * Math.PI * 2.0D;
            double d = 30.0D + r.nextDouble() * 40.0D;
            int x = Mth.floor(p.getX() + Math.cos(ang) * d);
            int z = Mth.floor(p.getZ() + Math.sin(ang) * d);
            if (!l.hasChunkAt(new BlockPos(x, p.getBlockY(), z))) continue;
            Optional<BlockPos> g = HorrorUtil.ground(l, x, z, p.getBlockY(), 20, 30);
            if (g.isEmpty()) continue;
            BlockPos gp = g.get();
            if (!l.getFluidState(gp.below()).isEmpty()) continue;
            Direction face = Direction.Plane.HORIZONTAL.getRandomDirection(r);
            l.setBlock(gp, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, face), 3);
            if (l.getBlockEntity(gp) instanceof ChestBlockEntity chest) {
                chest.setCustomName(Component.literal("Archivo del Anexo"));
                chest.setItem(13, StoryEntries.book(idx));
                int n = 2 + r.nextInt(4);
                for (int k = 0; k < n; k++) {
                    int slot = r.nextInt(27);
                    if (slot == 13 || !chest.getItem(slot).isEmpty()) continue;
                    Item it = LOOT[r.nextInt(LOOT.length)];
                    chest.setItem(slot, new ItemStack(it, 1 + r.nextInt(it == Items.TORCH || it == Items.ARROW ? 8 : 4)));
                }
            } else {
                l.removeBlock(gp, false);
                continue;
            }
            t.putLong(K_POS, gp.asLong());
            t.putLong(K_TIME, l.getGameTime());
            return;
        }
    }

    /** Al abrir el cofre pendiente se avanza la historia y se programa el siguiente. */
    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer sp)) return;
        CompoundTag t = data(sp);
        if (!t.contains(K_POS) || t.getLong(K_POS) != event.getPos().asLong()) return;
        int idx = t.getInt(K_IDX);
        t.putInt(K_IDX, idx + 1);
        t.remove(K_POS);
        t.putLong(K_NEXT, event.getLevel().getGameTime() + 3600L + sp.getRandom().nextInt(6000)); // 3 a 8 min
        sp.sendSystemMessage(Component.literal("» Registro " + (idx + 1) + " de " + StoryEntries.COUNT + " encontrado.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    private StoryChests() {}
}
