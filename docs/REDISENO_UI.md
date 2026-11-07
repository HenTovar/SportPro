# Rediseño visual de SportPro

Implementación sobre Compose y Material 3 existentes; sin nuevas bibliotecas, fuentes descargadas, imágenes remotas ni servicios de diseño. El mismo PR conserva toda la lógica de eventos, historial, resumen y comunidad.

## Sistema visual

- Verde de cancha, azul profundo y superficies claras; paleta oscura propia según el modo del sistema. El color dinámico del dispositivo queda desactivado por defecto para conservar la identidad y los pares medidos.
- Sans serif del sistema, títulos jerárquicos y marcador grande; formas 8/12/16/24/28 dp. Tarjetas con elevación de 2 dp, encabezados con gradiente y badges con texto, sin comunicar estado sólo por color.
- Home por rol con accesos en tarjetas; jugador/padre conservan el vínculo requerido. No se añadieron permisos de servidor ni accesos por apariencia.
- Partido: marcador tipo transmisión, aviso de sincronización/caché, mesa de control del staff, cronología con icono de gol/tarjeta/cambio/anulación y revisión accesible.
- Comunidad: avatar genérico sin exponer nombres de menores, fecha, menú de publicación, reacciones/conversación y moderación. Las etiquetas reflejan sólo datos disponibles; no se inventan roles de los autores.
- Animaciones cortas de contenido y transición del marcador. No hay pulsos infinitos ni datos de marcador interpolados: el valor procede de eventos vigentes.

## Accesibilidad medida y pendiente

Los pares opacos texto/fondo de la paleta cumplen AA normal: mínimo **6,48:1** entre los pares revisados; blanco sobre los extremos del gradiente **12,25:1** y **15,74:1**. Outline contra superficies supera **3:1**. La medición es de tokens sRGB, no certificación de cada píxel renderizado ni de colores deshabilitados.

Botones e iconos accionables nuevos mantienen al menos 48 dp explícitos o el mínimo interactivo de Material 3. Avatares/badges no interactivos no cuentan como botones. Back y menú tienen descripciones; el marcador ofrece semántica del resultado y actualización polite. Textos largos pueden envolver, las listas y diálogos desplazan y no se cambian fechas/minutos por efectos visuales.

Pendiente en un Android real o emulador: revisar TalkBack, escala de fuente grande, orientación/pantalla pequeña, contraste de estados deshabilitados y el resultado final de layout. Una compilación no sustituye esas pruebas.

## Capturas antes/después

**No se generaron capturas Android en la máquina de construcción:** no hay AVD ni servidor ADB local accesible. Se solicitó un destino ADB autorizado. No se presentan imágenes HTML ni dibujos como si fueran capturas de la app.

Se conserva el APK previo (commit `de36d45`) y el nuevo APK en la entrega de construcción. En un emulador/dispositivo de pruebas propio, abrir con las mismas cuentas ficticias y el mismo partido:

1. Instalar el APK previo conservando los datos de prueba. Capturar Home, marcador+cronología y comunidad, en claro y oscuro.
2. Instalar el APK nuevo y repetir exactamente esos estados. No incluir login, contraseña, datos reales ni UID completos.
3. Usar la acción **Screen Capture** de Android Studio/Device Manager y guardar PNG con nombres `antes-home-claro`, `despues-home-claro`, etc. Si no hay login/datos de prueba, no afirmar que una captura de Preview es la pantalla conectada.
4. Anotar versión de Android, ancho/alto y escala de texto; comparar también fuente grande y TalkBack. Capturar un evento anulado y un resumen desactualizado para que la estética no oculte los estados.

Los componentes `HomeDashboard`, `MatchBroadcastHeader` y `CommunityPostCard` tienen Preview con datos ficticios para revisar el layout sin Firebase. Sirven para inspección local en Android Studio, pero no acreditan interacción o sincronización.
