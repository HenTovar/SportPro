# Seguridad 1.3.0 — candidato, no desplegado

## Antes → después
| Caso | Antes | Ahora |
|---|---|---|
| Entrenador | Acceso global por rol | Equipos asignados por ADMIN; ninguna autoasignación |
| ADMIN | Global | Global, sólo cuenta activa |
| Ficha revocada | Borrar/recrear podía perder el sello | No se borra una ficha sellada; se puede archivar |
| Rol de una familia revocada | El vínculo legado podía reaparecer | El historial de revocación invalida el par legado; requiere código nuevo |
| Emisión | Sin cuota, guardaba nombre | Máximo 20 por ventana de una hora por emisor; sin nombre |
| Mantenimiento | Recibos quedaban guardados | Canje elimina el recibo usado; limpieza confirmada de usados/vencidos y redacción de nombres antiguos |

## Migración compatible y publicación

No hay migración automática de datos ni despliegue desde esta rama. El APK y las reglas nuevas deben revisarse/publicarse juntos: la app anterior no implementa el contador ni el canje sin nombre.

1. Revisar las cuentas ENTRENADOR existentes. No se promueve ninguna cuenta por registrarse. ADMIN sigue aprobando solicitudes de entrenador.
2. Si `coachTeamIds` no existe, el entrenador conserva sólo `users.teamId` y los equipos cuyo `ownerId` es ese UID. Son señales verificables; no se infieren otros equipos por haber tenido acceso global. Un entrenador sin señal necesita asignación explícita antes de trabajar.
3. ADMIN entra en Perfil → Entrenadores por equipo. Selecciona entrenador y equipos, confirma y guarda. Se escriben `coachTeamIds`, `coachScopeBy` y `coachScopeAt` (hora del servidor). Hasta 20 equipos explícitos. Una lista vacía revoca también el acceso por `ownerId`/`teamId` legado. ADMIN debe revisar cualquier caso histórico que exceda ese límite antes de materializarlo.
4. El entrenador actualiza sesión y comprueba sus equipos. La lista se obtiene por IDs, nunca con una consulta global. Las reglas controlan cada operación, incluso si la interfaz conserva una sesión antigua.
5. Los vínculos familiares antiguos siguen leyéndose. El código antiguo aún no usado puede canjearse desde la app nueva: la escritura elimina su nombre. El nuevo mapa guarda sólo el código; el nombre visible se obtiene de la ficha autorizada, en memoria.
6. En una ficha de jugador, el personal autorizado puede previsualizar mantenimiento: confirma el número exacto de usados/vencidos a borrar y nombres de códigos antiguos activos a quitar. ADMIN puede limpiar cualquier emisor del equipo; un entrenador sólo los suyos. Cada lote es de hasta 100 documentos. Cancelar no escribe.

### Límites explícitos

- La cuota es una ventana fija de una hora desde la primera emisión, controlada por el servidor. Borrar códigos no reinicia el contador; el contador no se puede borrar desde la app.
- La limpieza de vencidos necesita una app abierta y confirmación. No hay purga de servidor con la app cerrada. El borrado posterior al canje es de mejor esfuerzo; si falla, queda un recibo sin nombre que puede limpiarse después.
- El reloj del teléfono sólo propone vencimiento/reinicio; el servidor rechaza fechas o contadores fuera del contrato.
- Las reglas revocan lecturas/escrituras futuras, no borran datos ya descargados ni las cachés del teléfono. La prueba del listener fuerza una escritura posterior a la revocación; no afirma latencia inmediata al cambiar sólo la dependencia de permisos.
- El entrenador no puede leer perfiles PADRE completos ni una lista global de usuarios/solicitudes. El alta de fichas sin cuenta sigue disponible; ADMIN vincula cuentas nuevas no asignadas. El entrenador conserva acceso al perfil JUGADOR que ya pertenece a su equipo.
- Crear equipos, transferir propiedad y escribir la biblioteca global de ejercicios requiere ADMIN. El entrenador edita sus equipos/fichas/sesiones; los partidos contra equipos no asignados los prepara ADMIN si la selección local no dispone del rival.
- El código sigue siendo portador, de un uso y con vencimiento. No sustituye verificar al apoderado antes de entregarlo.
- No se ha probado Android en dispositivo ni concurrencia contra Firestore de producción. Los tests locales usan el SDK real contra un emulador aislado.
- No se debe volver a reglas antiguas para sortear una denegación: reabriría los defectos. Conservar respaldo para análisis; cualquier reversión requiere revisión y autorización.

## Verificación reproducible

Con Java 17 y las dependencias ya instaladas:

```sh
cd tests/firestore
npm test
```

Ejecuta archivos en serie, proyecto `demo-sportpro-security`, Firestore en `127.0.0.1:8193`, sin usar datos reales. `security-v2.control.test.mjs` ejecuta los mismos cuatro ataques contra ambas versiones; `SPORTPRO_RULES_FILE` permite seleccionar un archivo de reglas para el control rojo.

`rules.test.mjs` conserva el protocolo anterior como histórico; no es el test por defecto del esquema nuevo. Sus casos dependientes de entrenador global, códigos con nombre/sin cuota y aprobaciones sin limpiar vínculos han sido sustituidos por contratos explícitos en `security-v2.test.mjs`. No se afirma que la suite antigua completa pase con el protocolo nuevo.
