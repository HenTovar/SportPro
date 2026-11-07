# Guía de demostración y video — Semana 12

## Preparación

Usar cuentas de prueba autorizadas, dos dispositivos Android, dos equipos ficticios registrados y un jugador/padre asignado al equipo A. Revisar/desplegar las reglas antes de la prueba remota. Entrenador: Home → Partidos → elegir equipo → Nuevo partido; seleccionar rival y nombre. Equipo A es el primer equipo seleccionado; B es el rival.

Configurar el proveedor IA localmente sólo si se tiene autorización de uso. No filmar claves, correos personales, UID completos ni datos de menores. El video real debe ser continuo según el enunciado; este guion no sustituye la grabación.

## Secuencia para grabar

1. Abrir el mismo partido en ambos dispositivos. Iniciar el encuentro. Mostrar el catálogo: gol, falta, tarjetas, cambio y tipos configurables. Registrar un gol al minuto 5 para A: marcador 1–0 y la misma cronología en el otro dispositivo, sin recargar.
2. Registrar falta sin jugador y sin minuto. Luego registrar tarde una tarjeta al minuto 3: aparece antes del gol por minuto, no por orden de llegada. Corregir el gol al equipo B y abrir Historial: ver actor/revisión. Anular sin borrar: marcador vuelve a 0–0; la entrada e historial siguen visibles. Cambiar el nombre del tipo en catálogo y comprobar que los registros anteriores conservan su etiqueta.
3. Desactivar la conexión de un dispositivo y registrar un evento nuevo. Mostrar aviso de pendientes/caché. Reconectar: esperar sincronización y aparición en el otro dispositivo. Corregir requiere conexión; no afirmar que el dispositivo desconectado está actualizado.
4. Home → Comunidad. Publicar un texto inocuo, comentar y reaccionar desde la otra cuenta. Reportar. Entrenador abre reportes y oculta la publicación; jugador/padre deja de verla. Una cuenta de otro equipo no puede leerla. No usar datos personales reales.
5. Entrenador → Resumen IA. Generar con el proveedor configurado, contrastar con cronología, editar una frase y aprobar. Jugador sólo ve el aprobado. Registrar/corregir otro evento: el anterior deja de presentarse como actual; generar y revisar otra versión. Si falta configuración no hay una prueba IA real: mostrar el aviso y declarar la limitación, no presentar un texto de prueba como salida del proveedor.
6. Finalizar partido y comprobar persistencia al cerrar y reabrir. Filmar también los datos incompletos y las correcciones: forman parte de la evaluación.

## Cómo explicarlo al profesor

«La pantalla escucha Firestore con addSnapshotListener, como el ejemplo de tiempo real de clase. El repositorio convierte los documentos y el ViewModel publica el estado que observa Compose. El marcador se calcula de eventos vigentes; no es un número que se edita por separado. Una corrección usa transacción y añade una revisión, por eso sabemos quién cambió qué sin borrar el original. Un lote mantiene evento, contador de versión e historial juntos.

La comunidad usa rutas del equipo y reglas de servidor: jugador/padre sólo leen su equipo, el reporte no es público y el entrenador modera. El resumen usa Retrofit; recibe únicamente hechos del partido, vuelve como borrador y requiere aprobación humana. Comparamos hash y versión para que un cambio posterior obligue a revisar otra vez. Probamos reglas en un emulador independiente; eso no despliega nada en el proyecto real».

No decir «ya se probó en dos celulares» ni «la IA nunca alucina» si no se ejecutó esa prueba. Adjuntar el video real y anotar los dispositivos y las cuentas por rol, sin credenciales.
