package pe.edu.esan.sportpro.ui.screens.community

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import pe.edu.esan.sportpro.data.model.RolePolicy
import pe.edu.esan.sportpro.data.repository.*
import pe.edu.esan.sportpro.di.DefaultAppContainer
import pe.edu.esan.sportpro.ui.navigation.safeBack
import pe.edu.esan.sportpro.ui.navigation.BindSelectedTeam
import pe.edu.esan.sportpro.ui.viewmodel.CommunityViewModel
import pe.edu.esan.sportpro.ui.common.*
import pe.edu.esan.sportpro.ui.theme.SportProTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CommunityScreen(team:String,nav:NavHostController,embedded:Boolean=false) {
    BindSelectedTeam(team)
    val vm:CommunityViewModel=viewModel(key="community-$team",factory=object:ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(c:Class<T>):T =
            CommunityViewModel(team,CommunityRepository(FirebaseFirestore.getInstance(),FirebaseAuth.getInstance()),DefaultAppContainer().authRepository) as T
    })
    val state by vm.state.collectAsState();val moderator=RolePolicy.isStaff(state.profile?.role)
    var text by remember {mutableStateOf("")};var editing by remember {mutableStateOf<CommunityPost?>(null)}
    var report by remember {mutableStateOf<CommunityPost?>(null)};var reason by remember {mutableStateOf("")}
    Scaffold(topBar={if(!embedded)TopAppBar(title={Text("Comunidad")},navigationIcon={pe.edu.esan.sportpro.ui.navigation.PrimaryDestinationBackButton(nav)})}) {padding->
        LazyColumn(Modifier.padding(padding),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item {
                SportHero("DENTRO Y FUERA DE LA CANCHA","Somos equipo","Un espacio privado para celebrar, acompañar y compartir.")
                Spacer(Modifier.height(12.dp))
                StatusBadge("Sólo tu equipo y personal autorizado",BadgeTone.Positive)
            }
            state.error?.let {message->item {MessageNotice(message,true);TextButton(onClick=vm::clearError){Text("Cerrar aviso")}}}
            item {ElevatedCard(elevation=CardDefaults.elevatedCardElevation(defaultElevation=2.dp)) {Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(if(editing==null)"¿Qué comparte el equipo hoy?" else "Editar mi publicación",style=MaterialTheme.typography.titleMedium)
                OutlinedTextField(text,{text=it},placeholder={Text("Comparte una buena jugada, una idea o un mensaje de ánimo…")},
                    modifier=Modifier.fillMaxWidth(),minLines=3,maxLines=6,supportingText={Text("${text.length}/1000 · sin datos personales de menores")},isError=text.length>1000)
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.End) {
                    if(editing!=null)TextButton(onClick={editing=null;text=""},modifier=Modifier.heightIn(min=48.dp)){Text("Cancelar")}
                    Button(onClick={editing?.let {vm.edit(it,text)} ?: vm.post(text);text="";editing=null},enabled=state.profile!=null&&text.isNotBlank()&&text.length<=1000,modifier=Modifier.heightIn(min=48.dp)) {
                        Icon(Icons.Default.Send,null);Spacer(Modifier.width(8.dp));Text(if(editing==null)"Publicar" else "Guardar")
                    }
                }
            }}}
            if(state.loading)item {LinearProgressIndicator(Modifier.fillMaxWidth())}
            item {SectionHeading("El pulso del equipo","Novedades y conversaciones en tiempo real")}
            if(!state.loading&&state.error==null&&state.posts.isEmpty())item {EmptyState(Icons.Default.Favorite,"Tu equipo tiene mucho por contar","Sé el primero en compartir un mensaje. Evita contactos, datos médicos y ubicaciones de menores.")}
            items(state.posts,key={it.id}) {post->CommunityPostCard(post,post.authorUid==state.profile?.uid,moderator,
                onOpen={vm.open(post)},onEdit={editing=post;text=post.text},onReport={report=post;reason=""},onModerate={vm.moderate(post)},onReports={vm.reports(post)})}
        }
    }
    state.selected?.let {post->var comment by remember(post.id){mutableStateOf("")}
        AlertDialog(onDismissRequest=vm::close,title={Text("Conversación del equipo")},text={Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(post.text,style=MaterialTheme.typography.bodyLarge)
            FilledTonalButton(onClick=vm::react,enabled=post.state=="VISIBLE",modifier=Modifier.heightIn(min=48.dp)) {
                Icon(Icons.Default.Favorite,null);Spacer(Modifier.width(8.dp));Text("${if(state.thread.liked)"Quitar Me gusta" else "Me gusta"} · ${state.thread.likes}")
            }
            HorizontalDivider()
            if(state.thread.comments.isEmpty())Text("Todavía no hay comentarios. Inicia la conversación.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            state.thread.comments.forEach {c->Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceVariant) {Column(Modifier.padding(12.dp)) {
                Text(if(c.state=="HIDDEN")"[Oculto por moderación] ${c.text}" else c.text)
                if(moderator&&c.state=="VISIBLE")TextButton(onClick={vm.hideComment(c)},modifier=Modifier.heightIn(min=48.dp)){Text("Ocultar comentario")}
            }}}
            OutlinedTextField(comment,{comment=it},label={Text("Tu comentario")},supportingText={Text("${comment.length}/500")},isError=comment.length>500,modifier=Modifier.fillMaxWidth())
            Button(onClick={vm.comment(comment);comment=""},enabled=post.state=="VISIBLE"&&comment.isNotBlank()&&comment.length<=500,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Comentar")}
        }},confirmButton={TextButton(onClick=vm::close){Text("Cerrar")}})
    }
    report?.let {post->AlertDialog(onDismissRequest={report=null},title={Text("Ayúdanos a cuidar el equipo")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("El motivo sólo lo ve moderación. No añadas datos privados innecesarios.")
        OutlinedTextField(reason,{reason=it},label={Text("Motivo del reporte")},supportingText={Text("${reason.length}/300")})
    }},confirmButton={TextButton(onClick={vm.report(post,reason);report=null},enabled=reason.isNotBlank()&&reason.length<=300){Text("Enviar reporte")}},dismissButton={TextButton(onClick={report=null}){Text("Cancelar")}})}
    state.reports?.let {reports->AlertDialog(onDismissRequest=vm::closeReports,title={Text("Reportes · moderación")},text={LazyColumn(Modifier.heightIn(max=350.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        if(reports.isEmpty())item {Text("Esta publicación no tiene reportes.")}
        items(reports){Text("Miembro ${it.reporter.take(8)}: ${it.reason}")}
    }},confirmButton={TextButton(onClick=vm::closeReports){Text("Cerrar")}})}
}

