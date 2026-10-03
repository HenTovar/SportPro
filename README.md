# SportPro - Aplicación Móvil de Gestión de Equipos de Fútbol

## Descripción

SportPro es una aplicación Android para la gestión integral de equipos y academias de fútbol. Permite administrar jugadores, entrenamientos, partidos y asistencia, con un enfoque en la comunicación y comunidad entre entrenadores, jugadores y padres de familia.

**Estado del Proyecto:** Entregable 2 - 40% de completitud

## Requisitos Previos

- **Android Studio** (versión 2024.1 o más reciente)
- **Java Development Kit (JDK) 17 o superior**
- **Android SDK 35** (API Level 35)
- **Gradle 8.10.2** (incluido en el proyecto)
- Conexión a Internet para descargar dependencias

### Para Windows

1. Descargar e instalar [Android Studio](https://developer.android.com/studio)
2. Durante la instalación, asegúrate de que se instale:
   - Android SDK
   - JDK 17 (o instálalo por separado si es necesario)
   - Android Build Tools 35.0.0
3. Clona este repositorio o abre el proyecto en Android Studio

## Configuración del Proyecto

### Paso 1: Configurar Firebase

1. Crea un nuevo proyecto en [Firebase Console](https://console.firebase.google.com/)
2. Agrega una aplicación Android con el package name `pe.edu.esan.sportpro`
3. Descarga el archivo `google-services.json`
4. Coloca el archivo en la carpeta `app/` del proyecto:
   ```
   SportPro/app/google-services.json
   ```

### Paso 2: Habilitar Autenticación en Firebase

En Firebase Console:
1. Ve a **Authentication** > **Sign-in method**
2. Habilita **Email/Password**

### Paso 3: Configurar Firestore Database

En Firebase Console:
1. Ve a **Firestore Database**
2. Crea una base de datos en modo de producción
3. Copia las reglas de seguridad desde `firestore.rules` (en el repositorio) al Firestore

### Paso 4: Configurar Cloud Storage

En Firebase Console:
1. Ve a **Storage**
2. Crea un bucket
3. Copia las reglas de seguridad desde `storage.rules` (en el repositorio) al Storage

## Construcción y Ejecución

### Abrir en Android Studio

1. Abre **Android Studio**
2. Selecciona **Open** y navega a la carpeta del proyecto `SportPro`
3. Espera a que Gradle se sincronice automáticamente
4. Si no se sincroniza, ve a **File** > **Sync Now**

### Ejecutar en Emulador

1. Ve a **Tools** > **Device Manager**
2. Crea o selecciona un dispositivo virtual (recomendado: Pixel 5, Android 13+)
3. Inicia el emulador
4. Haz clic en el botón **Run** (▶) o presiona `Shift + F10`

### Ejecutar en Dispositivo Físico

1. Conecta tu dispositivo Android por USB
2. Habilita **Depuración USB** en las opciones de desarrollo del dispositivo
3. Haz clic en **Run** en Android Studio

## Estructura del Proyecto

```
SportPro/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/pe/edu/esan/sportpro/
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/        # Modelos de datos (User, Team, Player, Training)
│   │   │   │   │   └── repository/   # Repositorios (Auth, Teams, Players, Training)
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/      # Pantallas (Auth, Teams, Players, Trainings)
│   │   │   │   │   ├── theme/        # Tema Material 3
│   │   │   │   │   ├── viewmodel/    # ViewModels
│   │   │   │   │   └── navigation/   # Navegación
│   │   │   │   ├── di/               # Inyección de dependencias
│   │   │   │   └── MainActivity.kt
│   │   │   └── res/                  # Recursos (strings, colores, iconos)
│   │   └── test/                     # Pruebas unitarias
│   ├── build.gradle.kts              # Configuración de Gradle
│   └── google-services.json          # (generado después de descargar de Firebase)
├── gradle/                           # Configuración de Gradle
├── build.gradle.kts
├── settings.gradle.kts
├── firestore.rules
├── storage.rules
└── README.md
```

## Funcionalidades Principales (Entregable 2)

### Autenticación
-  Registro de usuarios con roles (Admin, Entrenador, Jugador, Padre)
-  Inicio de sesión con email y contraseña
-  Cierre de sesión
-  Validación de email y contraseña

### Gestión de Equipos
-  Listar equipos por categoría (Sub-10, Sub-15, Primera, etc.)
-  Crear nuevos equipos
-  Editar equipos (acceso limitado a administradores)
-  Eliminar equipos

### Gestión de Jugadores
-  Listar jugadores por equipo
-  Crear perfil de jugador (nombre, posición, datos físicos, contacto)
-  Editar datos del jugador
-  Subir foto del jugador (Cloud Storage)
-  Vista limitada para jugadores/padres (sin acceso a datos de otros)

### Entrenamientos
-  Crear sesiones de entrenamiento (equipo, fecha, objetivo, duración)
-  Biblioteca reutilizable de ejercicios
-  Listar entrenamientos

### Asistencia
-  Registrar asistencia a entrenamientos
-  Historial de asistencia por jugador
-  Cálculo de porcentaje de asistencia

### Arquitectura
-  MVVM con ViewModel y StateFlow
-  Navigation Compose
-  Material 3 Design
-  Firebase Authentication
-  Firestore Database
-  Cloud Storage
-  Inyección de dependencias manual (AppContainer)

## Pruebas Unitarias

El proyecto incluye **30 pruebas unitarias** que cubren:

- Validación de email
- Validación de contraseña
- Funcionalidad del ViewModel
- Flujos de registro e inicio de sesión
- Pruebas con repositorio simulado (fake)

Para ejecutar las pruebas:
```bash
./gradlew testDebugUnitTest
```

Los resultados se encuentran en:
```
app/build/test-results/testDebugUnitTest/
```

## Tecnologías Utilizadas

- **Kotlin** 2.0.20
- **Jetpack Compose** para UI
- **Material 3** para diseño
- **Firebase** (Auth, Firestore, Storage, Messaging)
- **Coroutines** para programación asíncrona
- **Navigation Compose** para enrutamiento
- **Mockito** y **Truth** para pruebas unitarias
- **Gradle 8.10.2** para compilación

## Notas Importantes

1. **google-services.json**: Este archivo se genera desde Firebase y NO debe ser compartido públicamente. Está incluido en `.gitignore`.

2. **Primeros pasos en la app**:
   - Crea una cuenta con email y contraseña
   - Selecciona tu rol (Entrenador para crear equipos y jugadores)
   - Navega a la sección de Equipos y comienza a crear tu equipo

3. **Restricciones de privacidad**:
   - Los jugadores solo pueden ver su propio perfil
   - Los padres pueden ver el perfil de sus hijos
   - Los datos de contacto son privados según el rol

4. **Flujo de la aplicación**:
   - Si no está autenticado → Pantalla de Login/Registro
   - Después de autenticarse → Home (según tu rol)
   - Acceso a Equipos, Jugadores, Entrenamientos, Asistencia

## Próximos Pasos (Entregable 3+)

- [ ] Implementar video en vivo de partidos
- [ ] Sistema de notificaciones push
- [ ] Estadísticas avanzadas
- [ ] Reportes en PDF
- [ ] Integración con calendario
- [ ] Módulo de comunicación (chat, publicaciones)
- [ ] Implementar IA para resumen narrativo de partidos

## Soporte y Contacto

Para preguntas o problemas:
- Henry Aaron Tovar Landa (24100514)
- Julia Jhamilett Rojas Estrada (24100478)

Curso: **Desarrollo de Aplicaciones Móviles**
Universidad: **ESAN**
Fecha de Entrega: **3 de Octubre de 2026**

## Licencia

Este proyecto es académico y es propiedad de los estudiantes de ESAN.
