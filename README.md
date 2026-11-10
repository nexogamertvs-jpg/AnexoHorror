# AnexoHorror (Forge 1.20.1 / 47.2.0)

Mod de terror analógico y psicológico para Forge 1.20.1. Las presencias usan el renderizador normal de Minecraft: no necesitan shaders. El efecto luminoso se limita a los ojos; no existe aura exterior.

## Compilar
Requiere JDK 17 y conexión a internet (descarga Forge/Minecraft).

    gradle wrapper --gradle-version 8.1.1     # una sola vez (o copia gradlew + gradle/ del MDK 1.20.1-47.x)
    ./gradlew build                           # jar en build/libs/

Alternativa sin instalar nada: sube la carpeta a GitHub; `.github/workflows/build.yml` compila el jar.
Para probar: `./gradlew runClient`.

## Comandos de prueba (OP nivel 2)
- `/anexohorror spawn [0-5]`  presencia delante de ti (0 peeker, 1 grin, 2 smiler, 3 drawn, 4 crawler, 5 eye, 6 slender 3D)
- `/anexohorror event <stare|peripheral|peek|door|bed|cave|fake_sound|footsteps|silence|window|hunt|other|blackout|static_burst|far_call|fake_chat|wall_taps|door_rattle|glass_scratch|ceiling_steps|tape_corruption|double_peek|distant_stare|breath_behind|approaching_steps|false_block_break|second_presence|voice_echo>`
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
- **Animacion organica de las presencias**: respiracion lenta, balanceo corporal, micro-oscilaciones y espasmos ocasionales. La animacion de asomarse ahora mantiene la silueta completa y utiliza el bloque para ocultar torso y parte del cuerpo.
- **Vida real para las presencias**: las entidades observadoras tienen 160 puntos de vida (80 corazones), armadura y resistencia al empuje; pueden recibir daño y solo desaparecen al morir. El Otro conserva sus 350 puntos de vida.
- **Evento de puertas**: algunas puertas cercanas se abren solas despues de golpes y crujidos. Una presencia se asoma lentamente por el costado y, en algunos encuentros, extiende la mano proporcionada. El alcance esta limitado a 7 bloques; solo alcanza al jugador si sigue dentro del radio y no se aparta del extremo.
- **Director de acecho adaptativo**: las presencias comunes aparecen desde mas lejos y con pausas mas largas (aprox. 25-60 s al principio de la noche); cada noche vivida acelera gradualmente los eventos y aumenta el peso de la persecucion especial.
- **Textos menos predecibles**: las frases de pantalla ya no aparecen con cada entidad; se seleccionan de forma aleatoria y poco frecuente. Se mantienen las frases originales del proyecto.
- **VHS corregido**: el HUD se muestra durante la noche. El servidor detecta el cruce de 13000 ticks incluso si el tiempo salta sobre ese valor o el mundo carga ya de noche. El video se envía cada noche cuando el modo del cliente es `NIGHTFALL`.
- **Sueño bloqueado**: cada intento de dormir cancela el descanso y activa un screamer aleatorio con el mensaje exacto **NO ESCAPARÁS DE ESTA REALIDAD**.
- **Recursos**: cuatro músicas, sonido de persecucion, sonido ambiental y mano recortada con transparencia a partir de los recursos proporcionados.


## 1.10.1 — Ajustes de acecho y animaciones

- Las apariciones `PEEK` siempre dibujan la silueta completa: el mundo oculta el torso detrás del bloque mientras la figura entra desde un lateral. La inclinación pivota desde la zona de los hombros, no recorta ni separa la cabeza.
- El brazo se renderiza en el plano de la cámara, con la geometría orientada hacia el jugador y una extensión de aparición más visible. El alcance real de la mano sigue limitado a 7 bloques.
- Los comandos `/anexohorror event <nombre>` usan rutas deterministas: `stare`, `peripheral`, `peek` y `hunt` generan su evento directamente; `door` busca la puerta más cercana y, si no encuentra una, usa una esquina de respaldo. `bed`, `cave` y `window` prueban el decorado correspondiente y recurren a un asomo visible si no existe.
- Inspiración mecánica independiente en *The Stalker Horror* y *LCrandom's Stalker 3D*: acecho por etapas, persecución normal que puede perder el rastro, variantes agresivas que insisten más, y encuentros situacionales desde lejos. No se reutilizan sus archivos, modelos ni código.
- La agresividad de la caza aumenta gradualmente según las noches sobrevividas. La caza normal puede terminar si el jugador se aleja y mantiene la línea de visión rota; las variantes agresivas persisten más tiempo.


## 1.10.2 — identidad analógica y renderizado independiente de shaders

