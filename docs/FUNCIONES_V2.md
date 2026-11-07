# Funciones locales del equipo — WIP 1.3.0

**Candidato integrado 1.3.0/code13 en pruebas, no aprobado para instalar ni publicar.** La lógica de funciones y el protocolo de cuenta pasaron pruebas JVM independientes y las consultas se midieron contra servidor emulado. Esto no acredita Android real: las pruebas Compose, los píxeles, los avisos del sistema y el aislamiento entre cuentas en teléfono siguen pendientes. Los resultados de la regresión integrada se entregan por separado.

Desde Inicio, selecciona un equipo y abre **Calendario y estadísticas**. Desde Perfil cambia el tema, el tamaño de letra y los avisos. No se necesita ninguna colección nueva ni publicar reglas para este carril; la integración de seguridad tiene su propio proceso de revisión.

## Avisos locales

Activa **Avisos locales** en Perfil. En Android 13 o posterior se solicita `POST_NOTIFICATIONS`; rechazarlo no impide usar la app. Se requiere que el sistema también permita las notificaciones de la aplicación.

Los avisos corresponden al equipo seleccionado: un partido pasa de programado a en vivo, se confirma un gol vigente o cambia/se retira un entrenamiento. La primera carga de cada listener es silenciosa: no anuncia historia. Caché y escrituras pendientes no producen avisos. Corregir sólo el nombre de un goleador no anuncia otro gol. Los avisos no muestran nombres de menores ni contenido privado en la pantalla de bloqueo.

**No son push de servidor**. Se generan mientras el proceso y el listener sigan activos, con la app abierta o en segundo plano. Android puede detenerlos, especialmente con ahorro de batería. Con la app cerrada o el proceso terminado no se garantiza recepción. No se han contratado Cloud Functions ni plan Blaze, ni añadido una colección de tokens. Una automatización de servidor sería otro proyecto.

