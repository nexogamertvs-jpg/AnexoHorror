# AnexoHorror (Forge 1.20.1 / 47.2.0)

Mod de terror analógico y psicológico. Siete presencias (seis planos con tus imagenes + un modelo 3D .obj) que observan,
nunca persiguen ni atacan.

## Compilar
Requiere JDK 17 y conexión a internet (descarga Forge/Minecraft).

    gradle wrapper --gradle-version 8.1.1     # una sola vez (o copia gradlew + gradle/ del MDK 1.20.1-47.x)
    ./gradlew build                           # jar en build/libs/anexohorror-1.5.0.jar

Alternativa sin instalar nada: sube la carpeta a GitHub; `.github/workflows/build.yml` compila el jar.
Para probar: `./gradlew runClient`.

## Comandos de prueba (OP nivel 2)
- `/anexohorror spawn [0-5]`  presencia delante de ti (0 peeker, 1 grin, 2 smiler, 3 drawn, 4 crawler, 5 eye, 6 slender 3D)
- `/anexohorror event <stare|peripheral|peek|door|bed|cave|fake_sound|footsteps|silence>`
- `/anexohorror status`       miedo / audacia / nerviosismo aprendidos del jugador

## Configuración
`config/anexohorror-common.toml`: intensidad, distancias mínimas, máximo de entidades, sonidos falsos,
silencios y escalada nocturna.

## Archivos originales
`originals/` conserva tus PNG y el MP3. En el mod: texturas con fondo transparente
(`textures/entity/*.png`) y `crack.ogg` (tu MP3 convertido a OGG mono, requerido por Minecraft para sonido posicional).

## v1.1.0
- Nueva presencia 3D `slender` (modelo `obj/slender.obj`, decimado de 798k a 29k triangulos; textura reentonada a la paleta de la foto de referencia).
- Animacion por codigo: respira, se inclina hacia ti y da sacudidas bruscas a "fotogramas".
- Efectos de pantalla al mirar una presencia: tinte mauve, vineta, lineas de escaneo, grano, cortes de cinta y latido.
- El .obj original (97 MB) no esta en el repo (GitHub web admite 25 MB por archivo): guardalo tu.

## v1.1.1
- Rendimiento: modelo 3D de 29k a 11k triangulos (LOD de 3.4k a mas de 22 bloques), transformacion sin asignaciones por vertice, precarga del modelo al cargar recursos (sin tiron al aparecer) y overlay mas ligero.

## v1.2.0
- Las seis presencias 2D se reemplazaron por nuevas imagenes, recortadas sin fondo (alpha limpio, sin halos):
  peeker = figura negra con mascara, grin = esqueletica, smiler = muchos brazos con sombrero,
  drawn = hombre alto encorvado, crawler = criatura con cuernos, eye = gritador de espinas. El modelo 3D `slender` se mantiene.
- Originales en `originals/v1.2/`.

## v1.3.0
- **Video de introduccion** (VHS de advertencia): 312 fotogramas a 12 fps empaquetados en `assets/anexohorror/video/intro.bin` + audio `sounds/intro.ogg`.
  Sale la primera vez que entras a cada mundo/servidor (configurable: NEW_WORLD / ONCE / ALWAYS / OFF). ESC lo salta.
  En un mundo propio el jugador es invulnerable mientras suena. El video original (27 MB) no esta en el repo.
- **Flashbacks**: dos imagenes que cubren la pantalla de golpe cada 4-10 min (mas seguido de noche), ajustadas enteras sin recortar la entidad.
- **Crujido limpio**: `crack.ogg` ahora es UNA sola fractura aislada (0.3 s). Alternativas en `originals/crack_alternativas/`.
- **Frases en pantalla** al aparecer una presencia (sin repetir las 3 ultimas), en rojo sangre con aberracion cromatica, parpadeo y letras que se corrompen.
- **Signos extranos**: maximo 6 a la vez, efimeros.
- Opciones en `config/anexohorror-client.toml` (puedes desactivar flashbacks, texto, signos y efectos si eres sensible a destellos).
- Comandos de prueba del cliente: `/ahc intro`, `/ahc flash`, `/ahc text`.

## v1.4.0
- **Video en alta calidad**: 960x540 a 20 fps (642 fotogramas JPEG, 21 MB repartidos en `video/intro_0.bin` + `intro_1.bin`, indice `intro.idx`) y audio nuevo.
- **El video sale cada vez que anochece** (modo `NIGHTFALL`, por defecto). Tambien existe `NEW_WORLD` (primera vez en cada mundo) y `OFF`. Cambialo en `config/anexohorror-client.toml`.
- **Las presencias solo aparecen de noche** (tiempo 13000-23000) y se esfuman al amanecer. De dia quedan solo sonidos/eventos sin entidad. Los comandos de prueba (`/anexohorror spawn`, `/anexohorror event`) siguen funcionando de dia.
- **Aura luminosa**: halo suave y emisivo que sigue la silueta de cada presencia (`textures/entity/*_glow.png`), con pulso lento y parpadeo. Ajustable con `glow` y `glowStrength` en la config del cliente.

## v1.5.0
- **VHS al anochecer garantizado**: el SERVIDOR vigila el reloj del mundo y, al cruzar el tick 13000, manda el video al jugador (paquete de red). Ya no depende de la config vieja ni del reloj del cliente. Nueva clave `intro.mode` (las instalaciones anteriores conservaban `NEW_WORLD`).
- **No se puede dormir**: cada intento se cancela y lanza un screamer (8 imagenes) con el mensaje "NO ESCAPARAS DE ESTA REALIDAD", chillido, impacto y acufeno. La cama sigue fijando el spawn (`bedSetsSpawn`). Desactivable con `blockSleep`.
- **Noches vividas**: cada noche sube un poco la frecuencia de eventos (hasta +96%).
- **Nueva entidad** `stalker` (silueta de garras largas) con su aura.
- **Eventos nocturnos nuevos**: apagon de pantalla, rafaga de estatica, llamadas lejanas (criatura / sirena / tono anomalo), mensajes de chat falsos.
- **Sonidos**: se usan tus 6 audios (susurro de cinta, llamada de criatura, sirena, tono anomalo, musica turbia tras el video) + sintetizados (screamer, impacto, acufeno, cinta que se detiene).
- **Visual**: marcas de camara VHS de noche (REC, PLAY, contador, fecha), grano suave nocturno, temblor de camara, latido que se acelera al mirar, ceniza alrededor de las presencias.
- Comandos de prueba: `/ahc screamer|blackout|static` y `/anexohorror event blackout|static_burst|far_call|fake_chat`.
