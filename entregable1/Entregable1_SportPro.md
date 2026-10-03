# SportPro – Entregable 1 (Semana 5)

**Proyecto:** Desarrollo de Aplicaciones Móviles  
**Universidad:** ESAN  
**Integrantes:**
- 24100514 – Henry Aaron Tovar Landa
- 24100478 – Julia Jhamilett Rojas Estrada

**Fecha:** 2 de Octubre de 2026

---

## Tabla de Contenidos

1. [Objetivo del Proyecto](#1-objetivo-del-proyecto)
2. [Stakeholders](#2-stakeholders)
3. [Reglas de Negocio](#3-reglas-de-negocio-y-supuestos)
4. [Privacidad y Permisos](#4-privacidad-y-permisos-por-rol)
5. [Comunidad y Moderación](#5-comunidad-y-moderación)
6. [Catálogo de Eventos](#6-catálogo-de-eventos-de-partido)
7. [Alcance IA](#7-alcance-del-resumen-narrativo-con-ia)
8. [Product Backlog](#8-product-backlog)
9. [Historias de Usuario](#9-historias-de-usuario)
10. [Modelo de Datos](#10-modelo-de-datos)
11. [Prototipos](#11-prototipos-navegables)

---

## 1. Objetivo del Proyecto

**SportPro** es una aplicación móvil integral para la gestión de académias y equipos de fútbol. Permite administrar jugadores, entrenamientos, partidos, comunicación y comunidad, e incorpora como único componente de IA la generación de un resumen narrativo del partido a partir de eventos registrados.

### Funcionalidades Principales

- Registro de equipos/académias por categorías (sub-10, sub-15, primera)
- Perfil del jugador con posición, datos físicos, fotografía, contacto y emergencia
- Gestión de mensualidades simuladas (sin dinero real)
- Planificación de entrenamientos con librería reutilizable de ejercicios
- Registro de asistencia e historial de participación
- Convocatoria y confirmación de disponibilidad
- Alineación con titulares y suplentes
- Registro dinámico de partido en vivo con catálogo configurable
- Sincronización en tiempo real de marcador y eventos
- Estadísticas acumuladas por jugador y equipo
- Anuncios y notificaciones
- Comunidad con posts, comentarios y reacciones
- Avisos de pruebas/convocatorias abiertas con filtros
- Moderación y reporte de contenido
- Resumen narrativo automático basado en eventos (IA)

---

## 2. Stakeholders

| Rol | Responsabilidades |
|---|---|
| **DT (Entrenador)** | Gestión de equipos, entrenamientos, alineación, registro de partidos, supervisión |
| **Jugador** | Perfil, entrenamientos, convocatoria, asistencia, estadísticas, comunidad |
| **Padre/Apoderado** | Supervisión de hijo/hija, información, notificaciones, pagos simulados |
| **Administrador** | Gestión de usuarios, configuración, moderación, avisos |

---

## 3. Reglas de Negocio y Supuestos

### Supuestos Técnicos

- Firebase disponible y configurado (Auth, Firestore, Storage, FCM)
- Firestore soporta eventos en tiempo real con <2 seg de latencia
- Dispositivos Android >= 8.0 (API 26)
- Conectividad intermitente; app funciona offline con caché local
- IA externa (OpenAI/Claude) disponible para resumen narrativo
- Máximo 200 jugadores por academia; máximo 50 eventos por partido

### Criterios de Aceptación Generales

- Funcionalidades testeadas con datos incompletos
- Privacidad: no exponer datos de menores en contextos públicos
- Trazabilidad: todo cambio auditable
- Sincronización: <2 segundos entre dispositivos
- IA: verifica que NO inventa jugadas, separa hechos de interpretación
- Catálogo configurable y sincronizable en tiempo real

---

## 4. Privacidad y Permisos por Rol

### Matriz de Acceso

| Información | DT | Jugador | Padre | Admin |
|---|---|---|---|---|
| Perfil jugador | Lectura | L/Edición | L (hijo) | Lectura |
| Foto perfil | Lectura | L/Edición | Lectura | Lectura |
| Datos físicos | Lectura | L/Edición | L (hijo) | Lectura |
| Contacto emergencia | Lectura | L/Edición | L (hijo) | Lectura |
| Asistencia | Lectura | L (propio) | L (hijo) | Lectura |
| Convocatoria | L/Creación | Lectura | Notificación | Lectura |
| Alineación | L/Edición | Lectura | L (hijo) | Lectura |
| Eventos partido | L/Edición | Lectura | Lectura | Lectura |

### Criterios de Privacidad de Menores

- Datos físicos (altura, peso) solo para padre autorizado, DT y admin
- Contacto de emergencia no expuesto en comunidad pública
- Foto de perfil solo en contexto de equipo autorizado
- En avisos públicos: no exponer datos sensibles

---

## 5. Comunidad y Moderación

### Reglas de Visibilidad

- Posts DT/Admin: visibles en academia a usuarios registrados
- Posts Jugadores: visibles en academia y equipos conectados autorizados
- Posts Padre: solo en contexto de su hijo
- Avisos de prueba: solo DT/Admin, con filtros de categoría y posición

### Moderación

- Reporte: cualquier usuario puede reportar contenido inapropiado
- Admin revisa y puede: ocultar, eliminar, bloquear usuario
- Bloqueado: no puede crear posts; posts previos quedan ocultos
- Apelación: usuario bloqueado puede contactar a admin
- Convocatorias: admin verifica contacto antes de publicar (anti-phishing)

---

## 6. Catálogo de Eventos de Partido

| Evento | Campos Obligatorios | Campos Opcionales | ¿Anulable? |
|---|---|---|---|
| Inicio | Minuto | - | No |
| Fin | Minuto, Marcador | - | No |
| Gol | Minuto, Equipo, Jugador | Asistencia, Tipo | Sí |
| Tarjeta | Minuto, Equipo, Jugador, Color | Razón | Sí |
| Cambio | Minuto, Equipo, Jugador Sale/Entra | - | Sí |
| Falta | Minuto, Equipo, Jugador | Tipo, Jugador Afectado | Sí |
| Penal | Minuto, Equipo, Jugador | Razón | Sí |
| Saque/Esquina | Minuto, Equipo, Jugador | Notas | Sí |
| Offside | Minuto, Equipo, Jugador | Notas | Sí |

### Trazabilidad

Cada evento registra:
- `createdBy`, `createdAt`: quién y cuándo se registró
- `updatedBy`, `updatedAt`: si fue editado
- `voidedBy`, `voidedAt`, `voidReason`: si fue anulado
- `version`: número de versión para auditoría

**Sincronización en tiempo real:** cambios visibles en todos los dispositivos en <2 segundos.

---

## 7. Alcance del Resumen Narrativo con IA

### Fuente de Datos

**ÚNICA fuente:** eventos registrados en tiempo real (`matches/{id}/events`)

**Prohibido:**
- Inventar jugadas
- Asumir posiciones
- Generar tácticas alternativas
- Sugerir alineación

### Estructura del Resumen

**Datos factales:**
- Marcador final
- Goles por minuto y jugador
- Tarjetas (amarillas/rojas)
- Cambios realizados

**Interpretación controlada (marcada como tal):**
- Ritmo del partido
- Posesión aproximada
- Momentos críticos

**Restricciones:**
- No nombrar menores directamente (usar "Jugador #" o iniciales)
- Separar claramente hechos de interpretación
- No generar críticas personales

### Proceso de Aprobación

1. IA genera borrador
2. DT revisa y puede: Aprobar, Rechazar, Solicitar Corrección
3. Resumen aprobado → publicado en comunidad
4. Rechazado → vuelve a IA con feedback
5. Historial de versiones mantenido

---

## 8. Product Backlog

**Total: 30 historias de usuario, 166 puntos**

### Priorización (MoSCoW)

**MUST (Core del Negocio):** Autenticación, roles, equipos, jugadores, entrenamientos, asistencia, convocatoria, alineación, partido en vivo, sincronización, resumen IA básico, anuncios.

**SHOULD:** Comunidad, moderación, pruebas/convocatorias abiertas, estadísticas, resumen IA mejorado.

**COULD:** Chat directo, exportar PDF, análisis predictivo, integración ticketing, app web.

**WON'T:** Pagos reales, chatbot RAG, sugerencias de alineación automáticas, streaming de video.

---

## 9. Historias de Usuario

### Tabla Resumen (Anexo 1)

| Historia | Responsable | Puntos |
|---|---|---|
| US-01: Registro de Usuario por Rol | 24100514 – Henry Aaron Tovar Landa | 8 |
| US-02: Inicio de Sesión | 24100514 – Henry Aaron Tovar Landa | 5 |
| US-03: Recuperación de Contraseña | 24100514 – Henry Aaron Tovar Landa | 3 |
| US-04: Crear y Gestionar Equipo | 24100514 – Henry Aaron Tovar Landa | 8 |
| US-05: Registrar Jugador en Equipo | 24100514 – Henry Aaron Tovar Landa | 5 |
| US-06: Ver y Editar Perfil de Jugador | 24100514 – Henry Aaron Tovar Landa | 5 |
| US-07: Registrar Contacto de Emergencia | 24100514 – Henry Aaron Tovar Landa | 3 |
| US-08: Asignar Padre/Apoderado a Jugador | 24100514 – Henry Aaron Tovar Landa | 5 |
| US-09: Planificar Sesión de Entrenamiento | 24100478 – Julia Jhamilett Rojas Estrada | 8 |
| US-10: Agregar Ejercicios a Librería | 24100478 – Julia Jhamilett Rojas Estrada | 5 |
| US-11: Registrar Asistencia a Entrenamiento | 24100478 – Julia Jhamilett Rojas Estrada | 5 |
| US-12: Ver Historial de Asistencia | 24100478 – Julia Jhamilett Rojas Estrada | 3 |
| US-13: Convocar Jugadores a Partido | 24100478 – Julia Jhamilett Rojas Estrada | 5 |
| US-14: Confirmar Disponibilidad para Partido | 24100478 – Julia Jhamilett Rojas Estrada | 3 |
| US-15: Crear Alineación (Titulares y Suplentes) | 24100478 – Julia Jhamilett Rojas Estrada | 8 |
| US-16: Ver Alineación como Jugador/Padre | 24100478 – Julia Jhamilett Rojas Estrada | 3 |
| US-17: Registrar Evento de Partido en Tiempo Real | 24100514 – Henry Aaron Tovar Landa | 13 |
| US-18: Corregir o Anular Evento de Partido | 24100514 – Henry Aaron Tovar Landa | 8 |
| US-19: Ver Scoreboard y Timeline en Tiempo Real | 24100514 – Henry Aaron Tovar Landa | 8 |
| US-20: Generar Resumen Narrativo con IA | 24100514 – Henry Aaron Tovar Landa | 13 |
| US-21: Revisar y Aprobar Resumen IA | 24100478 – Julia Jhamilett Rojas Estrada | 5 |
| US-22: Ver Estadísticas Acumuladas | 24100478 – Julia Jhamilett Rojas Estrada | 5 |
| US-23: Crear Anuncio de Academia | 24100478 – Julia Jhamilett Rojas Estrada | 3 |
| US-24: Recibir Notificaciones | 24100478 – Julia Jhamilett Rojas Estrada | 5 |
| US-25: Crear Post en Comunidad | 24100478 – Julia Jhamilett Rojas Estrada | 5 |
| US-26: Comentar y Reaccionar a Post | 24100478 – Julia Jhamilett Rojas Estrada | 3 |
| US-27: Publicar Aviso de Prueba/Convocatoria | 24100478 – Julia Jhamilett Rojas Estrada | 5 |
| US-28: Filtrar Avisos de Prueba | 24100478 – Julia Jhamilett Rojas Estrada | 3 |
| US-29: Reportar Contenido Inapropiado | 24100478 – Julia Jhamilett Rojas Estrada | 3 |
| US-30: Moderar y Gestionar Reportes | 24100478 – Julia Jhamilett Rojas Estrada | 5 |

**Total: Henry 84 puntos, Julia 82 puntos**

### Detalle de Historias

Todas las historias de usuario con descripción completa, criterios de aceptación verificables y referencias de prototipo se encuentran en el archivo `user_stories.md` en formato Anexo 1 del proyecto.

---

## 10. Modelo de Datos

### Colecciones Principales

| Colección | Descripción |
|---|---|
| `users/{uid}` | Información de usuarios (DT, Jugador, Padre, Admin) |
| `academies/{id}` | Academias y clubes |
| `academies/{id}/teams/{id}` | Equipos dentro de academias |
| `academies/{id}/teams/{id}/players/{id}` | Jugadores del equipo |
| `trainingSessions/{id}` | Sesiones de entrenamiento planificadas |
| `trainingSessions/{id}/attendance/{id}` | Asistencia a entrenamientos |
| `exerciseLibrary/{id}/exercises/{id}` | Librería reutilizable de ejercicios |
| `callups/{id}` | Convocatorias para partidos |
| `lineups/{id}` | Alineaciones de partidos |
| `matches/{id}` | Partidos disputados |
| `matches/{id}/events/{id}` | Eventos registrados durante partido |
| `matches/{id}/events/{id}/edits/{id}` | Historial de ediciones de eventos |
| `aiSummaries/{id}` | Resúmenes narrativos generados por IA |
| `announcements/{id}` | Anuncios de academia |
| `posts/{id}` | Posts de la comunidad |
| `posts/{id}/comments/{id}` | Comentarios en posts |
| `posts/{id}/reactions/{id}` | Reacciones (emojis) en posts |
| `tryouts/{id}` | Avisos de pruebas/convocatorias abiertas |
| `reports/{id}` | Reportes de contenido inapropiado |
| `fees/{id}` | Mensualidades simuladas |
| `guardians/{id}` | Padres/apoderados y vínculos con jugadores |

### Diagrama ER

Ver archivo `data_model.md` para diagrama Mermaid completo con todas las relaciones, campos, tipos y reglas de seguridad.

---

## 11. Prototipos Navegables

### Cómo Acceder

1. Navegar a la carpeta `prototipo/`
2. Abrir `index.html` con cualquier navegador web
3. El prototipo emulará una pantalla Android (~400x750px)
4. Hacer clic en elementos para navegar

### Pantallas Incluidas (25 pantallas)

| ID | Pantalla |
|---|---|
| P-01 | Registro de Usuario |
| P-02 | Inicio de Sesión |
| P-03 | Home Entrenador |
| P-04 | Home Jugador |
| P-05 | Home Padre |
| P-06 | Home Administrador |
| P-07 | Gestión de Equipos |
| P-08 | Jugadores del Equipo |
| P-09 | Perfil de Jugador |
| P-11 | Entrenamientos |
| P-12 | Registrar Asistencia |
| P-14 | Partidos |
| P-15 | Mis Convocatorias |
| P-16 | Crear Alineación |
| P-17 | Ver Alineación |
| P-18 | Registrar Evento de Partido |
| P-19 | Ver Partido en Vivo |
| P-20 | Revisar Resumen IA |
| P-21 | Mis Estadísticas |
| P-22 | Anuncios |
| P-24 | Notificaciones |
| P-25 | Comunidad |
| P-26 | Avisos de Prueba |
| P-27 | Reportar Contenido |
| P-28 | Centro de Moderación |

### Características

- **Material 3 Design:** colores gradientes, tipografía Roboto, sombras realistas
- **Interactivo:** todos los flujos principales son navegables
- **Responsive:** se adapta a diferentes tamaños de pantalla
- **Sin dependencias externas:** HTML, CSS y JavaScript puros (excepto Google Fonts)
- **Animaciones:** transiciones suaves entre pantallas

---

## Conclusión

Este Entregable 1 presenta una especificación completa del proyecto SportPro incluyendo:

✓ **Product Backlog:** 30 historias de usuario priorizadas por impacto en core  
✓ **Análisis de Requerimientos:** reglas de negocio, privacidad, comunidad, catálogo de eventos, alcance IA  
✓ **Historias de Usuario:** formato Anexo 1 con criterios de aceptación verificables  
✓ **Modelo de Datos:** Firestore collections con consideraciones de privacidad y seguridad  
✓ **Prototipos Navegables:** 25 pantallas en HTML5 con Material 3 Design  

**Distribución de Trabajo:**
- **Henry Aaron Tovar Landa (84 puntos):** Autenticación, equipos, jugadores, partido en vivo, catálogo de eventos, resumen IA
- **Julia Jhamilett Rojas Estrada (82 puntos):** Entrenamientos, convocatoria, alineación, comunidad, moderación, anuncios

**Próximas Etapas (Semana 7):** Implementación Android al 40% con autenticación, navegación y avance funcional de equipos, jugadores y entrenamientos.

