# Asignación explícita de equipos a entrenadores existentes

Estado: guía preparada; ejecución y aceptación en consola/teléfono pendientes del dueño. No dar la migración por hecha sólo porque existe esta pantalla.

Justo después de publicar conjuntamente el APK y sus reglas, entrar con una cuenta **ADMIN activa**. En Perfil → Entrenadores por equipo, revisar **cada** entrenador, seleccionar exactamente sus equipos legítimos, Guardar asignaciones y confirmar. El listado inicial puede incluir permisos heredados: NO confirmarlos sin revisarlos. Una selección vacía es válida y retira también el respaldo legado de ownerId/teamId; no borra los equipos.

Repetir para todos los entrenadores, incluso los que no deban conservar ningún equipo. Verificar por lectura que cada perfil tiene una lista explícita coachTeamIds y la auditoría coachScopeBy/coachScopeAt del ADMIN; no basta con abrir la pantalla. Anotar sólo cantidad y resultado, nunca nombres de menores. El entrenador actualiza su sesión; probar un equipo permitido y otro ajeno (denegado por el servidor). No bajar reglas para resolver rechazos.

La creación propia añade únicamente el nuevo equipo a una lista ya explícita (máximo 20); un entrenador legado sin coachTeamIds verá un aviso para que ADMIN complete primero esta revisión. Así no se pierden ni se legitiman automáticamente equipos heredados mediante una auto-migración. La creación y el nuevo alcance se confirman en una transacción; no hay botón de autoasignación a equipos ajenos. Equipo nuevo con sello coachCreated se archiva con active=false, no se borra físicamente ni se quita el sello. Los IDs nuevos están separados de los auto-ID heredados y ligados literalmente al UID del creador, sin interpolarlo en regex.

Si una operación se deniega o queda sin confirmar, conservar el estado y consultar el aviso; no declarar migrado ni repetir escrituras a ciegas. La caché del dispositivo no puede retraer información ya descargada.

Una lista vacía retira acceso a equipos existentes, no el rol de entrenador: un ENTRENADOR activo todavía puede crear sus propios equipos nuevos. Para impedir también nuevas creaciones, ADMIN debe desactivar la cuenta o retirar el rol. No interpretar lista vacía como bloqueo total de la cuenta.
