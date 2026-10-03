# Historias de Usuario - SportPro

## Resumen de Historias de Usuario

| Historia de Usuario | Responsable | Puntos de Historia |
|---|---|---|
| US-01: Registro de Usuario por Rol | 24100514 – Henry Aaron Tovar Landa | 8 |
| US-02: Inicio de Sesión y Autenticación | 24100514 – Henry Aaron Tovar Landa | 5 |
| US-03: Recuperación de Contraseña | 24100514 – Henry Aaron Tovar Landa | 3 |
| US-04: Crear y Gestionar Equipo | 24100514 – Henry Aaron Tovar Landa | 8 |
| US-05: Registrar Jugador en Equipo | 24100514 – Henry Aaron Tovar Landa | 5 |
| US-06: Ver y Editar Perfil de Jugador | 24100514 – Henry Aaron Tovar Landa | 5 |
| US-07: Registrar Contacto de Emergencia | 24100514 – Henry Aaron Tovar Landa | 3 |
| US-08: Asignar Padre/Apoderado a Jugador | 24100514 – Henry Aaron Tovar Landa | 5 |
| US-09: Planificar Sesión de Entrenamiento | 24100478 – Julia Jhamilett Rojas Estrada | 8 |
| US-10: Agregar Ejercicios a Librería Reutilizable | 24100478 – Julia Jhamilett Rojas Estrada | 5 |
| US-11: Registrar Asistencia a Entrenamiento | 24100478 – Julia Jhamilett Rojas Estrada | 5 |
| US-12: Ver Historial de Asistencia | 24100478 – Julia Jhamilett Rojas Estrada | 3 |
| US-13: Convocar Jugadores a Partido | 24100478 – Julia Jhamilett Rojas Estrada | 5 |
| US-14: Confirmar Disponibilidad para Partido | 24100478 – Julia Jhamilett Rojas Estrada | 3 |
| US-15: Crear Alineación (Titulares y Suplentes) | 24100478 – Julia Jhamilett Rojas Estrada | 8 |
| US-16: Ver Alineación como Jugador/Padre | 24100478 – Julia Jhamilett Rojas Estrada | 3 |
| US-17: Registrar Evento de Partido en Tiempo Real | 24100514 – Henry Aaron Tovar Landa | 13 |
| US-18: Corregir o Anular Evento de Partido | 24100514 – Henry Aaron Tovar Landa | 8 |
| US-19: Ver Scoreboard y Timeline en Tiempo Real | 24100514 – Henry Aaron Tovar Landa | 8 |
| US-20: Generar Resumen Narrativo de Partido con IA | 24100514 – Henry Aaron Tovar Landa | 13 |
| US-21: Revisar y Aprobar Resumen IA | 24100478 – Julia Jhamilett Rojas Estrada | 5 |
| US-22: Ver Estadísticas Acumuladas del Jugador | 24100478 – Julia Jhamilett Rojas Estrada | 5 |
| US-23: Crear Anuncio de Academia | 24100478 – Julia Jhamilett Rojas Estrada | 3 |
| US-24: Recibir Notificaciones | 24100478 – Julia Jhamilett Rojas Estrada | 5 |
| US-25: Crear Post en Comunidad | 24100478 – Julia Jhamilett Rojas Estrada | 5 |
| US-26: Comentar y Reaccionar a Post | 24100478 – Julia Jhamilett Rojas Estrada | 3 |
| US-27: Publicar Aviso de Prueba/Convocatoria Abierta | 24100478 – Julia Jhamilett Rojas Estrada | 5 |
| US-28: Filtrar Avisos de Prueba | 24100478 – Julia Jhamilett Rojas Estrada | 3 |
| US-29: Reportar Contenido Inapropiado | 24100478 – Julia Jhamilett Rojas Estrada | 3 |
| US-30: Moderar y Gestionar Reportes (Admin) | 24100478 – Julia Jhamilett Rojas Estrada | 5 |

---

## Detalle de Historias de Usuario

### US-01: Registro de Usuario por Rol

**Número:** US-01  
**Rol:** Cualquier usuario  
**Nombre historia:** Registro de Usuario por Rol (DT, Jugador, Padre, Admin)  
**Puntos de historia estimados:** 8  
**Programador responsable:** 24100514 – Henry Aaron Tovar Landa

**Descripción:**  
Como usuario deseo registrarme en SportPro seleccionando mi rol (Entrenador, Jugador, Padre o Administrador) con la finalidad de acceder a funcionalidades específicas según mi rol y proteger mi cuenta con autenticación.

**Criterios de aceptación:**
- El usuario accede a pantalla de registro (P-01)
- Debe ingresar: nombre completo, correo electrónico, contraseña (mínimo 8 caracteres), confirmación de contraseña
- Debe seleccionar un rol: "Entrenador/DT", "Jugador", "Padre/Apoderado" o "Administrador"
- Si rol es "Padre", debe ingresar nombre del hijo/hija a vincular (búsqueda por email del hijo)
- Si rol es "Jugador", debe seleccionar equipo/academia de la lista
- El sistema valida email único (no registrado previamente)
- Al enviar, se almacena en Firebase Authentication
- Se registra documento en `users/{uid}` con: nombre, email, rol, rol_timestamp, photoUrl (null inicialmente)
- Se muestra confirmación de registro exitoso
- El usuario es redirigido a login o a pantalla home según resultado
- Email de confirmación se envía (simulado o real)

**Prototipo:** P-01

---

### US-02: Inicio de Sesión y Autenticación

**Número:** US-02  
**Rol:** Cualquier usuario  
**Nombre historia:** Inicio de Sesión y Autenticación  
**Puntos de historia estimados:** 5  
**Programador responsable:** 24100514 – Henry Aaron Tovar Landa

**Descripción:**  
Como usuario registrado deseo iniciar sesión con mi correo y contraseña con la finalidad de acceder a mi cuenta y ver el home según mi rol.

