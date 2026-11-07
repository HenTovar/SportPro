# SportPro 1.3.0 — candidato integrado code13

Integra seguridad por equipo, familias, resumen académico y funciones locales. Candidato para revisión independiente, sin merge ni despliegue. No se presenta como aceptación Android completa. Las reglas y la app deben entregarse juntas, sin bajar a reglas anteriores.

## Seguridad y equipos

El alta pública no concede rol de entrenador: envía solicitud y conserva un rol sin privilegios. Sólo ADMIN aprueba el rol ENTRENADOR y asigna alcance. El ADMIN debe materializar coachTeamIds explícito a TODOS los entrenadores anteriores; [] quita acceso a los equipos existentes pero todavía permite crear equipos propios mientras la cuenta conserve rol activo. Desactivar o retirar el rol impide también crear.

El entrenador crea un equipo propio y añade exactamente ese equipo a su alcance en una transacción, con auditoría, id propio nuevo y máximo20equipos. No puede autoasignarse a equipos ajenos ni actualizar el sello coachCreated; su presencia y valor son inmutables incluso en equipos heredados. Archivar un equipo con sello en lugar de borrarlo. Revocación padre/hijo atómica, mantenimiento y cuota de códigos conservados. Storage no cambia.

## Funciones locales

Cuenta: confirmar Cerrar sesión o Cambiar de cuenta retira las pantallas/listeners, cierra Auth, limpia selecciones/notificaciones e intenta borrar cachés Firestore/Coil. Un marcador persistente impide Login ante fallos. Se advierte pérdida de cambios locales no sincronizados. No se borra el servidor ni se promete borrado forense.

Avisos de inicio/gol/cambio de entrenamiento sólo con listener vivo y confirmación del servidor. Primera carga silenciosa, sin nombres en avisos. Permiso de notificaciones Android13+, negar no impide usar la app. No son push con proceso cerrado. Calendario mensual, goles vigentes por nombre escrito, asistencia sobre registros del mes y racha de partidos cargados; datos ausentes no se inventan. Claro/oscuro/sistema y letra130/150% respetando escalas mayores. Caché y estados pendientes visibles. Detalles y casos reales: docs/FUNCIONES_V2.md.

## Resumen académico con Firebase AI Logic

BOM34.1.0, módulo firebase-ai, proveedor App Check debug sólo en build debug. Sin Retrofit/Gson para el resumen ni clave Gemini de servidor en BuildConfig/repo/APK/chat; la configuración pública Firebase existente no es esa clave de servidor. Aviso «Resumen generado con IA: sólo datos de prueba» y confirmación de datos ficticios por generación. Sin confirmación/hechos sincronizados se usa plantilla. Sólo tipos fijos/minutos/equipos neutros/alias salen del exportador; nombres se reponen localmente. Atestación válida antes del modelo, errores caen a plantilla. La build release actual falla cerrado antes del modelo y usa plantilla. La demo es privada entre adultos con datos ficticios; no constituye excepción a términos y no publicar con IA remota si la app llega a menores reales. No se llamó al proveedor desde el entorno de construcción.

Token debug secreto: registrar sólo teléfono autorizado, no pegarlo en chat/repo/capturas; revocar al terminar, sabiendo que las atestaciones emitidas pueden persistir hastaTTL. Guía docs/APP_CHECK_DEBUG.md. Guardar/aprobar exige servidor, hash y versión vigentes; borrador no se guarda automáticamente.

## Dependencias y reproducción

Restricciones Gradle, sin resolutionStrategy.force: Guava33.7.2-android y protobuf-javalite3.25.5. Si una dependencia declara incompatibilidad debe fallar la resolución, no silenciarla. Resueltas ambas en debug/release. Protobuf ya se seleccionaba en3.25.5 antes de este pin; solicitudes antiguas de dependencias no equivalen a artefactos finales. Los hashes y origen no prueban licencias ni ausencia de vulnerabilidades.

JDK17, SDK35, configuración Firebase Android local ignorada. La primera resolución de las versiones nuevas requiere red; las corridas siguientes pueden usar --offline tras verificar que todos los artefactos estén en caché. Regresión completa y APK se entregan con resultados y SHA específicos; no heredar resultados de otro code.

```sh
./gradlew --no-daemon --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest compileReleaseKotlin
```

## Límites de aceptación

Pruebas JVM/SDK locales y compilación no acreditan teléfono: siguen pendientes logoutA→B y cacheAndroid, Atrás, reinicio durante limpieza, permisos/avisos/segundo plano, IME y toda la matriz claro/fontScale1.3/1.5. Instrumentación se compila, no se ejecuta sin dispositivo/AVD. App Check real, modelo disponible en el plan y llamada real sólo se verifican en el teléfono autorizado para el caso académico permitido. Concurrencia Firestore real, licencias y alcance explotable de avisos de seguridad no medidos. Instalar APK no publica reglas. No hay cambios remotos de cuentas/datos/reglas en esta entrega.
