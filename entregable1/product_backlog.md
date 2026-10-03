# Product Backlog - SportPro

## Objetivo General
Desarrollar una aplicación móvil integral para la gestión de academias y equipos de fútbol, que permita administrar jugadores, entrenamientos, partidos, comunicación y comunidad, con resumen narrativo automático de partidos basado en eventos registrados.

## Stakeholders
- **Entrenador/Director Técnico (DT)**: gestión de equipos, entrenamientos, alineación, registro de partidos
- **Jugador (JUG)**: perfil, entrenamientos, convocatoria, estadísticas, comunidad
- **Padre/Apoderado (PAD)**: supervisión de hijo/hija, pagos, notificaciones, datos públicos del hijo
- **Administrador (ADM)**: gestión de academias, usuarios, configuración global

## Supuestos y Reglas de Negocio

### Privacidad y Permisos por Rol

| Información / Acción | DT | Jugador | Padre | Administrador | Público |
|---|---|---|---|---|---|
| Perfil jugador (nombre, posición, altura, peso) | Lectura | Lectura/Edición | Lectura (propio hijo) | Lectura | No |
| Foto perfil | Lectura | Lectura/Edición | Lectura | Lectura | No |
| Contacto emergencia | Lectura | Lectura/Edición | Lectura (propio hijo) | Lectura | No |
| Email/Teléfono (contacto principal) | Lectura | Lectura/Edición | Lectura (propio hijo) | Lectura | No |
| Historial asistencia entrenamientos | Lectura | Lectura (propio) | Lectura (propio hijo) | Lectura | No |
| Estadísticas acumuladas (goles, tarjetas) | Lectura | Lectura (propio) | Lectura (propio hijo) | Lectura | Sí (equipo/ronda) |
| Convocatoria a partidos | Lectura/Creación | Lectura | Notificación (propio hijo) | Lectura | No |
| Disponibilidad confirmada | Lectura | Lectura/Confirmación | No | Lectura | No |
| Alineación (titulares, suplentes) | Lectura/Edición | Lectura | Lectura (propio hijo) | Lectura | Lectura (público) |
| Eventos de partido en vivo | Lectura/Edición | Lectura | Lectura | Lectura | Lectura (pública) |
| Resumen narrativo IA | Lectura/Aprobación | Lectura | Lectura | Lectura | Lectura |
| Avisos de prueba/convocatoria abierta | Lectura/Creación | Lectura/Filtro | Lectura | Lectura/Moderación | Lectura (sin datos sensibles) |
| Anuncios de academia | Lectura/Creación | Lectura | Lectura | Lectura/Creación | Lectura (si público) |
| Comunidad: crear posts | Sí | Sí | Sí (restringido) | Sí | No |
| Comunidad: comentar/reaccionar | Sí | Sí | Sí | Sí | No |
| Moderación/reporte de contenido | Lectura | Lectura/Reporte | Lectura/Reporte | Lectura/Edición/Eliminación | No |
| Mensualidades estado | No | Lectura (propio) | Lectura (propio hijo) | Lectura/Edición | No |
| Crear/Editar equipos | No | No | No | Sí | No |
| Editar datos de academia | No | No | No | Sí | No |

### Criterios de Privacidad
- **Menores de edad (< 18 años)**: datos físicos (altura, peso) y contacto de emergencia solo visibles para padre/apoderado autorizado, DT y administrador. NO mostrar en comunidad pública ni en avisos abiertos.
- **Autorización parental**: cada padre/apoderado solo ve datos de su(s) hijo(s) registrado(s).
- **Datos de contacto**: no se exponen en la comunidad general; solo accesibles dentro del contexto de equipo/academia.
- **Estadísticas públicas**: nombres y goles/tarjetas por categoría/ronda, sin datos sensibles.
- **Foto de perfil**: solo en contexto de equipo autorizado; no indexable/descargable públicamente.

### Comunidad y Moderación

#### Reglas de Visibilidad
- **Posts/Comentarios del DT o Admin**: visibles dentro de su academia a todos los usuarios registrados.
- **Posts de Jugadores**: visibles dentro de su academia y a equipos conectados (autorizados por DT o Admin).
- **Posts de Padre**: solo en contexto de su hijo (no abiertos a toda la comunidad).
- **Avisos de prueba/convocatoria abierta**: publicables solo por DT o Admin, con filtros de categoría y posición; no exponen datos sensibles de menores.

#### Moderación y Bloqueo
- **Reporte de contenido**: cualquier usuario registrado puede reportar post, comentario o reacción inapropiada (lenguaje abusivo, spam, suplantación, acoso).
- **Revisión**: administrador revisa reportes y puede ocultar/eliminar contenido o bloquear usuario.
- **Bloqueo de usuario**: un usuario bloqueado no puede crear posts ni comentarios; sus posts anteriores quedan ocultos.
- **Apelación**: usuario bloqueado puede contactar a administrador para revisar decisión.
- **Prevención de convocatorias engañosas**: avisos de prueba/convocatoria deben incluir DT responsable, teléfono de contacto verificado, ubicación y fecha específica. Admin verifica antes de publicar.

### Catálogo de Eventos de Partido

#### Tipos de Eventos Configurables
Cada tipo especifica campos obligatorios y opcionales:

