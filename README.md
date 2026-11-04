# AnexoHorror (Forge 1.20.1 / 47.2.0)

Mod de terror analógico y psicológico. Siete presencias principales (seis planos con tus imagenes + una entidad 3D .obj), con comportamiento de acecho. Una variante especial puede iniciar una persecucion y El Otro conserva su combate de jefe.

## Compilar
Requiere JDK 17 y conexión a internet (descarga Forge/Minecraft).

    gradle wrapper --gradle-version 8.1.1     # una sola vez (o copia gradlew + gradle/ del MDK 1.20.1-47.x)
    ./gradlew build                           # jar en build/libs/anexohorror-1.9.0.jar

Alternativa sin instalar nada: sube la carpeta a GitHub; `.github/workflows/build.yml` compila el jar.
Para probar: `./gradlew runClient`.

## Comandos de prueba (OP nivel 2)
- `/anexohorror spawn [0-5]`  presencia delante de ti (0 peeker, 1 grin, 2 smiler, 3 drawn, 4 crawler, 5 eye, 6 slender 3D)
- `/anexohorror event <stare|peripheral|peek|door|bed|cave|fake_sound|footsteps|silence|window|hunt|other|blackout|static_burst|far_call|fake_chat>`
- `/anexohorror status`       miedo / audacia / nerviosismo aprendidos del jugador

## Configuración
`config/anexohorror-common.toml`: intensidad, distancias mínimas, máximo de entidades, sonidos falsos,
silencios y escalada nocturna. Para conservar el acecho a distancia, los valores recomendados son `minDistanceDay = 24` y `minDistanceNight = 18`; los archivos de configuracion antiguos pueden conservar sus valores previos.

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
- **El video sale cada vez que anochece** (modo `NIGHTFALL`, por defecto). `NEW_WORLD` tambien lo muestra al entrar por primera vez a cada mundo; `OFF` desactiva los videos. Cambialo en `config/anexohorror-client.toml`.
- **Las presencias solo aparecen de noche** (tiempo 13000-23000) y se esfuman al amanecer. De dia quedan solo sonidos/eventos sin entidad. Los comandos de prueba (`/anexohorror spawn`, `/anexohorror event`) siguen funcionando de dia.
- **Aura luminosa**: halo suave y emisivo que sigue la silueta de cada presencia (`textures/entity/*_glow.png`), con pulso lento y parpadeo. Ajustable con `glow` y `glowStrength` en la config del cliente.

## v1.5.0
- **VHS al anochecer garantizado**: el SERVIDOR vigila el reloj del mundo y, al cruzar el tick 13000, manda el video al jugador (paquete de red). Ya no depende de la config vieja ni del reloj del cliente. Nueva clave `intro.mode` (las instalaciones anteriores conservaban `NEW_WORLD`).
- **No se puede dormir**: cada intento se cancela y lanza un screamer (8 imagenes) con el mensaje "NO ESCAPARAS DE ESTA REALIDAD", chillido, impacto y acufeno. La cama sigue fijando el spawn (`bedSetsSpawn`). Desactivable con `blockSleep`.
- **Noches vividas**: cada noche sube un poco la frecuencia de eventos (hasta +96%).
- **Nueva entidad** `stalker` (silueta de garras largas) con su aura.
- **Eventos nocturnos nuevos**: apagon de pantalla, rafaga de estatica, llamadas lejanas (criatura / sirena / tono anomalo), mensajes de chat falsos.
- **Sonidos**: se usan audios originales del proyecto (susurros, llamada de criatura, sirena, tono anomalo) + sintetizados (screamer, impacto, acufeno, cinta que se detiene). La musica de anochecer fue retirada y se trasladó a apariciones aleatorias.
- **Visual**: marcas de camara VHS de noche (REC, PLAY, contador, fecha), grano suave nocturno, temblor de camara, latido que se acelera al mirar, ceniza alrededor de las presencias.
- Comandos de prueba: `/ahc screamer|blackout|static` y `/anexohorror event blackout|static_burst|far_call|fake_chat`.

## v1.6.0
- **Asomarse despacio (solo la cabeza)**: en modo PEEK cada entidad 2D enseña solo su cabeza (recorte propio por entidad), a la altura de los ojos, y sale lentamente (4-8 s) desde detras del obstaculo. Funciona por esquinas, bloques, puertas abiertas y VENTANAS DE CRISTAL (la cabeza sube desde debajo del alfeizar). Probable con cualquier entidad.
- **Persecucion**: una presencia cualquiera congela tu mirada 5 s con "NO ESCAPARAS NUNCA MAS DE MI" y despues te persigue frenetica: rompe bloques (si mobGriefing esta activo), trepa, nada la detiene. Al alcanzarte NO hace dano: solo screamer. Config `[hunt]`: enabled, speed, breakBlocks, minNight, cooldownMinutes. Prueba: `/anexohorror event hunt`.
- **HUD VHS nocturno** (REC, PLAY, contador y fecha) y grano analogico.
- **Mas presencias**: intervalos nocturnos mas cortos, mas intentos de colocacion y reintento rapido si no hubo sitio.
- **Screamers**: si falta alguna textura se salta (ya no sale el cuadro negro y morado). Ojo: la subida web de GitHub admite 100 archivos por vez.
- La carpeta `originals/` ya no va en el paquete (para no pasar de 100 archivos).

