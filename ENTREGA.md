# SportPro — Semana 12

Base de trabajo: `b4712be`; rama `feat/semana12`. Propuesta para revisión en un Pull Request a `main`, sin merge ni despliegue de reglas.

## Incremento visual y reglas

El mismo PR añade el rediseño Compose/Material 3 de `docs/REDISENO_UI.md`, sin bibliotecas nuevas. Las reglas candidatas no cambiaron. No hay cuenta autenticada en Firebase CLI en la máquina de construcción: **se entrega el fallback para Henry**, con respaldo previo, publicación exclusivamente Firestore y comprobación de una lectura real permitida/otra denegada en `docs/PUBLICAR_REGLAS_FIRESTORE.md`. No se presenta como despliegue remoto ejecutado.

Las capturas Android antes/después siguen pendientes de un dispositivo/AVD accesible; se conservan ambos APK y Previews con datos ficticios. No se fabricaron capturas. Ver límites y pruebas en el documento del rediseño.

## Qué se añadió

- Partidos: crear entre dos equipos registrados, iniciar/finalizar, catálogo editable de tipos, registro con minuto/equipo/jugador/observación y marcador calculado desde eventos vigentes. El minuto y jugador pueden quedar desconocidos. La cronología ordena por minuto, aunque el registro llegue tarde.
- Correcciones y anulaciones conservan el documento original y crean una revisión con UID del actor, fecha, estado anterior y nuevo. Las revisiones no se pueden editar ni borrar. La transacción rechaza una corrección si otro dispositivo ya cambió ese evento. Cambiar el catálogo no reescribe eventos históricos.
- Listeners Firestore para lista de partidos, marcador/cronología, catálogo y comunidad; se retiran al cerrar el flujo. Los registros nuevos pueden quedar pendientes sin conexión; la UI los señala. Las correcciones usan transacción y requieren conexión. El servidor puede rechazar un registro pendiente si su catálogo cambió mientras estaba desconectado; el error se muestra, no se declara confirmado.
- Comunidad privada por equipo: texto, comentarios, una reacción por UID, reportes privados para moderación y ocultación/restauración de publicaciones por entrenador/administrador. Sin enlaces públicos ni nuevas cargas de fotos de menores. Se advierte no publicar datos de contacto, salud o ubicaciones. El texto lo modera una persona; no hay detector automático de datos sensibles.
- Resumen IA v1 con Retrofit: envía únicamente estado/marcador y eventos vigentes registrados; excluye anulados. El entrenador genera, edita y aprueba; los lectores no reciben el borrador. Hash de eventos y versión del partido detectan cambios antes de aprobar; una nueva revisión deja obsoleto el resumen anterior y la UI no lo presenta como actual. El servidor rechaza aprobar con una versión vieja. El documento publicado previo se conserva para trazabilidad; no se borra del servidor al cambiar el partido.
- Reglas Firestore nuevas, pruebas unitarias y pruebas reales del servidor emulado. No se añadieron bibliotecas de aplicación fuera de Compose/Material3/Navigation, Firebase y Retrofit ya contempladas en el curso.

## Compilar

JDK 17, Android SDK 35 y Build Tools 35.0.0. Abrir en Android Studio, poner la configuración local Firebase en `app/google-services.json` y el SDK en `local.properties` (ambos ignorados por Git).

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest --max-workers=1
npm --prefix tests/firestore ci --ignore-scripts --no-audit --no-fund
npm --prefix tests/firestore test
```

APK generado en `app/build/outputs/apk/debug/app-debug.apk`. En Windows usar `gradlew.bat`. La construcción de esta entrega se hizo en ARM64 con JDK/SDK locales y AAPT2 x86 emulado; ese adaptador sólo pertenece al entorno de construcción y no al APK ni al repositorio.

El archivo Firebase local estaba versionado en la base pese al `.gitignore`. Se retira del índice sin borrarlo de la copia local. Su contenido histórico ya existente en Git no se elimina ni se afirma revocado. El ejemplo anterior era de cuenta de servicio: se reemplazó por una plantilla Android sin credencial privada. Nunca colocar una clave privada de Admin SDK en la app.

## Configuración IA (pendiente de elegir proveedor)

Copiar los campos de `local.properties.example` al archivo local y completar `SUMMARY_BASE_URL`, `SUMMARY_API_KEY` y `SUMMARY_MODEL`. Contrato compatible con `POST chat/completions`, mensajes system/user y respuesta `choices[0].message.content`, vía HTTPS. No hay proveedor, modelo ni clave por defecto. Sin configuración la pantalla lo informa y no inventa un resumen.

La clave queda fuera de Git, siguiendo el patrón de clase. **BuildConfig no oculta una clave dentro de un APK distribuido**: para una publicación real se necesita un servicio propio que custodie la clave. No compartir un APK con una clave de producción. La solicitud puede tener costo según el proveedor; no se realizó ninguna llamada facturable.

La prueba Retrofit usa un receptor HTTP local simulado y texto ficticio. Demuestra serialización/respuesta, no calidad factual de un modelo. La instrucción al modelo no garantiza ausencia de errores: la revisión y aprobación del entrenador son obligatorias.

## Correcciones de construcción heredadas

Android Lint detectó cuatro errores ya presentes en la base: `shared_pref` en vez de `sharedpref`, elementos de configuración de red dentro del XML de extracción de datos y hardware de cámara implícitamente obligatorio. Se corrigió el esquema XML conservando las categorías de respaldo existentes y se declaró cámara opcional. No se deshabilitó Lint ni se creó un baseline para ocultar errores.

## Validación y límites

Ver `docs/SEMANA12_PRUEBAS.md` para resultados medidos y `docs/SEMANA12_DEMO.md` para aceptación y video. La compilación y las reglas locales no acreditan una prueba visual Android, dos teléfonos reales ni un partido real. Esas pruebas siguen pendientes de Henry. El video solicitado por el enunciado todavía no está grabado.

No se modificaron cuentas, datos ni reglas del proyecto Firebase remoto. Antes de probar con datos reales, Henry debe revisar y publicar las reglas mediante su proceso autorizado: instalar el APK por sí solo no instala reglas. El registro público de ENTRENADOR es política heredada y conserva permisos staff; la comunidad no acredita por sí sola la identidad profesional de quien se registra con ese rol.