**Criterios de aceptación:**
- Pantalla de login (P-02) solicita correo y contraseña
- Al enviar, se valida contra Firebase Authentication
- Si credenciales son válidas, se carga documento de usuario y se guarda UID en sesión local (SharedPreferences/DataStore)
- Se redirige a home según rol (DT → P-03, Jugador → P-04, Padre → P-05, Admin → P-06)
- Si credenciales inválidas, se muestra error "Correo o contraseña incorrectos"
- Botón "Olvidé mi contraseña" redirige a US-03
- Sesión persiste al cerrar y reabrir app (verificado con reapertura)
- Logout limpia sesión y redirige a login

**Prototipo:** P-02

---

### US-03: Recuperación de Contraseña

**Número:** US-03  
**Rol:** Cualquier usuario  
**Nombre historia:** Recuperación de Contraseña  
**Puntos de historia estimados:** 3  
**Programador responsable:** 24100514 – Henry Aaron Tovar Landa

**Descripción:**  
Como usuario olvidé mi contraseña deseo poder recuperarla mediante un correo de restablecimiento con la finalidad de volver a acceder a mi cuenta.

**Criterios de aceptación:**
- Desde pantalla login, botón "Olvidé mi contraseña" abre diálogo (P-02)
- Usuario ingresa email registrado
- Se envía correo de restablecimiento vía Firebase (o simulado en desarrollo)
- Se muestra confirmación "Revisa tu correo para instrucciones"
- Link en correo abre app con token válido (deep link o reset flow)
- Usuario ingresa nueva contraseña (mínimo 8 caracteres) y confirmación
- Se actualiza en Firebase Authentication
- Redirige a login para confirmar con nuevas credenciales

**Prototipo:** P-02

---

### US-04: Crear y Gestionar Equipo

**Número:** US-04  
**Rol:** Administrador / Entrenador  
**Nombre historia:** Crear y Gestionar Equipo  
**Puntos de historia estimados:** 8  
**Programador responsable:** 24100514 – Henry Aaron Tovar Landa

**Descripción:**  
Como administrador o entrenador deseo crear y gestionar un equipo con categoría, nombre y datos básicos con la finalidad de organizar jugadores por categoría (sub-10, sub-15, primera).

**Criterios de aceptación:**
- Admin/DT accede a sección "Equipos" en home (P-07)
- Botón "Crear Equipo" abre formulario (P-07a)
- Formulario solicita: nombre del equipo, categoría (dropdown: sub-10, sub-15, primera, otra), descripción, año/ciclo
- Al crear, se registra en `academies/{uid}/teams/{teamId}` con: name, category, description, year, createdBy (UID del creador), createdAt
- Se muestra lista de equipos creados (P-07)
- Cada equipo permite editar datos, agregar/remover jugadores
- Al seleccionar equipo, se abre detalle (P-07) con: nombre, categoría, jugadores registrados, próximos entrenamientos, próximo partido
- Botón "Editar Equipo" permite cambiar nombre, categoría, descripción
- Confirmación de cambios guarda en Firestore

**Prototipo:** P-07, P-07a, P-07

---

### US-05: Registrar Jugador en Equipo

**Número:** US-05  
**Rol:** Administrador / Entrenador  
**Nombre historia:** Registrar Jugador en Equipo  
**Puntos de historia estimados:** 5  
**Programador responsable:** 24100514 – Henry Aaron Tovar Landa

**Descripción:**  
Como entrenador/admin deseo registrar un jugador en mi equipo con su información básica con la finalidad de gestionar la alineación, asistencia y convocatoria.

**Criterios de aceptación:**
- DT/Admin abre equipo (P-07) y selecciona "Agregar Jugador"
- Opciones: crear nuevo usuario (si no existe) o buscar usuario existente por email
- Si crea nuevo: solicita nombre, email, fecha de nacimiento, género
- Si busca: lista usuarios tipo "Jugador" y selecciona
- Al registrar, se guarda relación en `teams/{teamId}/players/{playerId}` con: playerId, joinDate, status (activo), posición (editable después)
- Se actualiza documento de usuario con `team_ids: [...]` para referencia rápida
- Se muestra lista actualizada de jugadores del equipo (P-08)
- Cada jugador en la lista permite: ver perfil, editar posición, remover del equipo

**Prototipo:** P-07, P-08

---

### US-06: Ver y Editar Perfil de Jugador

**Número:** US-06  
**Rol:** Jugador / Entrenador / Padre  
**Nombre historia:** Ver y Editar Perfil de Jugador  
**Puntos de historia estimados:** 5  
**Programador responsable:** 24100514 – Henry Aaron Tovar Landa

**Descripción:**  
Como jugador deseo ver y editar mi perfil incluyendo posición, datos físicos, foto de perfil con la finalidad de mantener información actualizada y presentarme correctamente.

**Criterios de aceptación:**
- Jugador accede a su perfil desde home (P-04)
- Se muestra: nombre, email, fecha nacimiento, género, posición, altura (cm), peso (kg), foto de perfil
- Botón "Editar Perfil" abre formulario editable (P-09)
- Campos editables: posición (dropdown), altura, peso, foto (camera o galería)
- DT puede ver el mismo perfil en P-09 (lectura + edición de posición si lo necesita)
- Padre puede ver perfil de hijo (P-09) en modo lectura (excepto datos sensibles de peso/altura según privacidad)
- Foto se sube a Firebase Storage en `users/{uid}/profile/photo`
- Cambios se guardan en `users/{uid}` con updateTimestamp
- Se muestra confirmación "Perfil actualizado"

**Prototipo:** P-04, P-09

---

### US-07: Registrar Contacto de Emergencia