- **Sin aura exterior:** eliminadas las capas que iluminaban la silueta entera, las tres capas rojas de El Otro, el halo del brazo y las partículas de ceniza que flotaban alrededor de las presencias comunes. También se retiraron las texturas antiguas de aura.
- **Oscuridad real:** el cuerpo ya no recibe un nivel mínimo de luz artificial; respeta la iluminación del mundo. Solo los ojos utilizan luz emisiva completa.
- **Ojos emisivos solamente:** cada una de las siete presencias 2D tiene una textura overlay del mismo tamaño que su imagen, con puntos de luz ajustados a sus ojos y una paleta propia: rojo, carmesí, ámbar, cian, verde tóxico, verde pálido o blanco frío. El Otro usa un overlay aparte sobre las UV de su rostro.
- **Sin shaders obligatorios:** el brillo se dibuja con `RenderType.eyes` y luz completa del renderizador de Minecraft. No se requiere Oculus, Iris, OptiFine ni un shader pack para ver los ojos. Los shaders pueden seguir usándose por preferencia, pero ya no controlan este efecto.
- **Interferencia analógica durante toda la partida:** el HUD de cámara ahora puede verse de día y de noche (`CAM-04`, `REC`, `PLAY`, contador SP, día/hora y estado de señal). De día la capa es discreta; de noche aparecen más grano, tracking y degradación de señal.
- **Fallos de cinta ocasionales:** añadidas bandas horizontales de tracking, pequeñas separaciones cromáticas y ráfagas breves de estática nocturna, con pausas largas para que no tapen el juego ni saturen el sonido.
- **STALKER con atención selectiva:** se queda inmóvil mientras el jugador lo mira directamente. Cuando el jugador aparta la vista, avanza muy lentamente en pequeños pasos, sin pasos audibles y sin atravesar colisiones de bloques. Esta variante tiene una permanencia más larga que las demás.
- **Sin cambiar la identidad de las otras presencias:** las demás siguen usando su comportamiento anterior; la mecánica de acercamiento por mirada pertenece solo a STALKER.



## 1.10.3 — asomos de cabeza y créditos del creador

