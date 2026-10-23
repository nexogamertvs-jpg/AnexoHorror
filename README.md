# AnexoHorror (Forge 1.20.1 / 47.2.0)

Mod de terror analógico y psicológico. Seis presencias (una por imagen proporcionada) que observan,
nunca persiguen ni atacan.

## Compilar
Requiere JDK 17 y conexión a internet (descarga Forge/Minecraft).

    gradle wrapper --gradle-version 8.1.1     # una sola vez (o copia gradlew + gradle/ del MDK 1.20.1-47.x)
    ./gradlew build                           # jar en build/libs/anexohorror-1.0.0.jar

Alternativa sin instalar nada: sube la carpeta a GitHub; `.github/workflows/build.yml` compila el jar.
Para probar: `./gradlew runClient`.

## Comandos de prueba (OP nivel 2)
- `/anexohorror spawn [0-5]`  presencia delante de ti (0 peeker, 1 grin, 2 smiler, 3 drawn, 4 crawler, 5 eye)
- `/anexohorror event <stare|peripheral|peek|door|bed|cave|fake_sound|footsteps|silence>`
- `/anexohorror status`       miedo / audacia / nerviosismo aprendidos del jugador

## Configuración
`config/anexohorror-common.toml`: intensidad, distancias mínimas, máximo de entidades, sonidos falsos,
silencios y escalada nocturna.

## Archivos originales
`originals/` conserva tus PNG y el MP3. En el mod: texturas con fondo transparente
(`textures/entity/*.png`) y `crack.ogg` (tu MP3 convertido a OGG mono, requerido por Minecraft para sonido posicional).
