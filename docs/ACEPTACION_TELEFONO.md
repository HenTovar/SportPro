# SportPro 1.2.0 / code8 — comprobación en teléfono

Cambios sólo de presentación sobre code6; se mantienen sus reglas y revocación familiar.

1. Pantalla 1080 px, ancho lógico 411 dp (densidad aproximada 2,628), modo oscuro y claro. Abrir Partidos desde la barra fija: no debe haber franja blanca ni flecha atrás en la raíz. Girar/regresar y comprobar cámara y barras del sistema.
2. Con cero partidos: Buscar y los filtros aparecen antes del estado vacío. Desplazar verticalmente y horizontalmente hasta Finalizados: cuatro etiquetas enteras, blancos táctiles de al menos 48 dp. Repetir con lista larga y fuente del sistema aumentada; la barra inferior no tapa filtros ni último partido.
3. Barra fija: Inicio / Partidos / Equipo / Muro / Perfil. Muro completo, Equipo con icono de lista/plantel y Perfil con persona. Cada uno abre su ruta original; no cambian permisos.
4. Abrir partido y editar jugador: esos detalles SÍ conservan Volver. Las raíces Equipo y Muro no muestran Volver.
5. Repetir revocación familiar code6: ADMIN revoca un hijo, el padre pierde lecturas futuras del servidor de su ficha/asistencia y conserva hermanos. No promete borrar copias offline.

Las pruebas PhoneChromeLayoutTest ejercitan controles de producción en fixtures sin red a 411 dp y a 1080 px/411 dp. Compilar el APK de pruebas no significa que se hayan ejecutado ni que el teléfono haya sido aceptado. Requiere dispositivo conectado y connectedDebugAndroidTest (o instalación de ambos APK + am instrument).

## Teclado (prioridad)
Abrir Partidos y tocar Buscar partido. Escribir una cadena larga y mover cursor: campo/texto visible encima del teclado, barra inferior ausente, sin franja blanca ni superposición con Crear. Cerrar con Buscar/atrás: reaparece la barra y se conserva la consulta. Reabrir, girar, probar fuente aumentada. Probar también login y el editor de jugador para evitar regresión de formularios. Teclado físico/flotante puede no emitir inset inferior: se oculta la barra por visibilidad IME y por altura mientras termina la animación. No se adivina altura por porcentaje.
El modo adjustResize entrega insets en edge-to-edge; AppViewport consume sólo barras del sistema/cutout y AppContentFrame aplica IME al contenido. No añadir padding extra al teclado en pantallas hijas.