- **Asomos de cabeza para las siete presencias 2D:** PEEK deja de mostrar la silueta completa. Cada entidad utiliza su propia región facial UV y sus propios ojos emisivos; el cuerpo no se dibuja durante este encuentro.
- **Entradas desde distintas direcciones:** cada aparición puede deslizar el rostro desde la izquierda, derecha, arriba o abajo. La cabeza tiene un giro de profundidad y una inclinación sutil, con una pequeña animación de respiración.
- **Bloques como cobertura:** el director busca bloques sólidos en distintas alturas y alrededor del jugador; coloca el rostro junto a un canto, comprobando que el bloque oculte parte de la cara y que otra parte siga visible. Si no encuentra uno, utiliza la búsqueda de esquinas habitual.
- **Asomos diurnos raros:** PEEK puede ocurrir durante el día con menos peso y un máximo de una presencia activa; de noche tiene más posibilidades y respeta el límite normal de entidades.
- **Comando de prueba:** `/anexohorror event peek` fuerza la nueva búsqueda de bloques y el asomo de cabeza. También se conserva el comportamiento visual de cabeza para puertas, ventanas, camas y cuevas que usen el modo PEEK.
- **Música limitada:** las cuatro pistas aleatorias de aparición, `intro.ogg` y `dark_tape.ogg` quedan en un máximo técnico inferior a 30 segundos. Los efectos (`hunt_scream`, `distant_sound`, voces, golpes, respiración, etc.) conservan sus duraciones completas. El audio de la intro se ha limitado a 29,95 s; el video no se ha recortado.
- **Pantalla de bienvenida:** al entrar a un mundo o servidor aparece una tarjeta analógica de créditos con el rostro de EYE, el nombre ANEXOHORROR, la versión, Minecraft Forge 1.20.1, el crédito **Creado por NexoGamerTvs** y un botón que abre [el canal de YouTube](https://www.youtube.com/@NexoGamer-t5h). Se cierra con CONTINUAR o ESC. Después, el video VHS de entrada/anochecer conserva su flujo anterior.
- **Alcance:** el nuevo PEEK cubre las siete presencias 2D comunes (PEEKER, GRIN, SMILER, DRAWN, CRAWLER, EYE y STALKER). **El Otro** permanece como jefe 3D especial con su propio sistema de encuentros, sin convertir sus apariciones de jefe en eventos PEEK normales.


## 1.10.4 — repertorio ampliado de eventos analógicos

Se añaden 12 eventos nuevos, integrados con el director adaptativo y los comandos `/anexohorror event <nombre>`:

- `wall_taps`: golpes secos y arañazos que parecen venir de un bloque concreto.
- `door_rattle`: manija/puerta que cruje y golpea sin modificar ni desbloquear la puerta.
- `glass_scratch`: arañazos sobre un cristal; ocasionalmente puede continuar con un rostro tras la ventana.
- `ceiling_steps`: pasos y roces sobre el techo, con sonido posicional.
- `tape_corruption`: caída breve de señal, corte de ambiente, tinnitus y una voz retardada.
- `double_peek`: dos asomos de cabeza en lugares distintos, separados por varios segundos.
- `distant_stare`: una presencia pequeña y muy lejana; si no hay lugar válido, queda una llamada distante.
- `breath_behind`: respiración muy cercana a la espalda y, a veces, un susurro posterior.
- `approaching_steps`: pisadas que avanzan desde atrás y se detienen cerca, sin entidad ni daño.
- `false_block_break`: sonido de bloque rompiéndose sin destruir ni cambiar ningún bloque.
- `second_presence`: si el jugador llega a observar el primer rostro, puede aparecer otro desde un punto diferente.
- `voice_echo`: una voz de cinta distante parece repetirse desde detrás del jugador.

Los eventos nuevos reutilizan los recursos de audio existentes, no añaden dependencias ni shaders. Sus pesos son moderados y el historial de eventos recientes reduce repeticiones. Ninguno de estos sucesos destruye bloques, desbloquea puertas, aplica daño ni obliga a iniciar una persecución.


## 1.10.5 — rotación justa de eventos y límite de presencias

- **Baraja aleatoria por jugador:** los 28 tipos de eventos se mezclan en una ronda. Cada evento que se ejecuta sale temporalmente de la baraja; no se repite hasta que los demás hayan tenido su oportunidad. El orden se vuelve a mezclar al terminar la ronda.
- **Reintentos sin bloqueo:** si un evento no puede encontrar una puerta, una cama, cristal, un hueco de aparición o espacio para otra entidad, no se pierde; se pospone para una siguiente oportunidad. Los eventos situacionales tienen alternativas de audio/ambiente para que la experiencia no se quede sin sucesos.
- **Excepciones de progresión:** `hunt` sigue esperando la noche mínima y su enfriamiento; `other` conserva su requisito de noche, dificultad y enfriamiento de jefe. Permanecen en espera hasta que cumplan sus condiciones, en lugar de desaparecer de la rotación.
- **Menos entidades simultáneas:** el límite efectivo queda en un máximo de dos presencias comunes por jugador de noche y una de día. Incluso instalaciones que conservaran `maxEntities = 3` quedan limitadas a dos por código. Los eventos `double_peek` y `second_presence` pueden convertirse en un segundo indicio sonoro cuando ya se alcanzó el límite.
- **Ritmo reajustado:** los intervalos se estabilizan aproximadamente entre 70 y 145 segundos de día y 35–65 segundos de noche (con variaciones de tensión y progresión). Se elimina el reintento cada seis segundos cuando no se encuentra un evento válido.
- **Estado ampliado:** `/anexohorror status` muestra el siguiente evento de la rotación y cuántos quedan pendientes.

El sistema es por jugador y usa un orden aleatorio sin repetir eventos comunes hasta que la ronda haya avanzado. Los eventos `HUNT` y `OTHER` son especiales y siguen sujetos a sus requisitos de progresión/cooldown.

## Cámara y almanaque de entidades

- Nuevo ítem `anexohorror:camera` (pestaña de creativo AnexoHorror). Clic derecho: foto con flash (enfriamiento de 1,5 s). Agachado + clic derecho: abre el almanaque.
- Si una presencia o El Otro está dentro del encuadre (~28°, hasta 48 bloques, con línea de visión) la foto la registra; el almanaque muestra todas las entradas bloqueadas hasta ese momento.
- El registro se guarda en los datos persistentes del jugador (`PlayerPersisted` → `anexohorror_almanac`, máscara de bits) y se conserva al morir.
- Visor analógico (esquinas, REC, estado del flash) mientras la cámara está en la mano, en primera persona.
- Archivos nuevos: `almanac/AlmanacEntry`, `almanac/Almanac`, `item/CameraItem`, `client/CameraFlash`, `client/AlmanacScreen`; paquetes reutilizan `ClientEffectPacket` (kinds 8 y 9). Texturas en `textures/item/camera.png` y `textures/gui/almanac/`.
- Pendiente: compilar con ForgeGradle y probar en el juego.

## Cámara en supervivencia

La cámara se fabrica en una mesa con tres lingotes de hierro (arriba, izquierda y derecha del cristal), un panel de cristal central y una redstone debajo del cristal. Patrón exacto:

```text
 I
IGI
 R
```

I = hierro, G = panel de cristal, R = redstone. La receta se descubre al recoger redstone. Clic derecho toma la fotografía; agachado + clic derecho abre el almanaque. Las fotografías requieren una línea de visión sin colisiones y el servidor comprueba el enfriamiento de 1,5 segundos.

## 1.10.6 — mejoras 01–10: ritmo y continuidad del acecho

El director reserva los sucesos intensos hasta la primera noche, separa los picos de tensión, da margen tras una caza y espera durante los encuentros activos. Cancela acciones pendientes al morir, cambiar de dimensión o desactivarse; evita repetir inmediatamente el último evento y alarga los silencios cuando el miedo aumenta. `/anexohorror status` muestra los segundos de recuperación. Detalles y pruebas de cada mejora en `MEJORAS.md`.
