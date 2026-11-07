package pe.edu.esan.sportpro.ui.common

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.ui.theme.HeroStart
import pe.edu.esan.sportpro.ui.theme.HeroEnd

enum class BadgeTone { Neutral, Positive, Warning, Danger }

@Composable
fun StatusBadge(text:String,tone:BadgeTone,modifier:Modifier=Modifier) {
    val c=MaterialTheme.colorScheme
    val colors=when(tone) {
        BadgeTone.Positive -> c.primaryContainer to c.onPrimaryContainer
        BadgeTone.Warning -> c.tertiaryContainer to c.onTertiaryContainer
        BadgeTone.Danger -> c.errorContainer to c.onErrorContainer
        BadgeTone.Neutral -> c.surfaceVariant to c.onSurfaceVariant
    }
    Surface(modifier,shape=CircleShape,color=colors.first,contentColor=colors.second) {
        Text(text,Modifier.padding(horizontal=12.dp,vertical=7.dp),style=MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun SportHero(eyebrow:String,title:String,subtitle:String,modifier:Modifier=Modifier,
    content:@Composable ColumnScope.()->Unit={}) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .background(Brush.linearGradient(listOf(HeroStart,HeroEnd))).padding(24.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(eyebrow,style=MaterialTheme.typography.labelLarge,color=Color.White)
        Text(title,style=MaterialTheme.typography.headlineLarge,color=Color.White)
        if(subtitle.isNotBlank())Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=Color.White)
        content()
    }
}

@Composable
fun SectionHeading(title:String,subtitle:String="") {
    Column(Modifier.padding(top=8.dp,bottom=4.dp)) {
        Text(title,style=MaterialTheme.typography.titleLarge)
        if(subtitle.isNotBlank())Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ActionTile(title:String,description:String,icon:ImageVector,onClick:()->Unit,modifier:Modifier=Modifier) {
    ElevatedCard(onClick=onClick,modifier=modifier.fillMaxWidth().heightIn(min=96.dp),
        elevation=CardDefaults.elevatedCardElevation(defaultElevation=2.dp)) {
        Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon,null,Modifier.padding(12.dp).size(24.dp),tint=MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(title,style=MaterialTheme.typography.titleMedium)
                Text(description,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward,null,tint=MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun EmptyState(icon:ImageVector,title:String,description:String,modifier:Modifier=Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=32.dp),
        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Surface(shape=CircleShape,color=MaterialTheme.colorScheme.primaryContainer) {
            Icon(icon,null,Modifier.padding(20.dp).size(32.dp),tint=MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(title,style=MaterialTheme.typography.titleMedium,textAlign=TextAlign.Center)
        Text(description,style=MaterialTheme.typography.bodyMedium,textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun BackButton(onClick:()->Unit) {
    IconButton(onClick=onClick,modifier=Modifier.size(48.dp)) {Icon(Icons.AutoMirrored.Filled.ArrowBack,contentDescription="Volver")}
}

@Composable
fun MessageNotice(message:String,isError:Boolean=false) {
    val c=MaterialTheme.colorScheme
    Surface(color=if(isError)c.errorContainer else c.secondaryContainer,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth().animateContentSize()) {
        Text(message,Modifier.padding(16.dp),style=MaterialTheme.typography.bodyMedium,color=if(isError)c.onErrorContainer else c.onSecondaryContainer)
    }
}
