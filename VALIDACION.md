# Validacion de AnexoHorror 1.10.0

## Cambios incluidos
- La musica deja de ejecutarse por el cambio de noche; cuatro pistas nuevas se seleccionan al azar al materializarse una entidad, con enfriamiento de 120 segundos por jugador.
- El VHS se dispara al cruzar el tick 13000, al cargar un mundo que ya esta de noche y al entrar un jugador durante la noche (una vez por noche y jugador, en modo `NIGHTFALL`).
- El HUD VHS solo se muestra de noche y la cola del video ya no expira mientras el cliente tiene otra pantalla abierta.
- Las frases de pantalla aparecen de forma aleatoria y menos frecuente.
- Las presencias 2D reciben 160 puntos de vida y pueden recibir daño; `El Otro` conserva sus 350 puntos.
- El director se ajustó para mantener a las presencias comunes a mayor distancia y con intervalos iniciales de unos 25-60 segundos; la frecuencia y la probabilidad de caza aumentan gradualmente con las noches.
- La animacion de las presencias incluye respiracion, balanceo, micro-movimiento y espasmos breves.
- El evento de puertas puede abrir una puerta, revelar una cabeza desde el lateral y, en algunos casos, extender la mano hasta un maximo de 7 bloques. Solo mata si el jugador sigue al alcance y no se aparta del extremo.
- El sonido de persecucion se reproduce al iniciar la fase de caza.
- Se integran cuatro pistas musicales, un sonido ambiental, el grito de persecucion y la mano del paquete adicional.
- Cada intento de dormir cancela el descanso y dispara un screamer aleatorio con el texto solicitado.

## Comprobaciones realizadas
- RAR del proyecto 1.9.0 extraido: 117 archivos.
- Paquete de recursos extraido: 7 archivos.
- Los seis audios nuevos se convirtieron a OGG Vorbis.
- `sounds.json` se valido como JSON y sus 25 eventos tienen archivos de audio presentes.
- La textura de la mano se convirtio a PNG RGBA con el fondo blanco eliminado.
- Se comprobaron balance de llaves y parentesis en las clases Java modificadas.
- `javac` no reporto errores de sintaxis, pero no pudo compilar las clases porque este entorno no contiene las dependencias de Minecraft/Forge.

## Compilacion pendiente
No se pudo ejecutar `gradle build` en este entorno: no hay Gradle instalado ni cache local de Forge/Minecraft. Se agrego `.github/workflows/build.yml` para que GitHub Actions ejecute la compilacion con Java 17 y Gradle 8.1.1 al subir el proyecto.

**No afirmar que el mod esta compilado o probado dentro de Minecraft hasta que la ejecucion `Build AnexoHorror` de GitHub Actions termine correctamente y se pruebe el JAR en Forge 1.20.1 / 47.2.0.**

## Configuracion heredada
Los archivos `anexohorror-common.toml` que ya existan pueden conservar sus valores anteriores. La logica aplica un minimo de acecho, pero para usar los valores completos recomendados revisa `minDistanceDay = 24` y `minDistanceNight = 18`.

## Pruebas dentro del juego
1. ` /time set 12000 ` y luego ` /time set 13000 `: confirmar HUD y video VHS en modo `NIGHTFALL`.
2. Entrar al mundo directamente de noche: confirmar que el video no se pierde durante la carga.
3. ` /anexohorror spawn ` varias veces: comprobar que la musica solo aparece a veces y no se reproduce por el simple cambio de hora.
4. ` /anexohorror event door ` junto a una puerta: comprobar golpes, apertura, cabeza asomandose y mano en los encuentros que la seleccionen.
5. ` /anexohorror event hunt `: comprobar el grito al comenzar la persecucion.
6. Atacar una presencia con armas: comprobar que resiste mas que un golem de hierro y que puede morir tras recibir suficiente daño.
7. Intentar dormir varias veces: comprobar que cada intento cancela el sueño y muestra el screamer con `NO ESCAPARÁS DE ESTA REALIDAD`.
8. Probar con dos jugadores y comprobar que la musica, los screamers y las apariciones dirigidas a un jugador no se mezclan entre clientes.
