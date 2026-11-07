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

- Registro: sólo JUGADOR/PADRE, `teamId` y `playerId` ausentes o vacíos.
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
- Elegir ENTRENADOR en la app crea un JUGADOR y una solicitud de rol pendiente. Sólo un ADMIN
  activo aprueba ese destino (ni otro ENTRENADOR ni un creador sin ADMIN). La solicitud por sí sola
  nunca concede staff. Los entrenadores existentes siguen funcionando.

Control de regresión enfocado (dentro de un emulador local):

```sh
node registration-control.mjs
# Para demostrar que detecta las reglas antiguas (salida esperada: FAIL):
node registration-control.mjs /ruta/al/respaldo/firestore.rules
```
El control ejecuta alta de ENTRENADOR y lectura de una ficha de otra familia: ambos deben denegarse.

Estas pruebas validan el archivo local; no demuestran que un proyecto remoto lo
haya desplegado. No se ha ejecutado ningún despliegue.

Validación local ejecutada el 2026-10-03: Firestore Emulator, cuatro pruebas
aprobadas y cero fallos (`npm test`, Node test runner). Se comprobaron tanto
lecturas puntuales como consultas contra las reglas compiladas del servidor
emulado. Esto no sustituye una prueba de aceptación en dispositivos ni una
verificación del despliegue remoto.


## Semana 12

Diez pruebas agrupadas en total. Las nuevas ejecutan lotes reales con evento,
revisión inmutable y contador de versión; corrección/anulación, listeners entre
clientes, evento nuevo pendiente offline, privacidad del borrador y rechazo de
aprobación desactualizada; comunidad, reportes, reacciones y moderación por equipo.
La salida `PERMISSION_DENIED` de los negativos es esperada cuando la aserción es
`assertFails`. No se permite convertir un error inesperado de un positivo en verde.
Los roles pueden cambiar mediante solicitudes aprobadas atómicamente según la
matriz de la suite heredada; la frase histórica «roles inmutables» se refiere a
que el titular no puede editarlos libremente.

## Revocación administrativa (1.2.0/code6)
ADMIN activo retira un único hijo y el puntero de ficha en transacción, con limpieza de vínculo legado
si coincidía. Se preservan hermanos, otros equipos y la familia que ahora ocupe un puntero cambiado.
Revocar invalida códigos anteriores para esa ficha. Coach/padre no pueden revocar ni alterar el sello.
El control revocation.control.test.mjs debe pasar con reglas code6 y fallar con code5.