**Número:** US-07  
**Rol:** Jugador / Padre  
**Nombre historia:** Registrar Contacto de Emergencia  
**Puntos de historia estimados:** 3  
**Programador responsable:** 24100514 – Henry Aaron Tovar Landa

**Descripción:**  
Como jugador o padre deseo registrar un contacto de emergencia (nombre, teléfono, relación) con la finalidad de que la academia pueda contactar en caso de necesidad.

**Criterios de aceptación:**
- Jugador/Padre accede a sección "Contacto de Emergencia" (P-09 o sección propia)
- Formulario solicita: nombre del contacto, teléfono, relación (padre, madre, hermano, otro)
- Solo Padre autorizado puede editar; Jugador mayor de edad también puede hacerlo
- Se guarda en `users/{uid}/emergencyContact` con: name, phone, relationship, updatedAt
- Se muestra confirmación "Contacto guardado"
- Este dato es visible para DT/Admin pero NO expuesto en comunidad pública

**Prototipo:** P-09

---

### US-08: Asignar Padre/Apoderado a Jugador

**Número:** US-08  
**Rol:** Administrador / Padre  
**Nombre historia:** Asignar Padre/Apoderado a Jugador  
**Puntos de historia estimados:** 5  
**Programador responsable:** 24100514 – Henry Aaron Tovar Landa

**Descripción:**  
Como administrador o padre deseo asignarme como apoderado de un jugador (hijo) con la finalidad de supervisar su información y recibir notificaciones relevantes.

**Criterios de aceptación:**
- Admin accede a "Usuarios" (P-08) y busca usuario tipo "Padre"
- Padre completa registro seleccionando su(s) hijo(s) por email (US-01)
- Sistema valida que el hijo existe en base de datos
- Se registra relación en `guardians/{guardianId}/players` con: playerId, relationship (padre, madre, otro), authorizedAt
- Se actualiza `users/{playerId}/guardians: [...]`
- Padre puede acceder a información de su hijo: perfil, asistencias, convocatorias, estadísticas
- Notificaciones sobre hijo se envían a email/push del padre
- Padre recibe confirmación "Vínculo creado con [Hijo]"

**Prototipo:** P-08

---

### US-09: Planificar Sesión de Entrenamiento

**Número:** US-09  
**Rol:** Entrenador / Administrador  
**Nombre historia:** Planificar Sesión de Entrenamiento  
**Puntos de historia estimados:** 8  
**Programador responsable:** 24100478 – Julia Jhamilett Rojas Estrada

**Descripción:**  
Como entrenador deseo planificar una sesión de entrenamiento con fecha, hora, duración y ejercicios asignados con la finalidad de organizar los entrenamientos del equipo.

**Criterios de aceptación:**
- DT accede a "Entrenamientos" desde home (P-11)
- Botón "Planificar Sesión" abre formulario (P-11a)
- Campos: equipo (dropdown), fecha, hora inicio, duración (minutos), objetivo general (ej: "mejora de pases"), notas
- DT selecciona ejercicios de librería o crea nuevos (US-10)
- Para cada ejercicio: nombre, descripción, duración, objetivo específico, observaciones
- Se guarda en `trainingSessions/{sessionId}` con: teamId, date, startTime, duration, objective, exercises: [{...}], createdBy, createdAt
- Se muestra lista de sesiones planificadas (P-11)
- Notificación automática se envía a jugadores del equipo 24h antes
- DT puede editar sesión si no ha comenzado
- Una vez iniciada, DT puede registrar asistencia (US-11)

**Prototipo:** P-11, P-11a

---

### US-10: Agregar Ejercicios a Librería Reutilizable

**Número:** US-10  
**Rol:** Entrenador / Administrador  
**Nombre historia:** Agregar Ejercicios a Librería Reutilizable  
**Puntos de historia estimados:** 5  
**Programador responsable:** 24100478 – Julia Jhamilett Rojas Estrada

**Descripción:**  
Como entrenador deseo agregar ejercicios a una librería reutilizable con la finalidad de no reingresar los mismos ejercicios en cada sesión de entrenamiento.

**Criterios de aceptación:**
- DT accede a "Librería de Ejercicios" (P-11b)
- Botón "Agregar Ejercicio" abre formulario (P-11b)
- Campos: nombre, descripción, duración (minutos), objetivo (fuerza, resistencia, técnica, táctica, flexibilidad), imagen/video (opcional), notas
- Se guarda en `exerciseLibrary/{academyId}/exercises/{exerciseId}` con: name, description, duration, objective, imageUrl, notes, createdBy, createdAt
- Se muestra lista de ejercicios creados
- Cada ejercicio permite: editar, duplicar, eliminar
- Al planificar entrenamiento (US-09), DT puede seleccionar ejercicios de librería o buscar por objetivo
- Librería es compartida entre todos los DT de la academia

**Prototipo:** P-11b, P-11b

---

### US-11: Registrar Asistencia a Entrenamiento

**Número:** US-11  
**Rol:** Entrenador  
**Nombre historia:** Registrar Asistencia a Entrenamiento  
**Puntos de historia estimados:** 5  
**Programador responsable:** 24100478 – Julia Jhamilett Rojas Estrada

**Descripción:**  
Como entrenador deseo registrar la asistencia y participación de cada jugador en el entrenamiento con la finalidad de llevar historial actualizado.

**Criterios de aceptación:**
- DT abre sesión de entrenamiento iniciada (P-12)
- Se muestra lista de jugadores del equipo convocados
- Para cada jugador, DT marca: Presente, Ausente, Justificado
- Si Ausente/Justificado, campo de motivo (opcional)
- Permite registrar participación: Participó completo, Participó parcial, No participó (lesión, otra)
- Se guarda en `trainingSessions/{sessionId}/attendance/{playerId}` con: status (present/absent/justified), participation (full/partial/none), reason, recordedAt, recordedBy
- Notificación opcional se envía a padre del jugador si ausencia no justificada
- Se muestra confirmación "Asistencia registrada"
- DT puede corregir registro antes de cerrar sesión

