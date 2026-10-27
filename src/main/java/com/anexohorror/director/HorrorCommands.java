package com.anexohorror.director;

import com.anexohorror.AnexoHorror;
import com.anexohorror.entity.HorrorMode;
import com.anexohorror.entity.HorrorVariant;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.Optional;

/** Comandos de prueba (nivel 2): /anexohorror event|spawn|status */
@Mod.EventBusSubscriber(modid = AnexoHorror.MODID)
public final class HorrorCommands {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("anexohorror")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("event")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(EventType.values()).map(e -> e.name().toLowerCase()), b))
                                .executes(c -> runEvent(c.getSource(), StringArgumentType.getString(c, "name")))))
                .then(Commands.literal("spawn")
                        .executes(c -> spawn(c.getSource(), -1))
                        .then(Commands.argument("variant", IntegerArgumentType.integer(0, HorrorVariant.values().length - 1))
                                .executes(c -> spawn(c.getSource(), IntegerArgumentType.getInteger(c, "variant")))))
                .then(Commands.literal("status").executes(c -> status(c.getSource()))));
    }

    private static int runEvent(CommandSourceStack src, String name) {
        try {
            ServerPlayer p = src.getPlayerOrException();
            EventType t;
            try {
                t = EventType.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException ex) {
                src.sendFailure(Component.literal("Evento desconocido: " + name));
                return 0;
            }
            boolean ok;
            HorrorUtil.forceDay = true;
            try {
                ok = HorrorEvents.run(t, p, p.serverLevel(), HorrorDirector.stateFor(p));
            } finally {
                HorrorUtil.forceDay = false;
            }
            if (ok) src.sendSuccess(() -> Component.literal("Evento ejecutado: " + t), false);
            else src.sendFailure(Component.literal("No hubo lugar valido para: " + t));
            return ok ? 1 : 0;
        } catch (Exception ex) {
            src.sendFailure(Component.literal("Solo un jugador puede ejecutar esto."));
            return 0;
        }
    }

    private static int spawn(CommandSourceStack src, int variant) {
        try {
            ServerPlayer p = src.getPlayerOrException();
            ServerLevel l = p.serverLevel();
            PlayerState st = HorrorDirector.stateFor(p);
            HorrorVariant v = variant < 0
                    ? HorrorVariant.values()[p.getRandom().nextInt(HorrorVariant.values().length)]
                    : HorrorVariant.byId(variant);
            Vec3 look = p.getLookAngle();
            Vec3 h = new Vec3(look.x, 0.0D, look.z).normalize();
            for (double d : new double[]{14.0D, 12.0D, 16.0D, 10.0D, 20.0D}) {
                Optional<BlockPos> g = HorrorUtil.ground(l, (int) Math.floor(p.getX() + h.x * d),
                        (int) Math.floor(p.getZ() + h.z * d), p.getBlockY(), 5, 12);
                if (g.isPresent()) {
                    Vec3 pos = new Vec3(g.get().getX() + 0.5D, g.get().getY(), g.get().getZ() + 0.5D);
                    HorrorEvents.spawn(p, l, st, v, HorrorMode.STARE, pos, 0, 1.0F);
                    src.sendSuccess(() -> Component.literal("Aparece: " + v.name()), false);
                    return 1;
                }
            }
            src.sendFailure(Component.literal("No hay suelo valido delante de ti."));
            return 0;
        } catch (Exception ex) {
            src.sendFailure(Component.literal("Solo un jugador puede ejecutar esto."));
            return 0;
        }
    }

    private static int status(CommandSourceStack src) {
        try {
            ServerPlayer p = src.getPlayerOrException();
            PlayerState st = HorrorDirector.stateFor(p);
            String msg = String.format("miedo=%.2f audacia=%.2f nerviosismo=%.2f encuentros=%d archivo=%d proximoEvento=%ds noche=%.2f",
                    st.fear, st.bold, st.jumpy, st.successfulEncounters, st.archiveIndex, st.cooldown / 20,
                    PlayerState.nightFactor(p.serverLevel()));
            src.sendSuccess(() -> Component.literal(msg), false);
            return 1;
        } catch (Exception ex) {
            return 0;
        }
    }

    private HorrorCommands() {}
}
