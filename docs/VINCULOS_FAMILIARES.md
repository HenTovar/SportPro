# Vínculos familiares — 1.2.0 (versionCode 6)

## Uso
1. El entrenador crea/guarda la ficha y vuelve a **Editar jugador**.
2. **Generar código para padre** muestra 12 símbolos (agrupados de cuatro).
3. El padre entra con su cuenta de rol PADRE, abre Inicio → Mis hijos e introduce el código.
4. **Vincular otro hijo** añade la ficha sin sustituir las anteriores. El selector cambia hijo/equipo y
   el acceso de asistencia corresponde al hijo seleccionado. Plantel muestra sólo las fichas vinculadas
   en ese equipo; los demás jugadores nunca se consultan en bloque.

No se copia el UID del padre. El vínculo de la cuenta JUGADOR continúa administrándose como antes.
Un hijo admite un padre/apoderado en este modelo; un padre admite varios hijos, incluso en equipos diferentes.
Los vínculos antiguos `teamId/playerId` permanecen y se presentan como la primera ficha.

## Modelo mínimo compatible
- El perfil `users/{uid}` conserva todos sus campos anteriores.
- Para PADRE se añade `children: {teamId: {playerId: {name, code}}}` y `lastFamilyCode`.
- `User.children` es una vista derivada excluida del mapper de Firestore: el repositorio decodifica
  explícitamente el mapa. Registrar una cuenta nueva no serializa ni autoasigna hijos.
- `familyCodes/{code}`: teamId, playerId, name, issuedBy, createdAt, expiresAt, usedBy, usedAt.
- La ficha del jugador conserva `parentUid` como dueño del vínculo familiar. El código no sustituye
  el rol, ni permite escribir entrenamiento, asistencia o partido.

## Seguridad y límites
- Emisión sólo staff; 12 símbolos aleatorios uniformes de alfabeto de 32 = 60 bits; sin caracteres ambiguos.
- Vence en 20 minutos; el servidor exige futuro y máximo 30 minutos desde `request.time`.
- El padre puede obtener sólo un código conocido y vigente; no puede listar códigos ni emitirlos.
- Canje transaccional de tres documentos: perfil, código y ficha. `getAfter()` exige que los tres
  coincidan; no vale consumir sin dar acceso ni obtener acceso sin consumir. El reloj del cliente no autoriza.
- `children` sólo puede modificar el equipo/jugador que indica ese código. No puede añadir fichas
  ajenas, cambiar roles, sustituir toda la familia ni robar una ficha con `parentUid` ajeno.
- Dos padres/códigos para la misma ficha compiten: sólo uno puede adquirirla. Un código consumido no
  se reutiliza. No se concede acceso a una ficha inactiva.
- Límite preventivo: 20 equipos por perfil y 20 hijos por equipo, además del enlace legado.
- El código es un secreto de un uso: compartirlo sólo con el apoderado previsto. No hay verificación
  externa de parentesco ni rate-limit de intentos; la posesión del código autoriza el vínculo.
- El alta propia sólo permite JUGADOR y PADRE. Elegir Entrenador crea un JUGADOR sin asignación
  y una solicitud PENDING; sólo un ADMIN activo puede aprobar ENTRENADOR. La cuenta creadora sin
  rol ADMIN tampoco puede conceder este rol. Los entrenadores existentes conservan sus permisos.
- Las cuentas que ya se registraron como ENTRENADOR antes de estas reglas no se revocan automáticamente.
  El propietario debe revisar su legitimidad antes de operar con datos reales de menores. Este cambio
  impide nuevas altas privilegiadas; no prueba retrospectivamente quién debía tener acceso.
- Cada cambio de rol aprobado exige children vacío en la misma transacción; volver a PADRE no
  reactiva un mapa antiguo. Esto no borra teamId/playerId legado ni cambia parentUid de las fichas.
- El límite de emisión por staff y la retención del nombre en códigos son pendientes: aún no hay cuota
  de emisión ni borrado de códigos usados. No se atribuye esa protección al generador aleatorio.
- El ADMIN activo puede revocar desde Editar jugador → Administrar vínculo familiar. Selecciona una
  cuenta relacionada y confirma exactamente 1 vínculo. La transacción retira sólo ese hijo del mapa,
  elimina el grupo de equipo si quedó vacío y limpia también teamId/playerId si apuntaban a ese hijo.
- Limpia parentUid sólo si aún pertenece a la cuenta revocada; un puntero de otra familia se conserva.
  Se guardan lastFamilyRevocation (perfil) y familyRevokedAt/familyRevokedParentUid (ficha) para ligar
  las dos escrituras y rechazar códigos emitidos antes de la revocación. No se borran jugadores ni historiales.
- Coach y padre no pueden revocar un vínculo, editar directamente parentUid o borrar el sello de revocación.
  El cambio de rol aprobado sigue teniendo su protocolo separado y limpia el mapa como antes.
- La revocación corta lecturas futuras autorizadas por el servidor, incluida la asistencia. No borra
  copias que un dispositivo ya haya descargado; no se promete retractar información ni limpiar caches ajenas.

## Reglas y despliegue manual
`firestore.rules` es el candidato nuevo. **No se ha desplegado.** El APK necesita estas reglas para
crear/canjear códigos. El propietario debe revisarlas y publicarlas desde Firebase Console después
 de la aceptación. El respaldo seguro de esta ronda es code5, commit 3e5bdddf16b69e9e05ee5d006c5078a376b3a12e.
Si hay un problema, preferir revertir sólo el APK y conservar las reglas que cierran el alta ENTRENADOR.
Volver a reglas code5 requiere decisión del propietario: elimina la nueva revocación pero mantiene cerrado
el alta privilegiada. Las reglas de la base antigua ef24d37c son sólo un antecedente: restaurarlas reabre
la auto-alta staff y NO es un rollback seguro para menores. No borrar perfiles, mapas o códigos.

No es necesario ni se propone cambiar Storage ni pagar un servicio nuevo.

## Verificación
```sh
cd tests/firestore
npm ci --ignore-scripts --no-audit --no-fund
npm test
```
Se usa exclusivamente `demo-sportpro`, emulador local en 127.0.0.1:8188. Cinco grupos familiares nuevos
cubren múltiples equipos/hermanos, legado, aislamiento de ficha/asistencia/partido/comunidad,
concurrencia, caducidad, rol/cuenta inactiva, no-autenticado, repetición, código forjado y atomicidad.
Se conservan los diez grupos anteriores. Los tests JVM incluyen el mapper real de Firestore,
compatibilidad, selección y códigos. Esto no es una instalación Android ni aceptación con cuentas reales.

Referencias de contrato: [reglas y getAfter](https://firebase.google.com/docs/firestore/security/rules-conditions),
[transacciones atómicas](https://firebase.google.com/docs/firestore/manage-data/transactions).
