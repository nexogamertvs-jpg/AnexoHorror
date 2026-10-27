# AnexoHorror 1.5.0 — Analog Psychological Expansion

**Minecraft Java 1.20.1 · Forge 47.2.0 · Java 17**

AnexoHorror es un mod de terror analógico y psicológico. Las presencias observan, aparecen de forma fugaz y desaparecen; no persiguen ni atacan al jugador. Esta versión amplía el director de eventos, añade memoria persistente y suma recursos de audio aportados para el proyecto.

## Cambios de esta versión

- **Apariciones diurnas opcionales y poco frecuentes**: por defecto están activadas, pero limitadas a una presencia por jugador durante el día.
- **Director adaptativo configurable**: ajusta los pesos de eventos según el entorno y la reacción del jugador. Se puede desactivar.
- **Memoria entre sesiones**: conserva miedo, audacia, nerviosismo, número de encuentros y progreso del archivo en los datos persistentes del jugador.
- **Expedientes anómalos**: mensajes narrativos fragmentados, con una secuencia que progresa y un tiempo mínimo entre registros.
- **Terror en refugios configurable**: los eventos de puertas y camas se pueden desactivar de forma independiente del resto de sucesos.
- **Audio adicional**: recursos proporcionados por el usuario convertidos a OGG para Minecraft; los sonidos más intensos se reproducen con volumen bajo y probabilidad reducida.
- **Mejora multijugador**: las presencias y su crujido de aparición se limitan al jugador objetivo cuando el cliente recibe el identificador de propietario sincronizado.
- **Ajuste de intensidad inicial**: intensidad 1.1, apariciones diurnas más lejanas y eventos narrativos con enfriamiento.

## Compilar

Requiere JDK 17 y acceso a internet para descargar las dependencias de Forge.

Si ya tienes Gradle:
```bash
gradle build
```

Si el repositorio contiene Gradle Wrapper:
```bash
./gradlew build
```

El JAR se genera en `build/libs/`. Para publicar, sube el contenido de la carpeta del proyecto al repositorio de GitHub y revisa el resultado de la compilación en Actions. La compilación de esta entrega debe verificarse en el entorno Forge real antes de publicar una release.

## Comandos de prueba (OP nivel 2)

- `/anexohorror spawn [0-6]` — invoca una presencia de prueba.
- `/anexohorror event <stare|peripheral|peek|door|bed|cave|fake_sound|footsteps|silence|archive>` — prueba un evento.
- `/anexohorror status` — muestra miedo, audacia, nerviosismo, encuentros, progreso del archivo y tiempo aproximado hasta el siguiente evento.

## Configuración

Después de iniciar el juego una vez, edita `config/anexohorror-common.toml`.

| Opción | Valor inicial | Función |
|---|---:|---|
| `enabled` | `true` | Activa el director de terror. |
| `intensity` | `1.1` | Frecuencia general de eventos. |
| `maxEntities` | `3` | Máximo de presencias simultáneas por jugador durante la noche. |
| `minDistanceDay` | `22` | Distancia mínima de aparición diurna. |
| `minDistanceNight` | `10` | Distancia mínima de aparición nocturna. |
| `daytimePresences` | `true` | Permite presencias diurnas excepcionales. |
| `adaptiveDirector` | `true` | Adapta el tipo de evento al entorno y a la reacción del jugador. |
| `encounterMemory` | `true` | Guarda memoria y progreso narrativo entre sesiones. |
| `fakeSounds` | `true` | Activa sonidos posicionales sin entidad. |
| `silenceEvents` | `true` | Permite eventos de silencio ambiental. |
| `escalation` | `true` | Permite una reaparición ocasional tras apartar la mirada; no hay persecución. |
| `homeAnomalies` | `true` | Activa anomalías sonoras en puertas y camas. |
| `archiveMessages` | `true` | Activa los expedientes narrativos. |
| `archiveCooldownSeconds` | `240` | Tiempo mínimo entre mensajes de archivo. |

Los efectos visuales y destellos tienen opciones independientes en `config/anexohorror-client.toml`.

## Recursos aportados

Los nuevos archivos originales se conservan en `originals/user_assets_v1.5/`. Sus versiones OGG listas para el juego están en `src/main/resources/assets/anexohorror/sounds/`. Los sonidos largos se mantienen como recursos opcionales para evitar que una pista de 40 segundos interrumpa automáticamente el juego.

## Pruebas recomendadas antes de publicar

1. Compilar con JDK 17 y Forge 47.2.0.
2. Probar un mundo nuevo de día y de noche, con `daytimePresences` activado y desactivado.
3. Confirmar que las entidades siguen inmóviles, no dañan al jugador y desaparecen correctamente.
4. Entrar con dos jugadores: cada uno debe ver sus propias presencias y no escuchar los crujidos privados del otro.
5. Desconectarse y volver a entrar para comprobar la persistencia del miedo y del archivo.
6. Verificar cada sonido nuevo, el volumen, la frecuencia y la configuración del cliente.
7. Revisar `latest.log` y probar también un servidor dedicado.

