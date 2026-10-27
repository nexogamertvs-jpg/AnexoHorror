# Validación de AnexoHorror 1.5.0

## Comprobaciones realizadas en esta entrega

- Se abrió y extrajo el archivo RAR de recursos aportado.
- Se conservaron los originales aportados en `originals/user_assets_v1.5/`.
- Se convirtieron seis recursos de audio a OGG para incluirlos como recursos del mod.
- Se validó el JSON de `sounds.json` y se comprobó que todas las referencias de audio apuntan a archivos existentes.
- Se revisó de forma estática el balance de llaves y paréntesis de los archivos Java modificados.
- Se actualizó la versión del proyecto a `1.5.0`.

## Pendiente antes de publicar una release

Este entorno no tiene Gradle instalado y no tiene acceso de red para descargar las dependencias de Forge. Por ello, **no se pudo ejecutar `gradle build` ni probar el mod dentro de Minecraft**. La compilación real, la prueba de sincronización multijugador y el ajuste de volúmenes deben comprobarse con JDK 17 y Forge 47.2.0 antes de marcar esta versión como estable.

## Pruebas manuales recomendadas

1. Compilar el proyecto y revisar cualquier error que reporte Gradle.
2. Entrar a un mundo de día y comprobar que las apariciones diurnas son raras.
3. Probar eventos desde `/anexohorror event archive` y comprobar el enfriamiento del archivo.
4. Desconectar y reconectar para verificar la memoria persistente.
5. En multijugador, comprobar que cada jugador solo ve las presencias que le corresponden.
6. Revisar `latest.log` después de probar todos los sonidos y opciones de configuración.
