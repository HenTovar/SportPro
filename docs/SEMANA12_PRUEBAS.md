# Resultados medidos — Semana 12

Validación local del 6 de noviembre de 2026 (UTC: 7 de noviembre).

| Prueba | Resultado | Qué acredita |
|---|---|---|
| Android assembleDebug | APK generado | Compila código Compose, navegación, repositorios y Retrofit |
| testDebugUnitTest | 90 ejecutadas, 0 fallos, 0 errores, 0 omitidas | Suite heredada + 5 casos de hechos/cronología/hash y 2 de Retrofit/respuesta |
| Firestore Emulator | 10 agrupadas, 10 PASS, 0 FAIL | Reglas reales locales, incluidos listeners, offline, auditoría, borrador y comunidad |
| Android Lint | 0 errores (advertencias heredadas conservadas) | XML corregido y cámara opcional; no se ocultaron errores con baseline |
| Proveedor IA | Simulado HTTP local | Serializa los hechos y lee una respuesta compatible; no acredita un modelo real |
| Android UI en dispositivo | PENDIENTE | Compilar no acredita renderizado, interacción ni dos teléfonos |
| Partido/video real continuo | PENDIENTE | Requiere grabación de Henry, ver guía de demo |
| Reglas en Firebase remoto | No desplegadas | Los resultados corresponden al archivo local |

## Casos nuevos

- Marcador excluye anulaciones, cambia al corregir equipo, minuto desconocido válido y cronología por minuto aunque llegue tarde.
- Hash independiente del orden de lectura; cambia con revisión, equipo, jugador, observación o anulación. Minutos fuera de rango y equipos ajenos se rechazan.
- Retrofit realiza una llamada HTTP a un receptor local ficticio: comprueba ruta, cabecera con credencial falsa, contenido JSON y respuesta. Se excluyen anulados y se conservan desconocidos. Respuesta ausente, vacía o excesiva falla sin fabricar texto.
- Dos clientes Firestore: uno registra/corrige/anula y el listener del otro recibe cada revisión. Historial de tres entradas inmutable; borrar, falsear actor, omitir auditoría, equipo ajeno y minuto inválido se rechazan.
- Evento nuevo con red deshabilitada queda pendiente; al reconectar se confirma y otro usuario lo lee. Esto prueba el SDK/emulador, no modo avión de Android.
- Borrador sólo para staff; jugador no puede leerlo ni aprobar. Una aprobación con versión anterior al cambio de estado se rechaza.
- Comunidad: equipo ajeno, autor falso, reacción bajo UID ajeno y moderación por jugador se rechazan; reportes privados, ocultación por staff y bloqueo de comentarios sobre publicación oculta.

## Reproducir

JDK 17 y SDK35 configurados localmente:

```sh
./gradlew --no-daemon --max-workers=1 :app:assembleDebug :app:testDebugUnitTest
npm --prefix tests/firestore ci --ignore-scripts --no-audit --no-fund
npm --prefix tests/firestore test
```

Los reportes JUnit quedan en `app/build/test-results/testDebugUnitTest/`.
Firestore usa únicamente `demo-sportpro` en localhost y retira el emulador al terminar.
No invocar `firebase deploy` como parte de las pruebas.

La reproducción conservó intentos fallidos de infraestructura de compilación y del receptor HTTP de pruebas antes de sus correcciones; no se sumaron como verdes. El fallo UTF-8 del receptor se corrigió leyendo Content-Length en bytes, no caracteres.