**Prototipo:** P-12

---

### US-12: Ver Historial de Asistencia

**Número:** US-12  
**Rol:** Jugador / Padre / Entrenador  
**Nombre historia:** Ver Historial de Asistencia  
**Puntos de historia estimados:** 3  
**Programador responsable:** 24100478 – Julia Jhamilett Rojas Estrada

**Descripción:**  
Como jugador o padre deseo ver el historial de asistencia a entrenamientos con la finalidad de conocer el desempeño en la participación.

**Criterios de aceptación:**
- Jugador accede a "Mi Asistencia" (P-13)
- Se muestra tabla con: fecha, sesión, estado (Presente/Ausente/Justificado), participación, motivo (si aplica)
- Filtros: mes, año, equipo
- Se calcula automáticamente: total sesiones, total asistencias, porcentaje asistencia
- Padre puede ver historial de su hijo (P-13)
- DT puede ver historial de cualquier jugador del equipo
- Datos se cargan desde `trainingSessions/{sessionId}/attendance` agrupados por jugador

**Prototipo:** P-13

---

### US-13: Convocar Jugadores a Partido

**Número:** US-13  
**Rol:** Entrenador / Administrador  
**Nombre historia:** Convocar Jugadores a Partido  
**Puntos de historia estimados:** 5  
**Programador responsable:** 24100478 – Julia Jhamilett Rojas Estrada

**Descripción:**  
Como entrenador deseo convocar a jugadores específicos para un partido y solicitar confirmación de disponibilidad con la finalidad de conocer quién puede asistir.

**Criterios de aceptación:**
- DT accede a "Partidos" (P-14) y selecciona "Nueva Convocatoria"
- Abre formulario (P-14a): equipo, fecha/hora del partido, rival (nombre), ubicación, categoría
- DT selecciona jugadores a convocar de lista del equipo (checkboxes)
- Opción "Convocar a todos" (atajo)
- Se guarda en `callups/{callupId}` con: teamId, matchDate, opponent, location, category, convocatedPlayers: [{playerId, status: pending}], createdBy, createdAt, deadline (fecha/hora límite para confirmar)
- Se envía notificación a cada jugador convocado (push + email)
- Se muestra pantalla de resumen de convocatoria
- DT puede editar convocatoria si aún está abierta (antes de fecha partido)

**Prototipo:** P-14, P-14a

---

### US-14: Confirmar Disponibilidad para Partido

**Número:** US-14  
**Rol:** Jugador  
**Nombre historia:** Confirmar Disponibilidad para Partido  
**Puntos de historia estimados:** 3  
**Programador responsable:** 24100478 – Julia Jhamilett Rojas Estrada

**Descripción:**  
Como jugador convocado deseo confirmar o rechazar mi disponibilidad para el partido con la finalidad de que el entrenador sepa con quién cuenta.

**Criterios de aceptación:**
- Jugador recibe notificación de convocatoria (push, email, o accede a "Mis Convocatorias" P-15)
- Se muestra detalles: fecha/hora, rival, ubicación, categoría
- Botones: "Confirmo mi asistencia", "No puedo asistir"
- Si no puede, campo opcional de motivo (lesión, compromiso personal, otro)
- Se actualiza `callups/{callupId}/convocatedPlayers/{playerId}` con: status (confirmed/declined), reason (si aplica), respondedAt
- Si padre debe autorizar (menor de edad), notificación se envía al padre para confirmar
- Se muestra confirmación al jugador
- DT puede ver estado de confirmaciones en tiempo real (P-14b)

**Prototipo:** P-15, P-14b

---

### US-15: Crear Alineación (Titulares y Suplentes)

**Número:** US-15  
**Rol:** Entrenador  
**Nombre historia:** Crear Alineación (Titulares y Suplentes)  
**Puntos de historia estimados:** 8  
**Programador responsable:** 24100478 – Julia Jhamilett Rojas Estrada

**Descripción:**  
Como entrenador deseo crear la alineación del partido definiendo titulares y suplentes con sus posiciones con la finalidad de organizar el equipo antes del partido.

**Criterios de aceptación:**
- DT abre convocatoria confirmada (P-14b) y selecciona "Crear Alineación"
- Se abre formulario visual (P-16) con campo de fútbol (11 posiciones: 1 portero, 4 defensas, 4 mediocampistas, 2 delanteros)
- DT arrastra jugadores confirmados a posiciones (drag-and-drop o seleccionar posición)
- Validación: máximo 1 portero, cumplir formación típica, mínimo 7 titulares
- Campo "Suplentes": lista de jugadores restantes en orden de preferencia
- Se guarda en `lineups/{lineupId}` con: matchId/callupId, match_date, formation (ej: "4-3-3"), starters: [{playerId, position}], substitutes: [{playerId, order}], createdBy, createdAt
- Se muestra alineación final en pantalla (P-16)
- Notificación se envía a jugadores informando si son titular o suplente
- DT puede editar alineación hasta 1 hora antes del partido (configurable)

**Prototipo:** P-16

---

### US-16: Ver Alineación como Jugador/Padre

**Número:** US-16  
**Rol:** Jugador / Padre / Público  
**Nombre historia:** Ver Alineación como Jugador/Padre  
**Puntos de historia estimados:** 3  
**Programador responsable:** 24100478 – Julia Jhamilett Rojas Estrada

**Descripción:**  
Como jugador o padre deseo ver la alineación oficial del partido con la finalidad de confirmar la posición del jugador.

**Criterios de aceptación:**
- Jugador accede a "Mi Próximo Partido" (P-17) o desde notificación
- Se muestra campo visual con alineación: titulares en cancha, suplentes en banco
- Muestra nombre, número (si aplica), posición de cada jugador
- Destaca al jugador propio
- Información: rival, fecha/hora, ubicación, árbitro (si disponible)
- Padre puede ver alineación de su hijo (P-17)
- Vista pública muestra solo nombres y posiciones (sin datos sensibles)
- La alineación se actualiza en tiempo real si el DT hace cambios (antes del partido)

