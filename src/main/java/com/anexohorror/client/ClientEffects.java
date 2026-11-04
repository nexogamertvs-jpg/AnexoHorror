package com.anexohorror.client;

import com.anexohorror.ClientConfig;
import com.anexohorror.net.ClientEffectPacket;

/** Recibe en el cliente los efectos que manda el servidor. */
public final class ClientEffects {
    public static void handle(int kind, int a, int b, String text) {
        switch (kind) {
            case ClientEffectPacket.VIDEO -> {
                if (ClientConfig.INTRO_MODE.get() != ClientConfig.IntroMode.OFF) IntroTrigger.queueVideo();
            }
            case ClientEffectPacket.SCREAMER -> PsychoClientTick.triggerScreamer(a, text);
            case ClientEffectPacket.BLACKOUT -> PsychoClientTick.triggerBlackout(a);
            case ClientEffectPacket.STATIC -> PsychoClientTick.triggerStatic(a);
            case ClientEffectPacket.NIGHT_TITLE -> {
                if (ClientConfig.NIGHT_TITLE.get()) PsychoClientTick.triggerNightTitle(a);
            }
            case ClientEffectPacket.BLOOD_FLASH -> PsychoClientTick.triggerBloodFlash(a);
            case ClientEffectPacket.TITLE -> PsychoClientTick.triggerTitle(text);
            case ClientEffectPacket.LOCK -> PsychoClientTick.triggerLock(a, b, text);
            default -> { }
        }
    }

    private ClientEffects() {}
}
