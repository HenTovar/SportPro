# Plan de Trabajo y Ramas de Git - SportPro

## Estudiantes

- **Henry Aaron Tovar Landa** (Carnet: 24100514)
- **Julia Jhamilett Rojas Estrada** (Carnet: 24100478)

## Descripción General

El proyecto SportPro utiliza Git Flow modificado con las siguientes ramas principales:

```
main (rama principal de producción)
├── chore/setup              (Infraestructura y configuración)
├── feature/auth             (Autenticación de usuarios)
├── feature/equipos-jugadores (Gestión de equipos y jugadores)
├── feature/entrenamientos    (Gestión de entrenamientos)
├── feature/asistencia       (Sistema de asistencia)
└── docs/diseno-tecnico      (Documentación técnica)
```

## Ramas y Responsabilidades

### 1. `chore/setup` - Configuración Inicial
**Responsable:** Ambos (colaborativo)

**Contenido:**
- Gradle configuration (build.gradle.kts, gradle wrapper)
- Android Manifest
- Material 3 theme setup
- MainActivity y navigation base
- Inyección de dependencias (AppContainer)
- .gitignore y configuración del proyecto

**Commits principales:**
```
- Initial project setup with Gradle
- Configure Gradle wrapper and dependencies
- Set up Android Manifest with required permissions
- Implement Material 3 theme
- Create MainActivity and navigation infrastructure
- Set up manual dependency injection (AppContainer)
```

---

### 2. `feature/auth` - Autenticación y Usuarios
**Responsable:** Henry Aaron Tovar Landa (24100514)

**Contenido:**
- Firebase Authentication integration
- AuthRepository con validación de email/password
- AuthViewModel con StateFlow
- RegisterScreen y LoginScreen
- User data model
- Firestore users collection schema

**Commits principales:**
```
- Add Firebase authentication dependencies
- Create AuthRepository with Firebase integration
- Implement email and password validation
- Create AuthViewModel with login/register flows
- Build RegisterScreen UI with Compose
- Build LoginScreen UI with Compose
- Set up Firestore users collection structure
```

**Historias de usuario cubiertas:**
- HU-1: Registro con rol (Admin, Entrenador, Jugador, Padre)
- HU-2: Inicio de sesión
- HU-3: Cierre de sesión

---

### 3. `feature/equipos-jugadores` - Gestión de Equipos y Jugadores
**Responsable:** Julia Jhamilett Rojas Estrada (24100478)

**Contenido:**
- Team data model con TeamCategory enum
- Player data model con PlayerPosition enum
- TeamRepository (CRUD de equipos)
- PlayerRepository (CRUD de jugadores)
- TeamsScreen (lista y navegación)
- PlayersScreen (lista de jugadores por equipo)
- CreateTeamScreen y CreatePlayerScreen
- Integración con Cloud Storage para fotos

**Commits principales:**
```
- Create Team and Player data models
- Implement TeamRepository with Firestore CRUD
- Implement PlayerRepository with Firestore CRUD
- Build TeamsScreen with team listing
- Build PlayersScreen with player management
- Create CreateTeamScreen with form validation
- Create CreatePlayerScreen with photo upload
- Implement role-based access control
```

**Historias de usuario cubiertas:**
- HU-4: Listar equipos por categoría
- HU-5: Crear equipo (Admin/Entrenador)
- HU-6: Editar equipo (Admin/Entrenador)
- HU-7: Listar jugadores por equipo
- HU-8: Crear perfil de jugador
- HU-9: Editar datos del jugador
- HU-10: Subir foto del jugador

---

### 4. `feature/entrenamientos` - Gestión de Entrenamientos
**Responsable:** Ambos (colaborativo)

**Contenido:**
- Training data model
- Exercise library (ExerciseLibrary object)
- TrainingRepository (CRUD de entrenamientos)
- TrainingsScreen (lista de entrenamientos)
- CreateTrainingScreen (crear sesión)
- Exercise selection y management

**Commits principales:**
```
- Create Training and Exercise data models
- Implement TrainingRepository
- Build TrainingsScreen with session listing
- Create CreateTrainingScreen with exercise selection
- Implement exercise library management
```

**Historias de usuario cubiertas:**
- HU-11: Crear sesión de entrenamiento
- HU-12: Biblioteca reutilizable de ejercicios
- HU-13: Listar entrenamientos

---

### 5. `feature/asistencia` - Asistencia e Historial
**Responsable:** Ambos (colaborativo)

**Contenido:**
- AttendanceRecord data model
- AttendanceRepository
- AttendanceScreen (registrar asistencia)
- AttendanceHistoryScreen (historial por jugador)
- Cálculo de porcentaje de asistencia
- FCM Integration para notificaciones
- DI enhancements

