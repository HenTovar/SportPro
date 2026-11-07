# SportPro - Aplicación Móvil de Gestión de Equipos de Fútbol

## Descripción

SportPro es una aplicación Android para la gestión integral de equipos y academias de fútbol. Permite administrar jugadores, entrenamientos, partidos y asistencia, con un enfoque en la comunicación y comunidad entre entrenadores, jugadores y padres de familia.

**Entrega en revisión:** Semana 12. Partido en vivo, comunidad y resumen IA v1.

Ver [ENTREGA.md](ENTREGA.md) para alcance y límites, [guía de demo](docs/SEMANA12_DEMO.md) y [pruebas](docs/SEMANA12_PRUEBAS.md). El video de partido real y la aceptación en dos teléfonos están pendientes; no se declara un porcentaje aceptado sin esa evidencia.

## Actualización visual y publicación de reglas

- [Rediseño y accesibilidad](docs/REDISENO_UI.md): Material 3 claro/oscuro, home por rol, marcador y comunidad.
- [Publicar reglas con respaldo y smoke real](docs/PUBLICAR_REGLAS_FIRESTORE.md): procedimiento para el propietario porque la máquina de construcción no tiene sesión Firebase. No se ha publicado desde aquí.
- `firebase.json` contiene sólo Firestore; el despliegue siempre lleva `--only firestore:rules --project sportpro-df6e0`.

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

## Funcionalidades heredadas y nuevas

### Autenticación
-  Registro de Entrenador/Jugador/Padre; ADMIN mediante aprobación
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

El proyecto incluye pruebas unitarias que cubren:

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

## Próximos Pasos (Entregable 3+)

- [ ] Implementar video en vivo de partidos
- [ ] Sistema de notificaciones push
- [ ] Estadísticas avanzadas
- [ ] Reportes en PDF
- [ ] Integración con calendario
- [x] Comunidad de texto, comentarios, reacciones y moderación por equipo
- [x] Integración Retrofit y revisión de resumen narrativo; proveedor real pendiente de configurar

## Soporte y Contacto

Para preguntas o problemas:
- Henry Aaron Tovar Landa (24100514)
- Julia Jhamilett Rojas Estrada (24100478)

Curso: **Desarrollo de Aplicaciones Móviles**
Universidad: **ESAN**
Actualización Semana 12: **6 de Noviembre de 2026**

## Licencia

Este proyecto es académico y es propiedad de los estudiantes de ESAN.

## Uso académico de la demostración 1.3.0

Resumen generado con IA: **sólo datos de prueba**. Esta entrega es para una demostración académica privada entre adultos y su profesor, sin despliegue público. Confirmar en la pantalla que los eventos son ficticios antes de solicitar IA. Los nombres nunca se envían: se usan alias y se restauran sólo en el teléfono. Si no hay red/atestación/cuota, se usa plantilla y revisión explícita. **No publicar con IA remota si la app llega a menores reales.** No introducir datos personales, sensibles ni confidenciales. La confirmación no verifica por sí sola que los datos sean ficticios. No hay clave Gemini en el repo/APK/chat. Véanse docs/APP_CHECK_DEBUG.md y docs/IA_POLITICA_PENDIENTE.md; release no habilita IA sin atestación de producción.
