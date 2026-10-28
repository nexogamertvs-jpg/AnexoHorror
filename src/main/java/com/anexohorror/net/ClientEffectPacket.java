package com.anexohorror.net;

import com.anexohorror.client.ClientEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: dispara un efecto en la pantalla del jugador. */
public final class ClientEffectPacket {
    public static final int VIDEO = 0;     // video VHS de anochecer
    public static final int SCREAMER = 1;  // a = indice de imagen (-1 = al azar), text = mensaje
    public static final int BLACKOUT = 2;  // a = ticks
    public static final int STATIC = 3;    // a = ticks

    private final int kind;
    private final int a;
    private final int b;
    private final String text;

    public ClientEffectPacket(int kind, int a, int b, String text) {
        this.kind = kind;
        this.a = a;
        this.b = b;
        this.text = text == null ? "" : text;
    }

    public static void encode(ClientEffectPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.kind);
        buf.writeInt(p.a);
        buf.writeInt(p.b);
        buf.writeUtf(p.text, 256);
    }

    public static ClientEffectPacket decode(FriendlyByteBuf buf) {
        return new ClientEffectPacket(buf.readVarInt(), buf.readInt(), buf.readInt(), buf.readUtf(256));
    }

    public static void handle(ClientEffectPacket p, Supplier<NetworkEvent.Context> sup) {
        NetworkEvent.Context ctx = sup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientEffects.handle(p.kind, p.a, p.b, p.text)));
        ctx.setPacketHandled(true);
    }
}
