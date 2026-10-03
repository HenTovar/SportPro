package pe.edu.esan.sportpro.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.data.model.UserRole
import pe.edu.esan.sportpro.data.repository.AuthRepository
import pe.edu.esan.sportpro.data.repository.Result

/**
 * Devuelve true si el usuario actual es ADMIN o ENTRENADOR (puede crear y editar).
 * JUGADOR y PADRE sólo ven en modo lectura.
 */
@Composable
fun rememberEsStaff(authRepository: AuthRepository): Boolean {
    var esStaff by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        authRepository.getCurrentUser().collect { resultado ->
            if (resultado is Result.Success) {
                esStaff = resultado.data?.role in listOf(UserRole.ADMIN, UserRole.ENTRENADOR)
            }
        }
    }
    return esStaff
}

/** Estado de carga de una lista: cargando, error, vacía o con datos. */
sealed class EstadoLista<out T> {
    object Cargando : EstadoLista<Nothing>()
    data class Error(val mensaje: String) : EstadoLista<Nothing>()
    data class Datos<T>(val items: List<T>) : EstadoLista<T>()
}

/** Muestra cargando / error / vacío; si hay datos, delega en [contenido]. */
@Composable
fun <T> ContenidoLista(
    estado: EstadoLista<T>,
    textoVacio: String,
    modifier: Modifier = Modifier,
    contenido: @Composable (List<T>) -> Unit
) {
    when (estado) {
        is EstadoLista.Cargando -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is EstadoLista.Error -> Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Error: ${estado.mensaje}", color = MaterialTheme.colorScheme.error)
        }
        is EstadoLista.Datos -> if (estado.items.isEmpty()) {
            Column(
                modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { Text(textoVacio, style = MaterialTheme.typography.bodyLarge) }
        } else {
            contenido(estado.items)
        }
    }
}

/** Convierte el Result de los repositorios al estado de la lista. */
fun <T> Result<List<T>>.aEstadoLista(): EstadoLista<T> = when (this) {
    is Result.Loading -> EstadoLista.Cargando
    is Result.Error -> EstadoLista.Error(message)
    is Result.Success -> EstadoLista.Datos(data)
}