## v1.7.0
- **El video VHS sale al inicio de cada noche** cuando `intro.mode = NIGHTFALL`. En noches posteriores tambien aparece el titulo "DIA N" en rojo con salpicaduras y gotas de sangre (opcion `nightTitle`).
- **Huevos generadores** de cada entidad (pestana creativa "AnexoHorror"): El Enmascarado, El Descarnado, El Sombrerero, El Encorvado, El Cornudo, El Gritón, El Habitante del Rincón (3D) y El Acechador. Clic derecho en un bloque: aparece, se queda ~2 min y solo se va si te acercas. Los nombres estan en `assets/anexohorror/lang/*.json`.

## v1.8.0
- **Historia en cofres**: 10 registros cronologicos del Anexo 7 (libros "Registro 01..10", fechados). Aparecen en cofres "Archivo del Anexo" a 30-70 bloques; al abrir uno, 3-8 min despues aparece el siguiente. Progreso por jugador. Config `[story] chests`.
- **El Otro** (jefe final, el modelo 3D): muy poco comun (peso 0.12, desde la noche 3, 20 min entre apariciones). Tienes 120 s para matarlo antes de que la grieta lo reclame. 350 de vida, armadura, ataca de verdad (oscuridad al golpear) y se detiene a hablar por el chat al llegar al 90/75/60/45/30/15 % de vida. Barra de jefe con vida y cuenta atras. Solo recibe dano de quien lo golpea (sin trampas). Config `[other]`. Prueba: `/anexohorror event other` o el huevo "El Otro".
- Al morir suelta **El Indice** (estrella del Nether renombrada, lore "La grieta cada vez se fractura mas") y muestra el titulo "EL OTRO HA CAIDO".
- El modelo 3D ya no sale como presencia comun.

## v1.9.0
- **Nuevo "El Otro"**: tu modelo de Blockbench (92 mallas, sentado en el suelo, sin silla) reemplaza al modelo anterior, que se elimino (obj/slender*, textura y variante `SLENDER`). Texturizado por codigo con la paleta de la foto de referencia (negro marron fibroso, cuello acanalado, cono gris-marron, cara palida con ojos oscuros), mas deformidades: relieve irregular, barriga hinchada, cabeza torcida y 22 bultos/tumores. Altura de 4 bloques (el cono incluido). LOD alto 9.3k tris, lejano 2.9k.
- **Combate**: ya no camina. Desaparece en una explosion de sangre y brillo y reaparece junto al jugador, carga 0.7 s (se inclina y vibra) y golpea; mas rapido con poca vida. Sigue hablando por el chat en las fases de dolor.
- **Aura roja** en tres capas (rojo seco, carmesi, rojo profundo), mas intensa al cargar. Respira, se mece, sacudidas bruscas y "pop" al aparecer. Destello rojo en pantalla al aparecer y al golpear.
- Huevo "El Otro" (nuevo item `egg_other`). El modelo 3D ya no es una variante comun.


## v1.10.0 — expansion analogica
- **Musica aleatoria por aparicion**: se retiró la pista programada al anochecer. Ahora, al materializarse una entidad, existe una probabilidad controlada de reproducir una de cuatro pistas nuevas, con enfriamiento de 120 segundos por jugador para evitar superposiciones.
- **Sonido de persecucion**: el audio `sonido de persecucion.mp3` se reproduce cuando el cazador pasa de la fase de inmovilizacion a la caza.
- **Sonidos adicionales**: `sonido.mp3` se integra como audio distante/ambiental y puede aparecer en eventos de acecho y en la secuencia de la mano.
- **Animacion organica de las presencias**: respiracion lenta, balanceo corporal, micro-oscilaciones y espasmos ocasionales. Se conserva la animacion de asomarse mostrando solo la cabeza.
- **Vida real para las presencias**: las entidades observadoras tienen 160 puntos de vida (80 corazones), armadura y resistencia al empuje; pueden recibir daño y solo desaparecen al morir. El Otro conserva sus 350 puntos de vida.
- **Evento de puertas**: algunas puertas cercanas se abren solas despues de golpes y crujidos. Una presencia se asoma lentamente por el costado y, en algunos encuentros, extiende la mano proporcionada. El alcance esta limitado a 7 bloques; solo alcanza al jugador si sigue dentro del radio y no se aparta del extremo.
- **Director de acecho adaptativo**: las presencias comunes aparecen desde mas lejos y con pausas mas largas (aprox. 25-60 s al principio de la noche); cada noche vivida acelera gradualmente los eventos y aumenta el peso de la persecucion especial.
- **Textos menos predecibles**: las frases de pantalla ya no aparecen con cada entidad; se seleccionan de forma aleatoria y poco frecuente. Se mantienen las frases originales del proyecto.
- **VHS corregido**: el HUD se muestra durante la noche. El servidor detecta el cruce de 13000 ticks incluso si el tiempo salta sobre ese valor o el mundo carga ya de noche. El video se envía cada noche cuando el modo del cliente es `NIGHTFALL`.
- **Sueño bloqueado**: cada intento de dormir cancela el descanso y activa un screamer aleatorio con el mensaje exacto **NO ESCAPARÁS DE ESTA REALIDAD**.
- **Recursos**: cuatro músicas, sonido de persecucion, sonido ambiental y mano recortada con transparencia a partir de los recursos proporcionados.