**Prototipo:** P-17

---

### US-17: Registrar Evento de Partido en Tiempo Real

**Número:** US-17  
**Rol:** Entrenador / Operador Autorizado  
**Nombre historia:** Registrar Evento de Partido en Tiempo Real  
**Puntos de historia estimados:** 13  
**Programador responsable:** 24100514 – Henry Aaron Tovar Landa

**Descripción:**  
Como entrenador o operador autorizado deseo registrar eventos del partido en vivo (goles, faltas, cambios, tarjetas) con minuto exacto, equipo y jugador involucrado con la finalidad de mantener un registro completo y preciso del partido.

**Criterios de aceptación:**
- DT abre pantalla "Partido en Vivo" (P-18) antes/durante partido
- Cronómetro en pantalla: muestra minuto actual (manual o automático)
- Botones de tipos de evento según catálogo (Gol, Tarjeta, Cambio, Falta, Penal, Saque, etc.)
- Al seleccionar evento (ej: Gol):
  - Solicita: minuto (pre-lleno con minuto actual, editable), equipo (local/visitante), jugador (selección de lista de campo), tipo de gol (pie/cabeza/penal), asistencia (opcional), notas (opcional)
  - Validación: jugador debe estar en alineación o haber ingresado como cambio
  - Se guarda en `matches/{matchId}/events/{eventId}` con: type, minute, team, playerId, details: {...}, createdBy, createdAt, version: 1, voided: false
- Eventos "Cambio": solicita jugador sale, jugador entra, minuto
- Eventos "Tarjeta": solicita color (amarilla/roja), minuto, jugador, razón
- Evento "Inicio Partido" se registra automáticamente; "Fin Partido" cierra registro
- Se muestra lista de eventos registrados en orden cronológico (P-18)
- Cada evento en lista muestra: minuto, tipo, equipo, jugador (si aplica), opción editar/anular
- Sincronización en tiempo real a todos los dispositivos conectados (Firestore listeners)
- Sistema funciona offline: cola local de eventos, sincroniza al reconectar

**Prototipo:** P-18

---

### US-18: Corregir o Anular Evento de Partido

**Número:** US-18  
**Rol:** Entrenador / Operador Autorizado  
**Nombre historia:** Corregir o Anular Evento de Partido  
**Puntos de historia estimados:** 8  
**Programador responsable:** 24100514 – Henry Aaron Tovar Landa

**Descripción:**  
Como entrenador deseo corregir o anular un evento registrado erróneamente con la finalidad de mantener la precisión del registro y preservar la trazabilidad de cambios.

**Criterios de aceptación:**
- Desde pantalla "Partido en Vivo" (P-18), DT selecciona evento de la lista
- Opciones: "Editar Evento" o "Anular Evento"
- Si Editar:
  - Se abre formulario con campos originales pre-llenos
  - Permitir cambiar minuto, jugador, detalles
  - Al guardar: se actualiza `matches/{matchId}/events/{eventId}` con updatedBy (UID), updatedAt, version (incrementa), y se registra changelog en subcollection `events/{eventId}/edits`
- Si Anular:
  - Diálogo solicita razón de anulación (ej: "Registrado erróneamente", "DT lo cambió")
  - Al confirmar: se actualiza evento con voided: true, voidedBy (UID), voidedAt, voidReason
  - Evento anulado NO se elimina; sigue visible en timeline con indicación "ANULADO"
  - Cambios se sincronizan en tiempo real
- Validación: solo el DT/operador que lo creó o admin pueden modificar eventos después de 5 minutos de creación (configurable)
- Historial completo de cambios es auditable en `events/{eventId}/edits`

**Prototipo:** P-18

---

### US-19: Ver Scoreboard y Timeline en Tiempo Real

**Número:** US-19  
**Rol:** Cualquier usuario conectado  
**Nombre historia:** Ver Scoreboard y Timeline en Tiempo Real  
**Puntos de historia estimados:** 8  
**Programador responsable:** 24100514 – Henry Aaron Tovar Landa

**Descripción:**  
Como jugador, padre, o espectador deseo ver el marcador, cronología de eventos y estado del partido en vivo con la finalidad de seguir el partido en tiempo real sin necesidad de recargara la pantalla.

**Criterios de aceptación:**
- Pantalla "Ver Partido en Vivo" (P-19) muestra:
  - **Scoreboard**: equipos (local vs visitante), marcador actual, cronómetro (minutos), estado (antes de iniciar, en vivo, finalizado)
  - **Timeline de eventos**: lista de eventos en orden cronológico (de abajo hacia arriba), cada uno muestra minuto, tipo evento, equipo, jugador, icono/emoji según tipo
  - Goles destacados (ej: fondo verde)
  - Tarjetas destacadas (ej: icono de tarjeta)
  - Cambios mostrados claramente (jugador sale - jugador entra)
- Sincronización en tiempo real: cuando un evento se registra, aparece en <2 segundos en todos los dispositivos viendo P-19
- Listener Firestore escucha `matches/{matchId}/events` y actualiza UI automáticamente
- Si hay pérdida de conexión, se muestra estado offline y reintenta conexión cada 5 segundos
- Refresh manual con botón "Actualizar"
- Responsive: se adapta a pantalla de teléfono (timeline vertical) y tablet (scoreboard + timeline lado a lado)

**Prototipo:** P-19

---

### US-20: Generar Resumen Narrativo de Partido con IA

**Número:** US-20  
**Rol:** Sistema (automatizado)  
**Nombre historia:** Generar Resumen Narrativo de Partido con IA  
**Puntos de historia estimados:** 13  
**Programador responsable:** 24100514 – Henry Aaron Tovar Landa

