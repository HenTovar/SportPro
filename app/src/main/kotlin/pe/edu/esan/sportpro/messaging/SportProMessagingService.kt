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
        Timber.d("Mensaje recibido de: ${remoteMessage.from}")

        // Procesar datos del mensaje
        if (remoteMessage.data.isNotEmpty()) {
            Timber.d("Datos del mensaje: ${remoteMessage.data}")
            handleNotification(remoteMessage.data)
        }

        // Procesar notificación
        remoteMessage.notification?.let {
            Timber.d("Título: ${it.title}, Cuerpo: ${it.body}")
        }
    }

    /**
     * Llamado cuando se genera un nuevo token de registro.
     */
    override fun onNewToken(token: String) {
        Timber.d("Token FCM generado: $token")
        // Aquí se debe enviar el token al servidor para almacenarlo
        // en la base de datos asociado al usuario actual
    }

    /**
     * Procesa los datos del mensaje.
     */
    private fun handleNotification(data: Map<String, String>) {
        when (val type = data["type"]) {
            "training" -> {
                Timber.d("Notificación de entrenamiento: ${data["message"]}")
            }
            "match" -> {
                Timber.d("Notificación de partido: ${data["message"]}")
            }
            "attendance" -> {
                Timber.d("Notificación de asistencia: ${data["message"]}")
            }
            else -> {
                Timber.d("Tipo de notificación desconocida: $type")
            }
        }
    }
}
