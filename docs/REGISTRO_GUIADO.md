# Registro guiado y pertenencia a equipos (candidato 1.3.1)

Datos obligatorios por rol, salvo talla y peso. Entrenador solicita aprobación, nunca obtiene ENTRENADOR por el formulario. Mis datos no cambia el rol autorizado. Los datos personales completos viven en guidedProfiles; no se copian al código, al rastro ni al contexto de IA.

El staff asignado genera un código POR EQUIPO (12 caracteres). Es portador y reutilizable:72horas como máximo o40canjes; no requiere confirmar manualmente cada ingreso. Compartirlo fuera del plantel permite que un extraño cree su ficha y adquiera el acceso normal de jugador, NO las fichas/contactos/asistencias de los demás. El nombre es autodeclarado, no una identidad verificada. El staff debe revisar quién se unió y revocar/rotar un código filtrado; emitir otro invalida el anterior. Cuota independiente20emisiones por hora/staff, sin borrar ni reiniciar el contador desde el cliente.

Canje transaccional: contador+perfil+ficha propia+rastro inmutable por código/uid. Solo JUGADOR activo sin equipo; nunca selecciona UID/ficha ajenos. Un solo equipo activo, mismo código no puede reutilizarse tras retiro. El jugador se retira sin permiso del entrenador; staff puede retirar intrusos. ADMIN mueve a otro equipo. Ficha anterior queda inactiva con historial y acción inmutable. Cambiar de rol estando JUGADOR vinculado requiere retirarlo primero para no dejar fichas activas huérfanas.

Expulsión NO es baneo permanente: no admite el código ya usado; con otro código válido conocido puede volver. Este comportamiento está escalado a coordinación como decisión explícita. El retiro/traslado no arrastra permisos de tutores a otra ficha: se renueva el vínculo familiar con el mecanismo existente. Sello de revocación y prohibición de borrado de fichas vinculadas se conservan.

Edición personal por el dueño, deportiva (posición/pie) por staff; ADMIN puede corregir ambos. Datos completos: dueño, tutor realmente vinculado a ficha actual y staff de equipo actual; no compañeros ni entrenadores ajenos. Un perfil legado sin deportes puede guardar personales, pero antes de unirse ADMIN debe completar los campos deportivos faltantes; no se inventan valores ni se permite al propio jugador editar deportes después del alta.

Pruebas con SDK/emulador y JVM no equivalen a uso en teléfono. Alcance académico vigente: adultos y datos ficticios, sin publicar en tienda; App Check debug requiere revocación al terminar. No desplegar reglas ni APK sin revisión y juicio. Índices requeridos: únicamente filtros simples existentes, más active en subcolección players.
