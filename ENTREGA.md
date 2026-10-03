# SportPro — correcciones de Julia

Base: `bf79ef3` (`gh/main`), rama `feat/correcciones-julia`. Copia propia de ALICE; sin push ni despliegue Firebase.

## Cambios por punto

1. Registro: sólo Entrenador, Jugador y Padre. ADMIN se asigna desde consola/Admin SDK; repositorio y reglas rechazan autoasignación y cambios de rol desde clientes.
2. Edición: equipos, jugadores y entrenamientos tienen entrada Editar, cargan el documento y esperan la escritura antes de volver. Actualizaciones parciales conservan campos no editados. IDs obtenidos del documento.
3. Asistencia: carga registros previos, conserva notas/estados, guarda únicamente cambios y reutiliza IDs antiguos; nuevas parejas entrenamiento/jugador reciben un ID estable. Un error de lectura impide guardar; un error de escritura conserva pendientes.
4. Jugador: consulta sólo su equipo y su ficha; reglas impiden consultas globales y datos ajenos.
5. Jugador: desde Home abre directamente ficha, entrenamientos/horarios, partidos y calendario de su equipo asignado.
6. Padre: mismo acceso para el hijo asignado. Un perfil admite un hijo; no se implementó selección de varios hijos.
7. Entrenador: puede editar jugadores y entrenamientos. Vincula cuentas mediante sus UID en el formulario del jugador; la transacción actualiza ficha y perfiles juntos, valida roles y rechaza cuentas vinculadas a otro jugador.
8. Calendario mensual por jugador, con días programados y marcas ✓/✗/L/E; lista detallada de entrenamientos. No transforma días sin registro en faltas automáticamente.
9. Rol visible en Home. Jugador/padre pueden copiar su código de cuenta para entregarlo al entrenador.
10. Jugador no tiene controles de edición; Firestore rechaza escrituras sobre ficha y asistencia.

## Validación

- `:app:assembleDebug :app:testDebugUnitTest`: BUILD SUCCESSFUL; 73 pruebas, 0 fallos, 0 errores, 0 omitidas (incluye 9 nuevas de roles/registro/asistencia).
- Firestore Emulator, proyecto local `demo-sportpro`: 4 pruebas agrupadas; registro, escalada, vinculación, lectura propia/ajena, consultas y escritura. Sin credenciales productivas ni despliegue.
- `git diff --check` limpio.
- APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Las pruebas unitarias usan dobles de Firebase; las reglas se ejecutan realmente en emulador. No se ejecutó la app en un dispositivo/emulador Android ni contra los datos de producción.

## Prueba de Henry antes de entregar

1. Desplegar las reglas revisadas al proyecto correcto mediante el proceso autorizado. **El APK por sí solo no cambia las reglas remotas**; la privacidad del servidor depende de ese despliegue.
2. Con entrenador, crear/editar equipo, jugador y entrenamiento; salir y reabrir para verificar persistencia. Probar fecha, hora y ejercicios personalizados. El Boolean se escribe como `active`.
3. Registrar una cuenta Jugador y otra Padre. Copiar el UID visible en Home; editar el jugador con ambos UID. Reabrir Home de esas cuentas: sólo su equipo/ficha, entrenamientos, horarios, partidos y calendario. Cuentas antiguas sin vínculo muestran instrucciones hasta que el entrenador las vincule.
4. Intentar abrir datos de otro equipo y editar la ficha como jugador: debe denegarse. ADMIN no debe aparecer en registro. El registro público de ENTRENADOR se conserva por pedido; este rol mantiene permisos staff sobre la academia.
5. Guardar asistencia de dos jugadores, reabrir, cambiar sólo el que llegó tarde y comprobar que el otro conserva su marca. Las faltas se marcan explícitamente con el control de estado; las casillas marcan presencia.
6. Revisar calendario cambiando mes; fechas corresponden a entrenamientos, no al día de edición de la asistencia.
7. Partidos consumen el esquema documentado `matches`: `teamA`/`teamB` con IDs de equipos, `date` Timestamp o milisegundos, `name`, `status`. No se añadió creación de partidos. Datos con otro esquema requieren adaptación explícita.
8. Probar pantallas en móvil pequeño y volver/rotar durante edición. El padding seguro global existente se conserva. Sincronización concurrente de ediciones de la misma marca sigue la última escritura; no se implementó historial de auditoría.

## Reproducción de reglas

Con Java 17 y Node disponibles: `npm --prefix tests/firestore ci`, luego `npm --prefix tests/firestore test`. Ver `tests/firestore/README.md`. No ejecutar deploy como parte de estas pruebas.
