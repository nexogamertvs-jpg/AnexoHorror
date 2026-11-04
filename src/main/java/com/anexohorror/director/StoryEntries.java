package com.anexohorror.director;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** La historia del Anexo 7, en orden cronologico. Cada cofre entrega el siguiente registro. */
public final class StoryEntries {
    private static final String AUTHOR = "Ing. M. Salcedo";

    /** {titulo, autor, texto} */
    private static final String[][] ENTRIES = {
        {"Registro 01 · Día 3", AUTHOR,
            "Me asignaron el turno de noche en el Anexo 7. Nadie quiso explicarme por qué el edificio lleva tres años sin inquilinos y aun así paga luz. Instalé los sensores en el pasillo norte. A las 3:12 el medidor de ruido marcó un crujido, como de hueso al partirse. No había nadie."},
        {"Registro 02 · Día 9", AUTHOR,
            "Las cámaras graban algo en la esquina del cuarto 4: una figura enorme sentada en el suelo, con la espalda doblada y la cabeza ladeada. Nosotros no tenemos a nadie así. Cuando miro el monitor, el rincón está vacío. Cuando miro el pasillo y regreso al monitor, la figura vuelve a estar. Ríos dice que es la cinta. Yo ya no estoy seguro de que sea la cinta."},
        {"Registro 03 · Día 14", AUTHOR,
            "Ríos no vino hoy. Dejó su cuaderno abierto: había anotado cuántas noches llevaba y, debajo, una sola regla subrayada tres veces: NO DUERMAS. Dice que quien duerme en el Anexo despierta con algo ya mirándolo desde la puerta. No dormí. Tomé café hasta que me dolieron los ojos."},
        {"Registro 04 · Día 21", AUTHOR,
            "Lo que he comprobado: salen de noche. Se quedan quietos. Desaparecen si les das la espalda y vuelven más cerca si no los miras. Se asoman por las puertas, por los cristales, por las esquinas. Nunca me han tocado. Creo que no pueden. Creo que solo esperan a que corra, a que haga ruido, a que me rompa."},
        {"Registro 05 · Día 30", AUTHOR,
            "Encontré la grieta. Está en el sótano, detrás del tablero eléctrico: una línea fina y negra, de arriba abajo en el muro. La medí con una regla. Ayer eran 31 cm. Hoy son 34. Cada noche se fractura un poco más y cada vez que lo hace se oye el crujido. Las cosas no vienen al Anexo. Salen de ahí."},
        {"Registro 06 · Día 38", AUTHOR,
            "En el archivo viejo hallé el Índice: una estrella pálida del tamaño de un puño, junto a un libro de cuentas que registra cada noche. El encargado anterior escribió: «Mientras alguien lo sostenga, la grieta se mantiene a raya». Al lado hay una sola palabra repetida muchas veces: el otro, el otro, el otro."},
        {"Registro 07 · Día 41", AUTHOR,
            "Hoy lo vi hablar. No es como las demás. Es más grande, más lento, y se mueve cuando nadie lo mira. Se sienta en el suelo del cuarto 4, doblado, y espera. Ríos le llamó «el otro» en su cuaderno. Es lo que queda cuando algo atraviesa la grieta y no logra volver. Habla con la voz de quienes ya no están."},
        {"Registro 08 · Día 44", AUTHOR,
            "Descubrí cuánto dura. Cuando el otro se manifiesta, la grieta lo reclama de vuelta en dos minutos. Ciento veinte segundos. Es el único momento en que se le puede herir. No es fácil: es fuerte y aguanta mucho. Pero se detiene a hablar cuando algo le duele. Escucha lo que dice. Miente menos cuando sufre."},
        {"Registro 09 · Día 46", AUTHOR,
            "Si lees esto, probablemente ya estoy sentado en el suelo del cuarto 4, esperando. Mátalo. Cuando caiga, soltará el Índice. Tómalo y ponlo en manos de quien aún respire. La grieta cada vez se fractura más y no sé cuánto tiempo nos queda. No te quedes quieto. No duermas."},
        {"Registro 10 · Sin fecha", "???",
            "No escaparás. No escaparás. No escaparás. No escaparás. No escaparás. (la tinta se corre en el resto de la página)"},
    };

    public static final int COUNT = ENTRIES.length;

    /** Libro firmado y desgastado con el registro numero idx (0..COUNT-1). */
    public static ItemStack book(int idx) {
        String[] e = ENTRIES[Math.max(0, Math.min(COUNT - 1, idx))];
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("title", e[0]);
        tag.putString("author", e[1]);
        tag.putInt("generation", 3); // "Desgastado"
        tag.putByte("resolved", (byte) 1);
        ListTag pages = new ListTag();
        for (String page : paginate(e[0] + "\n\n" + e[2])) {
            pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(page))));
        }
        tag.put("pages", pages);
        return stack;
    }

    private static List<String> paginate(String text) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            if (cur.length() + word.length() + 1 > 230) {
                out.add(cur.toString().trim());
                cur.setLength(0);
            }
            cur.append(word).append(' ');
        }
        if (cur.length() > 0) out.add(cur.toString().trim());
        return out;
    }

    private StoryEntries() {}
}
