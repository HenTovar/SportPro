# Resumen: Firebase AI Logic y respaldo por plantilla

**Sólo demostración académica privada con datos ficticios y público adulto.** La pantalla exige confirmación de datos ficticios para solicitar IA; sin ella usa plantilla. No publicar ni habilitar IA remota si la app llega a menores reales. La comprobación no puede demostrar por sí sola que los datos introducidos sean ficticios: es responsabilidad del dueño respetar ese alcance.

El botón «Generar IA o plantilla» usa Firebase AI Logic, backend Gemini Developer API, modelo `gemini-3.5-flash-lite`. No hay endpoint alternativo ni campo de clave de Gemini en BuildConfig o propiedades locales. El BOM Firebase se actualiza a34.1.0 y usa módulos principales (los KTX dejaron de incluirse). Evita la regresión Auth23.2.1 del primer BOM que incluía AI Logic. No añade otro proveedor.

## Privacidad

La solicitud sólo contiene tipos de evento de un conjunto fijo, minutos válidos o UNKNOWN, TEAM_1/TEAM_2 y alias ordinales Jugador N. Nunca lleva nombres, notas, IDs de usuario/jugador/equipo/partido, ni etiquetas de evento libres. Las desconocidas se convierten en OTHER. Eventos anulados no se envían. El mapa de alias→nombre se queda en memoria del teléfono y se aplica tras la respuesta, sin otra llamada.

La prueba captura el UTF-8 entregado al doble del proveedor y busca canarios en TODOS los campos privados, también etiquetas e IDs. Se prueba un control que reproduce el formato filtrante anterior. Eso NO prueba una llamada real a Gemini: la validación real la realiza el dueño del proyecto en su teléfono.

## Respaldo y revisión

Error de red, cuota, configuración, App Check, respuesta inválida/vacía/larga/alias ajeno o timeout producen plantilla. La cancelación del usuario se propaga. Offline no inicializa ni llama al SDK: usa hechos locales, muestra «borrador local» y no queda esperando una escritura Firestore. La generación conserva un borrador en memoria hasta «Guardar revisión». Guardar o aprobar exige volver a sincronizar y regenerar si los hechos no se pudieron confirmar; la publicación sigue requiriendo revisión explícita, hash/versión vigentes y reglas del servidor. No se autoaprueba nada. Una escritura explícita con timeout se informa como no confirmada: puede sincronizarse después, nunca se declara fallida o inexistente sin evidencia.

Se limita a200 eventos para una llamada y a40 filas para la plantilla; el resumen señala si la plantilla omite filas, mientras el marcador cuenta todos los goles vigentes. La IA puede equivocarse aun con hechos anónimos: el entrenador revisa antes de publicar.

## Consola, claves y App Check

La clave secreta de Gemini queda administrada por Firebase, nunca se pide ni añade al APK/repositorio/chat. La configuración pública de la app Firebase que ya existía (`google-services.json`, ignorado por git) es distinta de esa clave de Gemini; el SDK necesita esa configuración del cliente. No publicar sus valores.

Debug abre y la plantilla funciona sin registrar App Check. Sólo al pedir IA se instala el proveedor de depuración; su token se genera en el teléfono, no se empaqueta. Release no incluye ese proveedor ni un token fijo y, en esta implementación, siempre falla cerrado antes del modelo y usa plantilla: no hay atestación de producción configurada. Code13 se entrega sólo como APK debug académico; el build release queda pendiente por el límite térmico de la máquina, no aprobado ni probado en teléfono. Debug debe obtener un token de atestación válido antes de inicializar Firebase AI, incluso si el servidor no exige enforcement. Eso no desactiva la política del servidor: desde2nov2026 Firebase puede exigir atestación y rechazar la llamada IA. El dueño debe validar su configuración autorizada en consola/dispositivo; no se intenta evadirla ni cambiarla desde aquí. No confundir respaldo operativo con «Gemini real verificado».

Referencias oficiales verificadas7nov2026:
- https://firebase.google.com/docs/ai-logic/get-started
- https://firebase.google.com/docs/ai-logic/models
- https://firebase.google.com/docs/ai-logic/app-check
- https://firebase.google.com/support/release-notes/android

No se hacen llamadas reales al proveedor en las pruebas de construcción. El permiso de compilar también está sujeto a la coordinación térmica del equipo, no sólo a que pase la hora acordada.

## Datos locales y copia de seguridad

El manifiesto desactiva `allowBackup`; las reglas antiguas y Android 12+ excluyen todos los dominios de almacenamiento de la app, tanto en copia a la nube como en transferencia de dispositivo. No se prueba aquí el comportamiento de cada fabricante ni se borran copias antiguas que ya pudieran existir. La validación en teléfono sigue pendiente.
