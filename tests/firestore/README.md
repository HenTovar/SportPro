# Pruebas de autorización Firestore

Desde esta carpeta, con Node y Java 17 disponibles:

```sh
npm ci --ignore-scripts --no-audit --no-fund
npm test
```

Sólo usa el emulador local en `127.0.0.1:8188`, proyecto ficticio
`demo-sportpro`. No requiere login, cuenta Firebase ni despliegue. El primer
arranque puede descargar el emulador. Las dependencias son herramientas de
prueba locales y no forman parte del APK.

La suite ejecuta reglas reales: registro sin ADMIN ni autoasignación,
prohibición de cambiar roles/eliminar perfiles, asignación por staff, aislamiento
de equipo/ficha/asistencia/partidos, consultas filtradas y denegación de escritura
para jugadores/padres. También comprueba cuenta inactiva, autenticación ausente
y compatibilidad de `active` frente al campo legado `isActive`.

Contrato de datos:

- Registro: ENTRENADOR/JUGADOR/PADRE, `teamId` y `playerId` ausentes o vacíos.
- ADMIN: provisionado por administrador de confianza en consola/Admin SDK.
- Roles inmutables para todos los clientes. Staff sólo puede modificar en users
  los campos `teamId` y `playerId`; no se usan reglas de propietario editables.
- Lectores: documento de equipo asignado, ficha de jugador asignada,
  entrenamientos de ese equipo, asistencia filtrada por `playerId`.
- Partidos: dos consultas por `teamA` y `teamB`, unidas por ID en cliente. Los
  eventos/resúmenes heredan el equipo del partido. Partidos sin equipo se niegan
  a jugadores/padres.
- `User.teamId/playerId` son la autoridad de acceso, no campos suministrados por
  el lector ni `Player.parentUid/userUid`. Staff debe mantener esas referencias
  coherentes al vincular una ficha. Una vinculación vacía no concede acceso.
- `active` es el nombre serializado por Kotlin; para cuentas antiguas se acepta
  `isActive` sólo si falta `active`. Un `active=false` prevalece. El filtrado de
  entidades archivadas sigue siendo presentación; su lectura permanece limitada
  por rol/asignación y no exige índices ni filtros de actividad.
- ENTRENADOR sigue siendo un rol de registro permitido por requisito y concede
  acceso de staff. Esta política no equivale a validar acreditación de entrenadores.

Estas pruebas validan el archivo local; no demuestran que un proyecto remoto lo
haya desplegado. No se ha ejecutado ningún despliegue.

Validación local ejecutada el 2026-10-03: Firestore Emulator, cuatro pruebas
aprobadas y cero fallos (`npm test`, Node test runner). Se comprobaron tanto
lecturas puntuales como consultas contra las reglas compiladas del servidor
emulado. Esto no sustituye una prueba de aceptación en dispositivos ni una
verificación del despliegue remoto.