**Descripción:**  
Como sistema deseo generar automáticamente un resumen narrativo del partido a partir de los eventos registrados con la finalidad de producir un reporte textual confiable basado solo en datos verificados.

**Criterios de aceptación:**
- Al finalizar partido (evento "Fin Partido" registrado), se activa función Cloud Function de Firebase
- Cloud Function lee todos los eventos de `matches/{matchId}/events` donde voided = false
- Extrae datos factales: goles (minuto, jugador, tipo), tarjetas (tipo, jugador, minuto), cambios, balance de posesión (si se registran pases)
- Llama API externa (OpenAI/Claude/similar) con prompt estructurado:
  ```
  "Genera un resumen narrativo de un partido de fútbol basado ÚNICAMENTE en estos eventos:
  [JSON de eventos]
  
  Requisitos:
  - Solo hechos verificables (goles, tarjetas, cambios)
  - Separar claramente hechos de interpretación (ej: 'Fue un gol bien ejecutado' es interpretación)
  - NO inventar jugadas, posiciones ni tácticas
  - NO sugerir alineación alternativa ni crítica a jugadores
  - Incluir marcador final, protagonistas, momentos críticos
  - Máximo 300 palabras
  - Tono profesional, amigable
  - NO nombrar menores de edad directamente (usar 'Jugador #' o iniciales)
  ```
- Resumen se guarda en `aiSummaries/{matchId}` con: text, sourceEventIds: [...], status: 'draft', generatedAt, generatedBy: 'system'
- Se notifica al DT: "Resumen IA generado. Revisa y aprueba en [link]"
- Si API falla, se registra error y se notifica a admin; se puede reintentar manualmente
- Versionado: cada regeneración es una nueva entrada (no sobrescribe)

**Prototipo:** (backend, sin UI directa; se activa al finalizar partido)

---

### US-21: Revisar y Aprobar Resumen IA

**Número:** US-21  
**Rol:** Entrenador  
**Nombre historia:** Revisar y Aprobar Resumen IA  
**Puntos de historia estimados:** 5  
**Programador responsable:** 24100478 – Julia Jhamilett Rojas Estrada

**Descripción:**  
Como entrenador deseo revisar el resumen narrativo generado por IA, detectar errores o información inventada, y aprobarlo o solicitar corrección con la finalidad de garantizar precisión antes de publicar.

**Criterios de aceptación:**
- DT accede a "Resumen del Partido" (P-20) después de finalizar partido
- Se muestra:
  - Resumen narrativo en texto editable
  - Eventos fuente listados al costado (referencias)
  - Indicación de párrafos que son "hechos" vs "interpretación"
  - Botones: "Aprobar", "Solicitar Corrección", "Rechazar"
- Si Aprobar:
  - Se actualiza `aiSummaries/{matchId}` con status: 'approved', approvedBy (UID DT), approvedAt
  - Resumen se publica en comunidad (P-25) y en perfil del partido (visible públicamente)
  - Notificación a admin: "Resumen aprobado por [DT]"
- Si Solicitar Corrección:
  - DT ingresa feedback específico (ej: "Falta gol de Pérez en minuto 45", "No inventés posiciones")
  - Se guarda en `aiSummaries/{matchId}/feedback` con: message, requestedBy, requestedAt
  - Notificación a admin con feedback; se puede regenerar resumen con feedback
- Si Rechazar:
  - Motivo: "Información inventada", "Errores graves", "Otro"
  - Resumen no se publica; admin/sistema intenta regenerar
- Pantalla permite editar directamente texto si hay errores menores (typos)
- Al salvar cambios, se registra versión anterior en histórico

**Prototipo:** P-20

---

### US-22: Ver Estadísticas Acumuladas del Jugador

**Número:** US-22  
**Rol:** Jugador / Padre / Entrenador  
**Nombre historia:** Ver Estadísticas Acumuladas del Jugador  
**Puntos de historia estimados:** 5  
**Programador responsable:** 24100478 – Julia Jhamilett Rojas Estrada

**Descripción:**  
Como jugador o padre deseo ver las estadísticas acumuladas de participación en partidos con la finalidad de conocer el desempeño (goles, tarjetas, participación).

**Criterios de aceptación:**
- Jugador accede a "Mis Estadísticas" (P-21)
- Se muestra dashboard con:
  - Partidos jugados (totales y por categoría)
  - Goles marcados
  - Tarjetas amarillas y rojas
  - Partidos como titular vs suplente
  - Participación (completa, parcial, sustituido)
  - Promedio de goles por partido
- Filtros: equipo, categoría, período (mes/año/temporada completa)
- Gráficos (opcional): línea de goles por mes, torta de participación
- Padre puede ver estadísticas de su hijo (P-21)
- Datos se calculan dinámicamente desde `matches/{matchId}/events` y `lineups/{lineupId}`
- Vista pública muestra solo nombre y goles/tarjetas sin datos sensibles
- Actualización en tiempo real si se registran nuevos eventos

**Prototipo:** P-21

---

### US-23: Crear Anuncio de Academia

**Número:** US-23  
**Rol:** Administrador / Entrenador  
**Nombre historia:** Crear Anuncio de Academia  
**Puntos de historia estimados:** 3  
**Programador responsable:** 24100478 – Julia Jhamilett Rojas Estrada

**Descripción:**  
Como administrador o entrenador deseo crear anuncios (cambios de horario, suspensiones, eventos) con la finalidad de comunicar información importante a todos los usuarios.

