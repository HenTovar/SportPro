package pe.edu.esan.sportpro.messaging

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

/**
 * Servicio de mensajería de Firebase para notificaciones push.
 * Maneja la recepción de mensajes en primer plano y en segundo plano.
 */
class SportProMessagingService : FirebaseMessagingService() {

    /**
     * Llamado cuando se recibe un mensaje desde Firebase Cloud Messaging.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("Mensaje remoto recibido")

        // Procesar datos del mensaje
        if (remoteMessage.data.isNotEmpty()) {
            Timber.d("Mensaje con datos; sin volcado de contenido")
            handleNotification(remoteMessage.data)
        }

        // Procesar notificación
        remoteMessage.notification?.let {
            Timber.d("Mensaje con aviso remoto")
        }
    }

    /**
     * Llamado cuando se genera un nuevo token de registro.
     */
    override fun onNewToken(token: String) {
        // No registrar tokens ni enviarlos a colecciones nuevas.
        // Aquí se debe enviar el token al servidor para almacenarlo
        // en la base de datos asociado al usuario actual
    }

    /**
     * Procesa los datos del mensaje.
     */
    private fun handleNotification(data: Map<String, String>) {
        when (val type = data["type"]) {
            "training" -> {
                Timber.d("Tipo de mensaje: entrenamiento")
            }
            "match" -> {
                Timber.d("Tipo de mensaje: partido")
            }
            "attendance" -> {
                Timber.d("Tipo de mensaje: asistencia")
            }
            else -> {
                Timber.d("Tipo de notificación desconocida: $type")
            }
        }
    }
}
