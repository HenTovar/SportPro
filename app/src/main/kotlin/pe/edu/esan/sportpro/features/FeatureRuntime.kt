package pe.edu.esan.sportpro.features

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import pe.edu.esan.sportpro.MainActivity
import pe.edu.esan.sportpro.ui.navigation.TeamSession
import pe.edu.esan.sportpro.data.model.RolePolicy

object FirestoreOffline {
    private var configured=false
    @Synchronized fun restartAfterClear() { configured=false; configure() }
    @Synchronized fun configure() {
        if(configured)return
        FirebaseFirestore.getInstance().firestoreSettings=FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build()).build()
        configured=true
    }
}
class LocalNotifier(private val context: Context) {
    private val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    fun send(uid: String, event: LocalAlert) {
        if(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid != uid)return
        if(uid.isBlank() || !context.getSharedPreferences("appearance",Context.MODE_PRIVATE).getBoolean("notifications_$uid",false))return
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        if(!manager.areNotificationsEnabled())return
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(NotificationChannel("team_local","Actividad del equipo",NotificationManager.IMPORTANCE_DEFAULT))
        val intent=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification=NotificationCompat.Builder(context,"team_local").setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(event.title).setContentText(event.body).setContentIntent(intent).setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
        try { manager.notify(uid,event.key.hashCode(),notification) } catch (_: SecurityException) { /* Permission may be revoked between check and notify. */ }
    }
    fun clear()=manager.cancelAll()
}
val LocalTeamFeatures=staticCompositionLocalOf { FeatureState() }
val LocalNetworkConnected=staticCompositionLocalOf { true }

@Composable
fun FeatureRuntime(session: TeamSession,content:@Composable ()->Unit) {
    val context=androidx.compose.ui.platform.LocalContext.current.applicationContext
    val repo=remember { TeamFeaturesRepository(FirebaseFirestore.getInstance()) }
    val notifier=remember { LocalNotifier(context) }
    val uid=session.account?.uid.orEmpty();val team=session.selected?.id.orEmpty()
    var state by remember(uid,team,session.child?.key) { mutableStateOf(FeatureState(team=team,loading=team.isNotBlank())) }
    val manager=remember { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    fun online()=manager.activeNetwork?.let { manager.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } == true
    var connected by remember { mutableStateOf(online()) }
    DisposableEffect(manager) {
        val callback=object:ConnectivityManager.NetworkCallback(){
            override fun onAvailable(network:Network){connected=online()}
            override fun onLost(network:Network){connected=online()}
            override fun onCapabilitiesChanged(network:Network,caps:NetworkCapabilities){connected=online()}
        }
        manager.registerDefaultNetworkCallback(callback)
        onDispose{manager.unregisterNetworkCallback(callback)}
    }
    DisposableEffect(uid) { notifier.clear(); onDispose { notifier.clear() } }
    LaunchedEffect(uid,team,session.child?.key,session.account?.role,session.account?.isActive) {
        state=FeatureState(team=team,loading=team.isNotBlank())
        if(uid.isNotBlank() && team.isNotBlank() && session.account?.isActive==true && RolePolicy.canReadTeam(session.account,team)) {
            repo.observe(team,RolePolicy.isStaff(session.account?.role),session.profile?.playerId.orEmpty()) { notifier.send(uid,it) }.collect { state=it }
        }
    }
    CompositionLocalProvider(LocalTeamFeatures provides state,LocalNetworkConnected provides connected,content=content)
}
@Composable
fun ConnectionBanner() {
    val state=LocalTeamFeatures.current
    val online=LocalNetworkConnected.current
    val text=when {
        !online -> "Sin conexión · datos guardados en este dispositivo; los cambios esperan sincronización"
        state.error!=null -> "No se pudieron confirmar los datos del equipo; vuelve a cargar la sesión"
        state.pending -> "Cambios del equipo pendientes de sincronizar"
        state.cached && state.team.isNotBlank() -> "Mostrando caché · esperando confirmación del servidor"
        else -> null
    }
    if(text!=null) Surface(color=MaterialTheme.colorScheme.tertiaryContainer,modifier=Modifier.fillMaxWidth()) {
        Text(text,Modifier.padding(horizontal=12.dp,vertical=8.dp),style=MaterialTheme.typography.bodySmall)
    }
}