**Commits principales:**
```
- Create AttendanceRecord model
- Implement AttendanceRepository
- Build AttendanceScreen for marking attendance
- Build AttendanceHistoryScreen with statistics
- Integrate Firebase Cloud Messaging
- Enhance AppContainer with all services
```

**Historias de usuario cubiertas:**
- HU-14: Registrar asistencia
- HU-15: Ver historial de asistencia
- HU-16: Cálculo de porcentaje de asistencia

---

### 6. `docs/diseno-tecnico` - Documentación
**Responsable:** Ambos (colaborativo)

**Contenido:**
- Diseño técnico del sistema
- Firestore rules de seguridad
- Cloud Storage rules
- Diagrama de arquitectura
- README.md con instrucciones

**Archivos:**
```
docs/
├── Diseno_Tecnico_Tiempo_Real_e_IA.md
├── firestore.rules
└── storage.rules
```

---

## Flujo de Trabajo y Merges

### Estrategia de Fusión

1. **Desarrollo en feature branches**: Cada rama de feature desarrolla su funcionalidad de forma aislada
2. **Pull Request**: Antes de mergear a main, se abre un PR para revisión
3. **Merge a main**: Se usa `--no-ff` para mantener historial de ramas
4. **Etiquetas de versión**: Se crean tags para cada entregable

### Comandos de Merge (ejecutados en orden)

```bash
# 1. Mergear chore/setup (infraestructura)
git merge --no-ff chore/setup -m "Merge chore/setup: Initial project setup"

# 2. Mergear feature/auth (autenticación)
git merge --no-ff feature/auth -m "Merge feature/auth: User authentication"

# 3. Mergear feature/equipos-jugadores (gestión de datos)
git merge --no-ff feature/equipos-jugadores -m "Merge feature/equipos-jugadores: Teams and Players"

# 4. Mergear feature/entrenamientos (entrenamientos)
git merge --no-ff feature/entrenamientos -m "Merge feature/entrenamientos: Training sessions"

# 5. Mergear feature/asistencia (asistencia)
git merge --no-ff feature/asistencia -m "Merge feature/asistencia: Attendance tracking"

# 6. Mergear docs/diseno-tecnico (documentación)
git merge --no-ff docs/diseno-tecnico -m "Merge docs/diseno-tecnico: Technical documentation"

# 7. Crear tag para Entregable 2
git tag -a entregable-2 -m "Entregable 2 - 40% completion"
```

## Historial de Commits

El proyecto mantiene un historial de commits limpio y descriptivo siguiendo convenciones:

```
<tipo>(<alcance>): <descripción>

feat:       Nueva funcionalidad
fix:        Corrección de errores
docs:       Cambios en documentación
style:      Cambios de formato (sin cambio funcional)
refactor:   Refactorización del código
test:       Adición o modificación de tests
chore:      Tareas de mantenimiento, actualizaciones
```

### Ejemplos de Commits

```
feat(auth): add Firebase authentication integration
feat(teams): add team creation and editing
feat(players): add player profile management
fix(auth): fix email validation regex
test(auth): add unit tests for email validation
docs: add technical design documentation
```

## Estado Actual (Entregable 2)

**Rama actual:** `main`

**Cambios recientes:**
- ✅ Arreglo de errores de compilación
- ✅ Adición de 30 pruebas unitarias
- ✅ Creación de CreateTeamScreen con manejo de enums
- ✅ Creación de CreateTrainingScreen con parseo de fechas
- ✅ Creación de CreatePlayerScreen con subida de fotos
- ✅ Configuración correcta de Gradle y dependencias

**Compilación:**
- ✅ `./gradlew assembleDebug` → BUILD SUCCESSFUL
- ✅ `./gradlew testDebugUnitTest` → 30 tests passed

## Próximos Pasos para Entregas Futuras

1. **Entregable 3 (60%)**
   - Completar integración de todos los módulos
   - Implementar video en vivo de partidos
   - Sistema completo de notificaciones

2. **Entregable 4 (80%)**
   - Generación de reportes
   - Módulo de comunidad y comunicación
   - Estadísticas avanzadas

3. **Final (100%)**
   - Integración de IA para análisis de partidos
   - Optimizaciones finales
   - Testing completo

## Contacto para Cambios de Responsabilidad

Si durante el desarrollo hay cambios en las responsabilidades:

1. **Comunicar** al equipo el cambio
2. **Actualizar** este documento
3. **Crear commit** con el cambio: `chore(docs): update GITHUB_PLAN.md`

---

**Última actualización:** 3 de Octubre de 2026
**Estado del proyecto:** En desarrollo - Entregable 2 completado
