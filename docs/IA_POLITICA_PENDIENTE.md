# Alcance de IA: demostración académica privada

El dueño confirmó que esta entrega es una tarea, sólo para él, su grupo adulto y su profesor, sin despliegue público. Se incluye Firebase AI Logic/BOM34.1.0 con plantilla de respaldo y sin clave Gemini en el cliente. No se declara una excepción a los términos: **no publicar con IA remota si la app llega a menores reales**, ni enviar información personal/sensible/confidencial. Sólo datos ficticios para la demostración. El aviso y la confirmación de la pantalla no demuestran automáticamente que un dato sea ficticio; quien opera debe comprobarlo.

Debug exige registro de App Check únicamente del teléfono del dueño y token válido antes de inicializar IA; al cerrar las pruebas se elimina/revoca el token y se verifica su baja. Nunca compartir tokens o capturas de Logcat. Ver APP_CHECK_DEBUG.md. El valor de enforcement del servidor no fue medido aquí: no se asumió encendido ni apagado, y no se pide apagarlo. La comprobación local de atestación falla cerrada aun sin enforcement remoto.

Release carece de proveedor de atestación de producción y falla cerrado antes de inicializar IA o enviar prompts; usa plantilla. No es una variante pública de IA preparada para desplegar. No se probaron aquí llamadas reales al proveedor ni se registraron tokens.

Los términos de audiencia y datos siguen aplicándose a cualquier uso que salga del alcance académico. Cambiar a un plan de pago no acredita resolver la cláusula de edad. Ollama u otro proveedor no están integrados; requieren otro alcance y revisión.

Referencias oficiales: https://ai.google.dev/gemini-api/terms y https://firebase.google.com/docs/ai-logic/data-governance .