[Permiso oficial de Android](https://developer.android.com/develop/ui/compose/notifications/notification-permission).

## Estadísticas: qué cuentan

- Goles: eventos `countsGoal=true`, no anulados, del equipo seleccionado. Se agrupan por el **nombre escrito**: dos personas con el mismo nombre no se distinguen. Sin nombre queda `Sin identificar`. No se inventan identificadores. Correcciones y anulaciones reemplazan el evento, no acumulan goles históricos.
- Asistencia mensual: se usa la fecha del **entrenamiento**, no la fecha en que alguien editó la marca. Se toma la última marca por jugador/sesión. Sin entrenamiento fechado se informa fuera del cálculo. El porcentaje es sobre marcas registradas, no sobre todas las sesiones; no se inventan ausencias.
- Padre/jugador consulta sólo la asistencia de su ficha seleccionada (`where playerId`); el staff usa la asistencia del equipo autorizado. Las reglas del servidor siguen decidiendo acceso.
- Racha: partidos finalizados y fechados, de más reciente a antiguo; compara goles vigentes a favor/en contra, incluyendo partidos como visitante. Empates interrumpen racha de victorias o derrotas. Se calcula sólo con feeds cargados y se señala información provisional si falta confirmación.
- El selector de mes filtra calendario y asistencia. Los goleadores y la racha abarcan todos los partidos cargados, como dice cada título.

## Calendario

Mes real, semana de lunes a domingo y años bisiestos. El punto señala días con partido o entrenamiento. Toca un día para ver los registros y luego el registro para abrirlo. Anterior/Siguiente cambia de mes; Hoy vuelve al actual. Se usa la zona horaria del teléfono y sólo fechas válidas; los registros sin fecha se cuentan por separado. No hay fuentes de calendario externas.

## Pantalla y letra

Perfil permite **Sistema / Claro / Oscuro** y letra **Sistema / 130% / 150%**. Si Android ya usa una letra mayor, se respeta. Las preferencias sólo viven en este dispositivo; no cambian el perfil de Firestore.

Los controles de registro se apilan; los botones crecen en vez de fijar altura; la barra inferior permite dos líneas y reserva más alto con letra grande. Calendario, controles y resultados se desplazan. El fondo y los iconos de las barras del sistema siguen el tema elegido.

La revisión de código cubre todas las pantallas existentes; las pruebas Compose adicionales ejercitan calendario/estadísticas claro y oscuro a 1.3/1.5. **Compilar instrumentación no demuestra layout en teléfono**. La comprobación de píxeles, IME y todas las pantallas en dispositivo sigue siendo una prueba del cliente.

## Sin internet

Firestore conserva una caché persistente local configurada antes de empezar a usar la base. Permite consultar datos ya cargados; sin red no carga datos que nunca se descargaron en este dispositivo ni sustituye el primer login online. [Documentación de caché](https://firebase.google.com/docs/firestore/manage-data/enable-offline).

- Sin red validada: `Sin conexión`, con aviso de datos guardados y sincronización pendiente.
- Si los snapshots del equipo tienen cambios locales: `pendientes de sincronizar`.
- Si llegan de caché: se distingue de confirmación del servidor. [Metadata de listeners](https://firebase.google.com/docs/firestore/query-data/listen).

El indicador de pendientes corresponde a los snapshots observados del equipo; no es un inventario de todas las operaciones de toda la app. Los avisos locales necesitan una actualización confirmada del servidor. Las escrituras existentes compatibles con la cola de Firestore se sincronizan al reconectar; una transacción, aprobación o revocación que necesita servidor no se presenta como exitosa offline. La caché puede conservar información ya descargada después de una revocación: revocar corta lecturas futuras del servidor, no borra copias anteriores del dispositivo. Al cambiar de usuario/equipo se reinicia el estado del módulo y se cancelan sus listeners; cerrar sesión limpia los avisos e intenta borrar la persistencia Firestore antes de admitir otra cuenta; ante fallo bloquea Login. Esa API y el resultado entre cuentas no se han medido en un teléfono.

## Pruebas del cliente

1. Permiso de avisos concedido y denegado; primera carga sin avisos históricos. Con otro cliente iniciar partido, marcar gol y modificar entrenamiento. Comprobar un aviso por cambio confirmado, también en segundo plano mientras el proceso siga vivo.
2. Dos equipos/dos familias: nunca consultar asistencia ajena. Cambiar equipo/hijo y cerrar sesión: no conservar cifras o avisos del anterior.
3. Goles corregidos/anulados, jugador desconocido, empate y partido visitante; mes de febrero bisiesto y cambios de año; asistencia editada tarde.
4. Claro/oscuro y letra 1.3/1.5 en login, registro, inicio, equipo, partidos, eventos, comunidad, formularios, asistencia y perfil, con IME abierto/cerrado.
5. Cargar datos online, desconectar: caché e indicador; registrar un evento permitido por el flujo existente, comprobar pendiente, reconectar y confirmar una sola sincronización. Sin datos previos, mostrar vacío/carga honesta; no inventar acceso.

No ejecutar estas pruebas con datos privados de terceros. El candidato no se acepta a sí mismo; requiere revisión independiente y aceptación del cliente.


## Cuenta: cierre y cambio de usuario (candidato pendiente de validar)

En Perfil, la sección Cuenta contiene Cerrar sesión y Cambiar de cuenta. Ambos
requieren confirmación y abren un Login nuevo, con correo y contraseña vacíos.
La opción de Inicio lleva a Perfil para usar la misma confirmación.
Se retiró el UID sin formato de Inicio; no se presenta como código de vínculo.

La barrera superior retira el árbol de navegación (incluidos sus ViewModels y
listeners) antes de cerrar Firebase Auth. Elimina las selecciones de equipo/hijo,
las preferencias de notificación por cuenta, los avisos locales y las cachés de imágenes
de Coil (memoria y disco). Después termina
Firestore, borra su persistencia y crea una instancia nueva. No elimina datos del
servidor. La confirmación advierte que se descartan cambios locales no sincronizados.
La limpieza no equivale a un borrado forense seguro del almacenamiento.

Un marcador de limpieza sobrevive al cierre del proceso. Si una fase falla, Login
permanece bloqueado y se muestra Reintentar limpieza. Un cambio externo de identidad
también dispara la barrera. El repositorio de autenticación y TeamSession no se
modifican: la integración debe conservar AccountBoundary por encima de ambos.

Aceptación Android requerida (no medida con la compilación):
1. Cuenta A: seleccionar equipo/hijo, cargar datos y producir un aviso local.
2. Perfil > Cerrar sesión > Cancelar conserva la sesión. Confirmar limpia selecciones,
   avisos y caché. Atrás no vuelve a ninguna pantalla autenticada.
3. Perfil > Cambiar de cuenta > Confirmar abre Login vacío. Entrar con B, sin equipo
   de A; desconectar la red y comprobar que no se muestran registros de A.
4. Interrumpir la app durante la limpieza; al abrir, termina de limpiar antes de Login.
5. Inyectar fallo de limpieza en el arnés: Login no se admite hasta reintento exitoso.

AccountExitTest comprueba el protocolo con dobles y fallos por fase. No acredita
por sí solo la API Android de caché, las notificaciones ni el gesto Atrás.
Referencia: https://firebase.google.com/docs/reference/android/com/google/firebase/firestore/FirebaseFirestore