**Criterios de aceptación:**
- Admin/DT accede a "Anuncios" (P-22)
- Botón "Crear Anuncio" abre formulario (P-22a)
- Campos: título, contenido, fecha publicación (hoy o programada), destino (toda academia, un equipo, grupo específico), adjunto (opcional)
- Se guarda en `announcements/{announcementId}` con: title, content, authorId, createdAt, scheduledFor (si aplica), targetAudience, imageUrl (opcional)
- Se muestra lista de anuncios (P-22)
- Notificación automática se envía a usuarios en audiencia (push + email)
- Anuncios anteriores quedan en historial
- Permite editar/eliminar anuncios no publicados
- Anuncios publicados no se pueden eliminar (solo marcar como archivado)

**Prototipo:** P-22, P-22a

---

### US-24: Recibir Notificaciones

**Número:** US-24  
**Rol:** Cualquier usuario  
**Nombre historia:** Recibir Notificaciones  
**Puntos de historia estimados:** 5  
**Programador responsable:** 24100478 – Julia Jhamilett Rojas Estrada

**Descripción:**  
Como usuario deseo recibir notificaciones push y email sobre eventos relevantes (convocatoria, anuncios, cambios, resultados) con la finalidad de estar informado sin tener que revisar constantemente la app.

**Criterios de aceptación:**
- Sistema configura Firebase Cloud Messaging (FCM) con token por dispositivo
- Notificaciones se envían por:
  - **Convocatoria**: cuando jugador es convocado
  - **Alineación**: cuando alineación está lista (titular vs suplente)
  - **Anuncio**: cuando se publica anuncio relevante para el usuario
  - **Recordatorio entrenamiento**: 24h antes de sesión
  - **Cambios**: cuando se edita/anula evento durante partido
  - **Resultado**: cuando partido termina (resumen)
  - **Padre**: asistencia/ausencia del hijo, eventos de su equipo
- Usuario puede configurar preferencias de notificación (P-24): on/off por tipo, horario no molestar
- Las notificaciones se guardan en centro de notificaciones en app (P-24) con historial
- Click en notificación abre detalle relevante (ej: notificación de convocatoria abre P-15)
- Notificaciones sin lectura se marcan con badge
- Se envía email resumen si usuario no abre app en X días (configurable)

**Prototipo:** P-24

---

### US-25: Crear Post en Comunidad

**Número:** US-25  
**Rol:** Entrenador / Jugador / Padre  
**Nombre historia:** Crear Post en Comunidad  
**Puntos de historia estimados:** 5  
**Programador responsable:** 24100478 – Julia Jhamilett Rojas Estrada

**Descripción:**  
Como entrenador, jugador o padre deseo crear posts en la comunidad para compartir experiencias, resultados, celebraciones con la finalidad de fomentar interacción entre miembros de la academia.

**Criterios de aceptación:**
- Usuario accede a "Comunidad" (P-25)
- Botón "Crear Post" abre formulario (P-25a)
- Campos: contenido (texto), foto/video (opcional), audiencia (academia, equipo, público -si aplica-)
- Tags opcionales: #categoría, #evento, #jugador
- Menciones: @usuario (autocompletar)
- Se guarda en `posts/{postId}` con: authorId, content, mediaUrl (si aplica), audience, tags, createdAt, status: 'active', reportCount: 0
- Se muestra en feed de comunidad (P-25) en orden cronológico inverso (más recientes arriba)
- Validaciones: no permite contenido vacío, máximo 5000 caracteres, máximo 10 MB en media
- Post se publica de inmediato (visible para audiencia indicada)
- Autor puede editar post dentro de 1 hora de creación (con indicación "editado")
- Autor puede eliminar post (solo si no tiene comentarios)
- Notificación opcional se envía a usuarios mencionados

**Prototipo:** P-25, P-25a

---

### US-26: Comentar y Reaccionar a Post

**Número:** US-26  
**Rol:** Entrenador / Jugador / Padre  
**Nombre historia:** Comentar y Reaccionar a Post  
**Puntos de historia estimados:** 3  
**Programador responsable:** 24100478 – Julia Jhamilett Rojas Estrada

**Descripción:**  
Como usuario deseo comentar y reaccionar (emoji) a posts de otros usuarios con la finalidad de participar en conversación de comunidad.

**Criterios de aceptación:**
- Cada post muestra: contador de reacciones, botón "Reaccionar", botón "Comentar", contador de comentarios
- Reacciones: emoji buttons (👍, ❤️, 😂, 😮, 😢, 😡) o selector personalizado
- Al reaccionar: se registra en `posts/{postId}/reactions/{userId}` con: emoji, timestamp
- Contador se actualiza en tiempo real
- Al comentar: se abre sección de comentarios o form inline
- Campo de comentario permite: texto, mención @usuario, emoji
- Se guarda en `posts/{postId}/comments/{commentId}` con: authorId, text, createdAt, status: 'active'
- Comentarios se muestran en hilo debajo del post
- Usuario puede editar comentario propio dentro de 1h, o eliminar
- Notificación se envía al autor del post y usuarios mencionados en comentario
- Cada comentario permite: editar, eliminar, reaccionar

**Prototipo:** P-25

---

### US-27: Publicar Aviso de Prueba/Convocatoria Abierta

**Número:** US-27  
**Rol:** Administrador / Entrenador  
**Nombre historia:** Publicar Aviso de Prueba/Convocatoria Abierta  
**Puntos de historia estimados:** 5  
**Programador responsable:** 24100478 – Julia Jhamilett Rojas Estrada

**Descripción:**  
Como administrador deseo publicar avisos de pruebas o convocatorias abiertas para jugadores interesados con la finalidad de atraer talento a la academia.

