package com.anexohorror.net;

import com.anexohorror.AnexoHorror;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AnexoHorror.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void init() {
        CHANNEL.registerMessage(0, ClientEffectPacket.class, ClientEffectPacket::encode,
                ClientEffectPacket::decode, ClientEffectPacket::handle);
    }

    public static void send(ServerPlayer p, int kind, int a, int b, String text) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new ClientEffectPacket(kind, a, b, text));
    }

    private ModNetwork() {}
}