| Tipo de Evento | Campo Minuto | Campo Equipo | Campo Jugador | Campos Adicionales | Puede Anularse |
|---|---|---|---|---|---|
| **Inicio Partido** | Obligatorio (0) | No | No | Marcador inicial (0-0) | No |
| **Fin Partido** | Obligatorio | No | No | Marcador final | No |
| **Saque de Meta** | Obligatorio | Obligatorio | Obligatorio | Notas (opcional) | Sí |
| **Saque Lateral** | Obligatorio | Obligatorio | Obligatorio | Notas (opcional) | Sí |
| **Tiro de Esquina** | Obligatorio | Obligatorio | Obligatorio | Notas (opcional) | Sí |
| **Falta** | Obligatorio | Obligatorio | Obligatorio (quien cometió) | Tipo falta, jugador afectado (opcional) | Sí |
| **Penal** | Obligatorio | Obligatorio | Obligatorio | Razón, notas | Sí |
| **Gol** | Obligatorio | Obligatorio | Obligatorio (quien marca) | Asistencia (opcional), tipo (cabeza, pie, penal, etc.) | Sí |
| **Tarjeta Amarilla** | Obligatorio | Obligatorio | Obligatorio | Razón falta, notas | Sí |
| **Tarjeta Roja** | Obligatorio | Obligatorio | Obligatorio | Razón, notas | Sí |
| **Fuera de Juego** | Obligatorio | Obligatorio | Obligatorio | Notas | Sí |
| **Cambio (Sustitución)** | Obligatorio | Obligatorio | Obligatorio (sale) | Jugador entra (obligatorio) | Sí |

#### Trazabilidad de Eventos
- **Campos de auditoría en cada evento**:
  - `createdBy` (UID del DT/operador)
  - `createdAt` (timestamp exacto)
  - `updatedBy` (UID de quien modificó, si aplica)
  - `updatedAt` (timestamp de modificación)
  - `voidedBy` (UID de quien anuló, si aplica)
  - `voidedAt` (timestamp de anulación)
  - `voidReason` (motivo de anulación o corrección)
  - `version` (número de versión del evento)
- **Registro de correcciones**: al editar o anular, se mantiene el histórico; nunca se elimina la evidencia original.
- **Sincronización en tiempo real**: todos los dispositivos conectados reciben cambios en <2 segundos.

### Alcance del Resumen Narrativo con IA

#### Fuente de Datos
- **ÚNICA fuente**: eventos registrados en tiempo real (tabla `matches/{id}/events`).
- **NO**: fotografías, comentarios, mensajes, suposiciones, datos del entrenamiento previo.
- **Prohibido**: inventar jugadas, asumir posiciones, generar tácticas alternativas, sugerir alineación.

#### Estructura del Resumen
El resumen debe incluir:
1. **Datos factales**:
   - Marcador final
   - Goles por jugador y minuto
   - Tarjetas (amarillas/rojas) por jugador
   - Cambios realizados y minutos
2. **Interpretación controlada** (marcada como tal):
   - Ritmo del partido (lento/normal/acelerado)
   - Posesión aproximada (si se registran pases)
   - Momentos críticos (lluvia de goles, juego físico intenso, etc.)
3. **Restricciones**:
   - NO mencionar nombres de menores de edad directamente en narrativa pública (reemplazar por "Jugador #" o iniciales)
   - Separar claramente hechos ("Se anotó gol") de interpretación ("Fue un gol bien ejecutado")
   - NO generar tácticas alternativas o críticas personales a jugadores

#### Proceso de Aprobación
1. IA genera borrador de resumen
2. DT/Coach revisa y puede:
   - Aprobar como está
   - Rechazar con motivo (factualmente incorrecto, información inventada, expone menores, etc.)
   - Solicitar corrección específica
3. Resumen aprobado se publica en comunidad; rechazado vuelve a IA con feedback
4. **Historial de versiones**: mantener todas las versiones y cambios solicitados

## Priorización (MoSCoW)

### MUST (Core del Negocio - Semana 5+7)
- Autenticación y roles (DT, Jugador, Padre, Admin)
- Gestión de equipos y categorías
- Perfil de jugador (posición, datos físicos, contacto)
- Planificación de entrenamientos y asistencia
- Convocatoria y confirmación de disponibilidad
- Alineación (titulares, suplentes)
- Registro de partido en vivo con eventos
- Sincronización en tiempo real
- Resumen narrativo con IA (básico)
- Anuncios y notificaciones

### SHOULD (Semana 12)
- Comunidad (posts, comentarios, reacciones)
- Moderación y reporte
- Avisos de prueba/convocatoria abierta
- Estadísticas acumuladas
- Resumen narrativo IA mejorado (separación hechos/interpretación)
- Mensualidades simuladas (estado)

### COULD (Futuro)
- Chat directo entre usuarios
- Exportar resumen narrativo a PDF
- Análisis predictivo de rendimiento
- Integración con sistemas de ticketing
- App web responsive (además de mobile)

### WON'T (Fuera de Scope)
- Pagos reales o integración con pasarelas
- Chatbot RAG o asistente de conversación
- Sugerencias de alineación automáticas
- Streaming de video de partidos
- Integración externa con federaciones (IFAB, etc.)

## Supuestos Técnicos
- Firebase disponible y configurado (Auth, Firestore, Storage, FCM)
- Firestore puede soportar eventos en tiempo real con <2 seg de latencia
- Dispositivos Android >= 8.0 (API 26)
- Conectividad móvil intermitente; la app debe funcionar offline (caché local) y sincronizar al reconectar
- IA externa (p.ej. OpenAI, Claude API) disponible para generar resumen narrativo
- Máximo 200 jugadores por academia en beta; máximo 50 eventos por partido

## Criterios de Aceptación Generales
- Todas las funcionalidades deben testearse con datos incompletos (jugador desconocido, evento sin minuto exacto)
- Privacidad: no exponer datos de menores en contextos públicos
- Trazabilidad: todo cambio debe ser auditable
- Sincronización: evento registrado en un dispositivo debe verse en todos los conectados en <2 segundos
- Narrativa IA: revisar que NO inventa jugadas y separar hechos de interpretación
