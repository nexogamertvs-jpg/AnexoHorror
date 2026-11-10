# AnexoHorror 1.10.5 — validación del paquete fuente

## Cambios preparados

- Modo PEEK 2D actualizado para dibujar solo el recorte facial individual de cada una de las siete presencias comunes, con inclinación y giro sutiles.
- Búsqueda de bloques sólidos a diferentes alturas y ángulos; comprueba visibilidad parcial y utiliza entrada aleatoria desde izquierda, derecha, arriba o abajo.
- PEEK aleatorio poco frecuente durante el día, además de los eventos nocturnos. El comando `/anexohorror event peek` usa la nueva ruta.
- Pantalla CreditsScreen en cada conexión al mundo/servidor, con cara de EYE, versión, créditos a NexoGamerTvs y enlace de YouTube.
- Las seis pistas clasificadas como música (`entity_music_1..4`, `intro`, `dark_tape`) están limitadas a 29,95 s. Los efectos de sonido se conservan completos.

## Validaciones estáticas

- JSON del mod parseados.
- Texturas faciales de las siete entidades y texturas emisivas presentes.
- Textura `credits_face.png` creada en PNG RGBA de 112x112.
- Duración de las seis pistas musicales comprobada con ffprobe y menor o igual a 30 s.
- Los efectos de sonido no musicales conservan sus bytes originales respecto a 1.10.2.
- Se revisaron las referencias de clases, recursos, eventos PEEK y comandos.
- Se ejecutó una comprobación sintáctica de Java; no se dispone de Gradle ni de las dependencias Forge 1.20.1 en este entorno, por lo que no se certifica compilación ni ejecución dentro del juego.
- ZIP final sometido a prueba de integridad.

## Pruebas pendientes en Minecraft Forge 1.20.1

1. Entrar a un mundo y revisar la pantalla de créditos, botón de canal y botón CONTINUAR.
2. Ejecutar `/anexohorror event peek` cerca de bloques, paredes, pilares y desniveles; repetirlo para comprobar las cuatro direcciones y las siete caras.
3. Probar PEEK tanto de día como de noche; durante el día debe ser poco frecuente y no acumular más de una presencia común.
4. Confirmar que las entidades no muestran el torso durante PEEK y que la inclinación de la cabeza es sutil.
5. Comprobar la longitud de las pistas durante el juego y que persecución, respiración, crujidos y sonido distante siguen completos.
6. Ejecutar `gradle --no-daemon build` con Java 17 / ForgeGradle y probar el `.jar` en una instancia real.

**Estado:** fuente actualizada y validaciones estáticas realizadas. No se ha generado ni certificado un `.jar` final; el comportamiento visual debe confirmarse dentro del juego.


## 1.10.4 — nuevos eventos

- Se incorporan 12 tipos de eventos: `wall_taps`, `door_rattle`, `glass_scratch`, `ceiling_steps`, `tape_corruption`, `double_peek`, `distant_stare`, `breath_behind`, `approaching_steps`, `false_block_break`, `second_presence` y `voice_echo`.
- Todos se exponen en las sugerencias de `/anexohorror event <nombre>` y tienen rutas de ejecución directas para pruebas.
- Los sonidos son posicionales y reutilizan audio ya incluido; ningún evento rompe bloques, cambia estados de puertas o inflige daño.
- Las probabilidades son bajas/moderadas y quedan sujetas al historial de eventos recientes.
- Pendiente de compilación con ForgeGradle y pruebas dentro de Minecraft: el entorno de preparación no dispone de Gradle ni de dependencias Forge.


## 1.10.5 — rotación justa y control de entidades

- Reemplazada la selección de pesos que favorecía repetidamente a los eventos comunes por una baraja por jugador con todos los valores de `EventType`. La baraja se mezcla y retira un evento cuando logra ejecutarse; los que no son válidos se posponen al final para reintentarse.
- Se conservan los bloqueos intencionales de `HUNT` y `OTHER` (noche mínima, configuración, dificultad y cooldown).
- Añadidas alternativas ambientales para `STARE`, `PERIPHERAL`, `PEEK`, `BED`, `CAVE`, `WINDOW`, `FOOTSTEPS`, `DISTANT_STARE`, `DOUBLE_PEEK` y `SECOND_PRESENCE` si no hay una ubicación o cupo de entidad adecuado.
- El límite efectivo de presencias comunes queda en dos por jugador de noche y una de día; `maxEntities` se limita a 1–2 también en instalaciones con TOML antiguo.
- Los intervalos de eventos se ajustan para mantener pausas sin reintentos excesivos. El comando `/anexohorror status` informa el próximo evento y los eventos pendientes.

## Validación de la entrega 1.10.5

- 28 valores de `EventType` comprobados y los 28 tienen una rama correspondiente en `HorrorEvents.run`.
- 12 archivos JSON parseados correctamente.
- La búsqueda global confirma que el límite configurado de entidades se consulta a través de `entityCap()` y queda limitado a 2.
- La comprobación de sintaxis de `javac` no mostró errores de parser; la compilación completa se detiene por la ausencia de los paquetes Minecraft/Forge.
- Gradle no está instalado en este entorno. La compilación con Gradle/Forge 1.20.1 y una prueba de varias rondas dentro del juego siguen pendientes.
- El ZIP final se somete a prueba de integridad antes de entregarse.

## 1.10.6 — tanda 01 (01–10)

- Analizados los 43 archivos Java con el parser de javac de JDK 17, sin errores sintácticos. Esta comprobación no resuelve tipos de Minecraft/Forge.
- Todos los JSON de recursos parseados correctamente y `git diff --check` sin incidencias.
- Versiones del código, Gradle y nombre del artefacto de CI sincronizadas en 1.10.6.
- Matriz de diez pruebas funcionales en `MEJORAS.md`; pendiente de ejecución dentro del juego, incluyendo dos jugadores, cambios de dimensión y fin de caza.

Compilación final: **BUILD SUCCESSFUL**, Gradle 8.1.1, JDK 17, Forge 47.2.0. Generado `anexohorror-1.10.6.jar`. Sin tests automatizados en el proyecto (`test NO-SOURCE`); pruebas dentro de Minecraft pendientes.
