# SportPro 1.2.0 — navegación funcional

## Primer incremento
Base: `ef24d37ca4d1d3052e52788e54acf2247322f0ee` (`feat/semana12`).
Sin cambios de colecciones, reglas, repositorios ni dependencias en este incremento.

| Antes | Ahora |
|---|---|
| Inicio con botones sueltos, sin agenda | Próximo entrenamiento, partido próximo/en vivo, errores independientes y accesos según rol |
| Todas las secciones detrás de Equipos | Barra fija Inicio / Partidos / Equipo / Comunidad / Perfil |
| Plantel, entrenamientos, partidos y muro sin contenedor común | Ficha de equipo con cuatro pestañas y estado de consulta separado |
| Acciones de equipo repetidas dentro de cada tarjeta de jugador | Acciones comunes en las pestañas del equipo; cada jugador conserva edición e historial propio |
| Equipo elegido se pierde entre pantallas | Preferencia local separada por cuenta, validada contra permisos y equipos activos |
| Listas sin búsqueda | Jugadores por nombre/dorsal/posición; partidos por nombre/estado |
| Acceder al evento requiere entrar al partido y abrir editor | Acceso desde Inicio al editor del partido en vivo (sólo staff) |
| Subir foto puede esperar indefinidamente | Presupuesto de 20 segundos, cancelación y error visible; permite guardar sin foto |

## Recorridos previstos (desde Inicio, sesión y equipo cargados)
Estos son recuentos de transiciones de la interfaz, revisados contra las rutas, **no mediciones de toques en un dispositivo**.
No incluyen elegir un equipo sin selección previa, crear un entrenamiento/partido que no existe ni confirmar datos.

| Rol y objetivo | Antes | Ahora |
|---|---:|---:|
| Staff, registrar asistencia al próximo entrenamiento | Equipos → Entrenamientos → Asistencia: 3 | Registrar asistencia: 1 |
| Staff, abrir partido en vivo | Equipos → Partidos → partido: 3 | Ver partido: 1 |
| Staff, abrir editor de evento del vivo | Equipos → Partidos → partido → agregar: 4 | Registrar evento: 1 |
| Jugador, historial personal | Equipos → Jugadores → historial: 3 | Mi asistencia: 1 |
| Jugador, partido en vivo | Mis partidos → partido: 2 | Ver partido: 1 |
| Cuenta con equipo, muro | Comunidad: 1 (staff debe elegir equipo) | Comunidad: 1 (equipo recordado) |

Si no hay próximo entrenamiento, el atajo lleva al listado para seleccionar/crear uno: no inventa una asistencia.
Sin partido en vivo, el acceso conduce a preparar/seleccionar partido. El jugador no obtiene acciones de staff.

## Pruebas reproducibles
Con JDK17, SDK35 y el AAPT2 adecuado al host:

```sh
./gradlew --no-daemon assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
```

Resultado inicial: 120 pruebas JVM (90 anteriores + 30 nuevas), cero fallos; lint cero errores/fatales y 29 avisos.
`DashboardShortcutsTest` aporta cinco pruebas Compose compiladas para los atajos. **No ejecutadas** aquí:
no hay AVD, imagen de sistema ni dispositivo disponible. En un dispositivo configurado:

```sh
./gradlew connectedDebugAndroidTest
```

La prueba de timeout utiliza reloj virtual; no afirma una carga real en Storage. Firebase no se despliega.
El APK de depuración está firmado para pruebas, no es un release de tienda.

## Segundo incremento
Vínculo familiar por código de un uso y varios hijos por padre (incluidos equipos distintos)
añadido en versionCode 4. Contrato, límites, reglas y despliegue manual en [VINCULOS_FAMILIARES.md](VINCULOS_FAMILIARES.md).
El primer APK intermedio (versionCode 3) no lo incluía.
La instalación y aceptación con cuentas reales y el recorrido visual en Android siguen pendientes.
