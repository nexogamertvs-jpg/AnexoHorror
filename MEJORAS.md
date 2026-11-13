# Plan de 100 mejoras — tanda 01 (01–10)

Tema: terror analógico y psicológico; acecho, incertidumbre, silencio y escalada nocturna.
Estas diez mejoras se aplican al director automático. Los comandos de prueba siguen permitiendo forzar eventos.

| N.º | Mejora implementada | Comprobación en Forge |
| --- | --- | --- |
| 01 | Al desactivar el director se cancelan acciones pendientes; cama y despertar respetan ENABLED. | Programar un evento retardado, desactivar y verificar que no se ejecuta. |
| 02 | Muerte, espectador y cambio de dimensión cancelan secuencias pendientes, silencio e inactividad acumulada. | Salir al Nether durante una secuencia y volver; no debe reproducirse fuera de contexto. |
| 03 | Regresar al mundo, reaparecer o reactivar el director concede al menos 30 s de margen. | Reaparecer y comprobar que no hay evento automático inmediato. |
| 04 | Un evento automático no puede repetirse inmediatamente, incluso entre rondas. | Observar varias rondas con `/anexohorror status`. |
| 05 | Antes de la primera noche se reservan persecución, jefe, apagón, estática y corrupción para más adelante. | Entrar a un mundo nuevo de día; comprobar inicio ambiental. |
| 06 | Los cinco eventos intensos comparten una separación mínima de 90 s. | Revisar que dos eventos intensos automáticos no se encadenen antes del límite. |
| 07 | Un evento intenso deja 30 s de recuperación, además del intervalo normal. | Comprobar recuperación en status después del evento. |
| 08 | Terminar una caza concede al menos 60 s de recuperación y eleva el miedo. | Terminar una persecución y comprobar status. |
| 09 | El miedo alarga el intervalo hasta un 35%; las pausas cortas excepcionales requieren más de una noche y miedo bajo, y duran al menos 25 s. | Comparar intervalos con distintos niveles de miedo y noches. |
| 10 | Los eventos automáticos esperan durante una caza propia o un encuentro cercano con El Otro; se revisa cada 5 s. | Iniciar una caza o jefe y comprobar que no se acumulan otros eventos automáticos. |

`/anexohorror status` añade `recuperacion`, en segundos. Las cadenas ya programadas de un encuentro siguen formando parte de ese encuentro; la pausa regula la selección automática de nuevos eventos.

La receta y ajustes de la cámara preparados antes de esta tanda no cuentan dentro de las diez mejoras.

## Validación

- Diff sin errores de espacios y JSON de recursos válidos.
- Compilación con Java 17 y Gradle 8.1.1: ver resultado registrado al final de esta sección.
- Las pruebas de la tabla requieren Minecraft Forge 1.20.1; no se han realizado en el juego.

Compilación final: **BUILD SUCCESSFUL**, Gradle 8.1.1, JDK 17, Forge 47.2.0. Generado `anexohorror-1.10.6.jar`. Sin tests automatizados en el proyecto (`test NO-SOURCE`); pruebas dentro de Minecraft pendientes.
