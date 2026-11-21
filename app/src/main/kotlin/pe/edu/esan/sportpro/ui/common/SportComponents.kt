package pe.edu.esan.sportpro.ui.common

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.ui.theme.HeroStart
import pe.edu.esan.sportpro.ui.theme.HeroEnd
import pe.edu.esan.sportpro.ui.theme.HeroText
import pe.edu.esan.sportpro.ui.theme.HeroMuted
import pe.edu.esan.sportpro.ui.theme.HeroAccent

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
    Column(modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
        .background(Brush.linearGradient(listOf(HeroStart,HeroEnd))).padding(24.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        CompositionLocalProvider(LocalContentColor provides HeroText) {
            Box(Modifier.width(32.dp).height(4.dp).background(HeroAccent,CircleShape))
            Text(eyebrow,style=MaterialTheme.typography.labelLarge,color=HeroAccent)
            Text(title,style=MaterialTheme.typography.headlineLarge,color=HeroText)
            if(subtitle.isNotBlank())Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=HeroMuted)
            content()
        }
    }
}

@Composable
fun SectionHeading(title:String,subtitle:String="") {
    Column(Modifier.padding(top=8.dp,bottom=4.dp)) {
        Text(title,style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.onSurface)
        if(subtitle.isNotBlank())Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ActionTile(title:String,description:String,icon:ImageVector,onClick:()->Unit,modifier:Modifier=Modifier) {
    val c=MaterialTheme.colorScheme
    Card(onClick=onClick,modifier=modifier.fillMaxWidth().heightIn(min=96.dp),
        shape=MaterialTheme.shapes.large,
        border=BorderStroke(1.dp,c.outlineVariant),
        colors=CardDefaults.cardColors(containerColor=c.surfaceContainerLow,contentColor=c.onSurface),
        elevation=CardDefaults.cardElevation(defaultElevation=0.dp,pressedElevation=0.dp)) {
        Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Surface(shape=MaterialTheme.shapes.medium,color=c.primaryContainer,contentColor=c.onPrimaryContainer) {
                Icon(icon,null,Modifier.padding(12.dp).size(24.dp),tint=MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(title,style=MaterialTheme.typography.titleMedium)
                if(description.isNotBlank()) Text(description,style=MaterialTheme.typography.bodyMedium,color=c.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward,null,modifier=Modifier.size(20.dp),tint=c.primary)
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
    val c=MaterialTheme.colorScheme
    IconButton(onClick=onClick,modifier=Modifier.size(48.dp).clip(MaterialTheme.shapes.small)
        .background(c.surfaceContainerHigh)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack,contentDescription="Volver",tint=c.onSurface)
    }
}

@Composable
fun MessageNotice(message:String,isError:Boolean=false) {
    val c=MaterialTheme.colorScheme
    Surface(color=if(isError)c.errorContainer else c.secondaryContainer,shape=MaterialTheme.shapes.medium,
        modifier=Modifier.fillMaxWidth().animateContentSize().semantics { liveRegion=LiveRegionMode.Polite }) {
        Text(message,Modifier.padding(16.dp),style=MaterialTheme.typography.bodyMedium,color=if(isError)c.onErrorContainer else c.onSecondaryContainer)
    }
}
