package pe.edu.esan.sportpro.features

import androidx.compose.runtime.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.Exercise
import pe.edu.esan.sportpro.data.model.User

data class ExerciseCatalogState(
    val rows: Map<String, Exercise> = UiInteractionPolicy.exerciseCatalog(emptyList()),
    val loading: Boolean = false,
    val unavailable: Boolean = false
)

/** One explicit library read per authorized screen/key; never a query in a row or recomposition. */
@Composable
fun rememberExerciseCatalog(user: User?, refresh: Int = 0): ExerciseCatalogState {
    val allowed = UiInteractionPolicy.canReadExerciseLibrary(user)
    var state by remember(user?.uid, user?.role, user?.isActive, refresh) {
        mutableStateOf(ExerciseCatalogState(loading = allowed))
    }
    LaunchedEffect(user?.uid, user?.role, user?.isActive, refresh) {
        if (allowed) try {
            val snapshot = FirebaseFirestore.getInstance().collection("exercises").get().await()
            val remote = snapshot.documents.mapNotNull { it.toObject(Exercise::class.java)?.copy(id = it.id) }
            state = ExerciseCatalogState(rows = UiInteractionPolicy.exerciseCatalog(remote))
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { state = ExerciseCatalogState(unavailable = true) }
    }
    return state
}
