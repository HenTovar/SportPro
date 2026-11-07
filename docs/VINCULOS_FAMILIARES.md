# Vínculos familiares — 1.2.0 (versionCode 4)

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
- El registro público ENTRENADOR y su autoridad de staff son **heredados**. No equivalen a una
  acreditación profesional. Esta ampliación no cambia esa política ni la vuelve segura frente a
  una cuenta que se registre legítimamente como staff. Revisar esa política antes de operar con menores reales.
- Retirar un vínculo requiere administración de confianza que retire la entrada en `children`
  y el `parentUid` correspondiente; no se añade un borrado destructivo automático.

## Reglas y despliegue manual
`firestore.rules` es el candidato nuevo. **No se ha desplegado.** El APK necesita estas reglas para
crear/canjear códigos. El propietario debe revisarlas y publicarlas desde Firebase Console después
 de la aceptación. Las reglas previas son las del commit `ef24d37ca4d1d3052e52788e54acf2247322f0ee`:

```sh
git show ef24d37ca4d1d3052e52788e54acf2247322f0ee:firestore.rules > firestore.before.rules
```

Para rollback, restaurar ese archivo en la consola y la versión anterior del APK. No borrar perfiles,
ni mapas familiares ni códigos: se conservan como datos y las reglas anteriores no conceden accesos por ellos.
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
