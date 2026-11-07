package pe.edu.esan.sportpro.ui.screens.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import pe.edu.esan.sportpro.ui.viewmodel.CommunityViewModel

@Composable
fun CommunityScreen(team:String,nav:NavHostController) {
    val vm:CommunityViewModel=viewModel(key="community-$team",factory=object:ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(c:Class<T>):T =
            CommunityViewModel(team,CommunityRepository(FirebaseFirestore.getInstance(),FirebaseAuth.getInstance()),DefaultAppContainer().authRepository) as T
    })
    val state by vm.state.collectAsState();val moderator=RolePolicy.isStaff(state.profile?.role)
    var text by remember {mutableStateOf("")};var editing by remember {mutableStateOf<CommunityPost?>(null)}
    var report by remember {mutableStateOf<CommunityPost?>(null)};var reason by remember {mutableStateOf("")}
    Scaffold(topBar={TopAppBar(title={Text("Comunidad del equipo")},navigationIcon={TextButton(onClick={nav.safeBack()}){Text("Volver")}})}) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {
                Text("Visible sólo para integrantes del equipo y personal autorizado. No publiques contactos, datos médicos ni ubicaciones de menores.")
                state.error?.let {Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick=vm::clearError){Text("Cerrar aviso")}}
                OutlinedTextField(text,{text=it},label={Text(if(editing==null)"Publicación (hasta 1000 caracteres)" else "Editar mi publicación")},modifier=Modifier.fillMaxWidth())
                Row {
                    Button(onClick={editing?.let {vm.edit(it,text)} ?: vm.post(text);text="";editing=null},enabled=state.profile!=null&&text.isNotBlank()&&text.length<=1000){Text(if(editing==null)"Publicar" else "Guardar")}
                    if(editing!=null)TextButton(onClick={editing=null;text=""}){Text("Cancelar edición")}
                }
            }
            items(state.posts,key={it.id}) { post ->Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(12.dp)) {
                Text(if(post.authorUid==state.profile?.uid)"Mi publicación" else "Integrante del equipo",style=MaterialTheme.typography.labelMedium)
                if(post.state=="HIDDEN")Text("Oculta por moderación",color=MaterialTheme.colorScheme.error)
                Text(post.text)
                Row {
                    TextButton(onClick={vm.open(post)}){Text("Comentarios / Me gusta")}
                    if(post.authorUid==state.profile?.uid&&post.state=="VISIBLE")TextButton(onClick={editing=post;text=post.text}){Text("Editar")}
                }
                Row {
                    TextButton(onClick={report=post;reason=""}){Text("Reportar")}
                    if(moderator){TextButton(onClick={vm.moderate(post)}){Text(if(post.state=="HIDDEN")"Restaurar" else "Ocultar")};TextButton(onClick={vm.reports(post)}){Text("Ver reportes")}}
                }
            }} }
        }
    }
    state.selected?.let { post ->var comment by remember(post.id) {mutableStateOf("")}
        AlertDialog(onDismissRequest=vm::close,title={Text("Conversación del equipo")},text={Column(Modifier.heightIn(max=450.dp)) {
            Text(post.text,maxLines=4)
            TextButton(onClick=vm::react,enabled=post.state=="VISIBLE"){Text("${if(state.thread.liked)"Quitar Me gusta" else "Me gusta"} (${state.thread.likes})")}
            LazyColumn(Modifier.weight(1f,false)) {items(state.thread.comments,key={it.id}) { c ->
                Text(if(c.state=="HIDDEN")"[Oculto por moderación] ${c.text}" else c.text)
                if(moderator&&c.state=="VISIBLE")TextButton(onClick={vm.hideComment(c)}){Text("Ocultar comentario")}
            }}
            OutlinedTextField(comment,{comment=it},label={Text("Comentario (hasta 500 caracteres)")})
            Button(onClick={vm.comment(comment);comment=""},enabled=post.state=="VISIBLE"&&comment.isNotBlank()&&comment.length<=500){Text("Comentar")}
        }},confirmButton={TextButton(onClick=vm::close){Text("Cerrar")}})
    }
    report?.let { post ->AlertDialog(onDismissRequest={report=null},title={Text("Reportar publicación")},text={Column {
        Text("El motivo sólo lo ve el personal de moderación. No incluyas datos privados innecesarios.")
        OutlinedTextField(reason,{reason=it},label={Text("Motivo (hasta 300 caracteres)")})
    }},confirmButton={TextButton(onClick={vm.report(post,reason);report=null},enabled=reason.isNotBlank()&&reason.length<=300){Text("Enviar reporte")}},dismissButton={TextButton(onClick={report=null}){Text("Cancelar")}}) }
    state.reports?.let { reports ->AlertDialog(onDismissRequest=vm::closeReports,title={Text("Reportes: sólo moderación")},text={LazyColumn {items(reports){Text("Miembro ${it.reporter.take(8)}: ${it.reason}")}}},confirmButton={TextButton(onClick=vm::closeReports){Text("Cerrar")}}) }
}
