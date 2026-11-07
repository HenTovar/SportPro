# Activar IA en APK debug sin compartir secretos

**Estado: guía basada en documentación oficial; no ejecutada todavía en el teléfono ni en la consola de Henry.** No acredita una llamada real ni una aceptación del APK.

**Alcance autorizado: demostración académica privada con datos ficticios, sólo personas adultas. No uso general ni información real de menores.** La llamada remota requiere confirmar en la pantalla que los datos son ficticios y obtener una atestación válida. Esta guía no acredita una excepción contractual para otro público. Debe verificarse en el teléfono del dueño, sin tokens en el repo/APK/chat.

La app abre y la plantilla funciona aunque este paso no se haya hecho. Si AI Logic exige App Check, el borrador IA necesita atestación válida. El APK debug incluye únicamente en su variante debug el proveedor oficial; el token se genera en cada teléfono/instalación, NO está en el APK.

1. Después de revisar el sucesor y sólo en ese alcance, instalar el APK debug firmado del proyecto correcto **sólo en el teléfono de Henry**. No registrar otros dispositivos ni distribuir este debug para uso general. Entrar con una cuenta autorizada y cargar un partido de prueba sin datos reales de menores.
2. En la consola **del dueño del proyecto**, abrir Firebase → App Check y seleccionar la app Android `pe.edu.esan.sportpro` correcta. En su menú elegir **Administrar tokens de depuración**.
3. Conectar ese mismo teléfono a Android Studio/Logcat, seleccionar sólo el proceso de SportPro y filtrar por etiqueta `DebugAppCheckProvider`. Pulsar «Generar IA o plantilla» en el partido de prueba. El SDK mostrará un token de depuración de ese dispositivo; la app no imprime claves ni copia el token al chat.
4. Copiar el token **directamente de Logcat a la consola de Firebase** y guardarlo allí con un nombre que identifique el dispositivo de pruebas. No enviarlo al chat, no añadirlo al código, no adjuntar capturas que lo muestren y no incluirlo en reportes públicos.
5. Volver a abrir SportPro y repetir la generación. Verificar que aparece el aviso **Borrador con IA**, revisar el texto y confirmar que el proveedor es Gemini Developer API y el modelo configurado está disponible en Spark. Una plantilla por sí sola NO demuestra una llamada real de IA.
6. Si sigue en plantilla, revisar configuración de AI Logic/App Check y cuota en la consola del proyecto. No desactivar enforcement como remedio. Informar sólo el estado y el mensaje no sensible, nunca tokens/claves. No se ha probado aquí la llamada real.
7. **Al cerrar las pruebas, Henry debe eliminar/revocar ese token en App Check y comprobar que ya no está registrado.** El token es un secreto: quien lo posee puede obtener atestaciones de ese proyecto. No conservarlo como credencial permanente ni dejar copias en logs compartidos. Borrar datos/reinstalar puede generar otro token; no asumir que el anterior sigue siendo el correcto.

Antes de publicar una variante release se debe configurar un proveedor de atestación de producción y verificarlo en dispositivo. El proveedor debug NO se compila dentro de release. Este APK y esta guía no sustituyen esa validación.

Referencia oficial: https://firebase.google.com/docs/app-check/android/debug-provider
Política de AI Logic: https://firebase.google.com/docs/ai-logic/app-check

No se pidió, recibió, almacenó ni publicó una clave secreta de Gemini. La configuración pública Firebase del cliente que ya existía es distinta; tampoco hay razón para publicarla.

Eliminar el secreto de depuración impide nuevos intercambios con él; no se declara revocación instantánea de atestaciones ya emitidas y aún vigentes. Comprobar el estado y no reutilizar el APK de demostración como publicación general.