@Composable
fun CommunityPostCard(post:CommunityPost,own:Boolean,moderator:Boolean,onOpen:()->Unit={},onEdit:()->Unit={},onReport:()->Unit={},onModerate:()->Unit={},onReports:()->Unit={}) {
    var menu by remember {mutableStateOf(false)}
    ElevatedCard(Modifier.fillMaxWidth().animateContentSize(),elevation=CardDefaults.elevatedCardElevation(defaultElevation=2.dp)) {Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Surface(shape=CircleShape,color=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.size(44.dp)) {Box(contentAlignment=Alignment.Center){Icon(Icons.Default.Person,null,tint=MaterialTheme.colorScheme.onPrimaryContainer)}}
            Column(Modifier.weight(1f)) {
                Text(if(own)"Mi publicación" else "Integrante del equipo",style=MaterialTheme.typography.titleSmall)
                Text(if(post.at>0)SimpleDateFormat("dd MMM · HH:mm",Locale.getDefault()).format(Date(post.at)) else "Hora pendiente",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(onClick={menu=true},modifier=Modifier.size(48.dp)){Icon(Icons.Default.MoreVert,"Opciones de publicación")}
                DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                    if(own&&post.state=="VISIBLE")DropdownMenuItem(text={Text("Editar")},onClick={menu=false;onEdit()})
                    if(post.state=="VISIBLE")DropdownMenuItem(text={Text("Reportar")},onClick={menu=false;onReport()})
                    if(moderator) {
                        DropdownMenuItem(text={Text(if(post.state=="HIDDEN")"Restaurar publicación" else "Ocultar publicación")},onClick={menu=false;onModerate()})
                        DropdownMenuItem(text={Text("Ver reportes")},onClick={menu=false;onReports()})
                    }
                }
            }
        }
        if(post.state=="HIDDEN")StatusBadge("Oculta por moderación",BadgeTone.Warning)
        Text(post.text,style=MaterialTheme.typography.bodyLarge)
        HorizontalDivider()
        TextButton(onClick=onOpen,enabled=post.state=="VISIBLE"||moderator,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
            Icon(Icons.Default.Favorite,null);Spacer(Modifier.width(8.dp));Text("Reaccionar y conversar")
        }
    }}
}

@Preview(showBackground=true,widthDp=390)
@Composable private fun CommunityPreview() {SportProTheme {CommunityPostCard(CommunityPost("demo","Gran esfuerzo del equipo. Cada entrenamiento cuenta. ¡Vamos por la próxima jugada!","demo","VISIBLE",0),true,false)}}
@Preview(showBackground=true,widthDp=390)
@Composable private fun CommunityDarkPreview() {SportProTheme(darkTheme=true) {CommunityPostCard(CommunityPost("demo","Gracias por acompañar al equipo.","demo","VISIBLE",0),false,true)}}