**Criterios de aceptación:**
- Admin/DT accede a "Pruebas y Convocatorias" (P-26)
- Botón "Publicar Aviso" abre formulario (P-26)
- Campos: título, descripción, categoría (sub-10, sub-15, primera, otra), posiciones buscadas (multiple select), fecha de prueba, ubicación, contacto (nombre, teléfono verificado), edad mínima/máxima (si aplica), requisitos
- Validaciones: contacto debe ser email/teléfono de admin verificado (anti-phishing), descripción >50 caracteres, campos obligatorios
- Se guarda en `tryouts/{tryoutId}` con: title, description, category, positions, date, location, contact, ageRange, requirements, createdBy, createdAt, status: 'active'
- Se publica en sección "Avisos de Prueba" (P-26, visible para usuarios no logueados si configurado)
- Notificación se envía a jugadores filtrados (búsqueda histórica de preferencias o envío manual)
- Foto/logo de academia (opcional) se muestra en aviso
- Admin/DT puede editar aviso si está abierto
- Al cerrar convocatoria, aviso se muestra como "Cerrado" (archivo)

**Prototipo:** P-26, P-26

---

### US-28: Filtrar Avisos de Prueba

**Número:** US-28  
**Rol:** Jugador (no registrado o registrado)  
**Nombre historia:** Filtrar Avisos de Prueba  
**Puntos de historia estimados:** 3  
**Programador responsable:** 24100478 – Julia Jhamilett Rojas Estrada

**Descripción:**  
Como jugador interesado deseo filtrar avisos de prueba por categoría, posición, fecha y ubicación con la finalidad de encontrar convocatorias relevantes para mí.

**Criterios de aceptación:**
- Pantalla "Avisos de Prueba" (P-26) muestra lista de avisos activos
- Filtros: categoría (dropdown), posición (multi-select), fecha (desde-hasta), ubicación (búsqueda de text)
- Búsqueda: campo de text busca en título y descripción
- Resultados se actualizan dinámicamente al cambiar filtros
- Cada aviso muestra: título, categoría, posiciones, fecha, ubicación, contacto (oculto hasta interés)
- Botón "Más Detalles" abre P-26b con info completa
- Botón "Interesado" permite contactar (si no logueado, redirige a login)
- Si logueado, puede guardar aviso favorito o enviar mensaje de interés al contacto
- Avisos cerrados se muestran con etiqueta "Cerrado" pero aún filtrable
- Notificación de nuevos avisos según preferencias del usuario

**Prototipo:** P-26, P-26b

---

### US-29: Reportar Contenido Inapropiado

**Número:** US-29  
**Rol:** Cualquier usuario registrado  
**Nombre historia:** Reportar Contenido Inapropiado  
**Puntos de historia estimados:** 3  
**Programador responsable:** 24100478 – Julia Jhamilett Rojas Estrada

**Descripción:**  
Como usuario deseo reportar posts, comentarios, reacciones o usuarios por contenido inapropiado (abuso, spam, suplantación) con la finalidad de mantener la comunidad segura.

**Criterios de aceptación:**
- Cada post/comentario muestra ícono "..." (más opciones) con "Reportar" en menú
- Al reportar, se abre diálogo (P-27) solicitando:
  - Motivo: "Lenguaje abusivo", "Spam", "Suplantación", "Acoso", "Contenido sexual", "Violencia", "Otro"
  - Descripción adicional (opcional)
  - Adjuntar screenshot (opcional)
- Se guarda en `reports/{reportId}` con: reportedItemId (postId, commentId, userId), itemType (post/comment/user), reporterId, reason, description, createdAt, status: 'pending', adminNotes: null
- Se muestra confirmación "Reporte enviado. Gracias por mantener la comunidad segura"
- Usuario no puede reportar el mismo contenido dos veces
- Sistema detiene publicación de contenido reportado (oculta después de X reportes simultáneos, pending review)
- Reportes se envían a admin para revisión (P-28)
- Privacidad: identidad del reportante se oculta del reportado

**Prototipo:** P-27

---

### US-30: Moderar y Gestionar Reportes (Admin)

**Número:** US-30  
**Rol:** Administrador  
**Nombre historia:** Moderar y Gestionar Reportes  
**Puntos de historia estimados:** 5  
**Programador responsable:** 24100478 – Julia Jhamilett Rojas Estrada

**Descripción:**  
Como administrador deseo revisar reportes de contenido inapropiado, ocultar/eliminar posts o comentarios, bloquear usuarios con la finalidad de garantizar moderación segura de la comunidad.

**Criterios de aceptación:**
- Admin accede a "Centro de Moderación" (P-28)
- Pestaña "Reportes Pendientes" muestra cola de reportes sin revisar
- Cada reporte muestra: razón, descripción, contenido reportado (preview), reportador (anonimizado), fecha
- Admin puede:
  - **Desestimar reporte**: justificación, status: 'dismissed'
  - **Ocultar contenido**: se marca deleted: true, pero se mantiene en BD; visible solo para autor y admin; status: 'hidden'
  - **Eliminar contenido**: deleted: true, eliminatedBy (admin UID), eliminatedAt, eliminationReason; contenido no visible a nadie
  - **Bloquear usuario**: reporterId → status: 'blocked', blocker (admin UID), blockedAt, reason; usuario no puede postear/comentar; posts previos ocultos
- Bloqueado se notifica al usuario con razón y opción de apelación (enviar email a admin)
- Historia de acción se registra en `reports/{reportId}/actions`: [{action, adminId, timestamp, reason}]
- Estadísticas: dashboard de reportes por razón, velocidad de resolución, usuarios recurrentes
- Reportes resueltos se archivan (filtro "Resolved" en UI)
- Opcionalmente, admin puede ver historial de usuario bloqueado (posts, comentarios, reportes recibidos)

**Prototipo:** P-28

---

## Notas de Implementación
- Todas las historias deben testearse con datos incompletos (campos vacíos, usuarios desconocidos)
- Sincronización en tiempo real requiere Firestore listeners (StateFlow en Kotlin)
- Privacidad: campos sensibles deben tener reglas de seguridad Firestore por rol y edad
- IA: integración con API externa; gestionar latencia y reintentos
- Mensajería: opcional para esta entrega (US-31+ futuro)
