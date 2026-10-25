# AnexoHorror (Forge 1.20.1 / 47.2.0)

Mod de terror analógico y psicológico. Siete presencias (seis planos con tus imagenes + un modelo 3D .obj) que observan,
nunca persiguen ni atacan.

## Compilar
Requiere JDK 17 y conexión a internet (descarga Forge/Minecraft).

    gradle wrapper --gradle-version 8.1.1     # una sola vez (o copia gradlew + gradle/ del MDK 1.20.1-47.x)
    ./gradlew build                           # jar en build/libs/anexohorror-1.1.1.jar

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
