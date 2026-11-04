package com.kanaani.ibo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kanaani.ibo.data.Match
import com.kanaani.ibo.data.Matches
import com.kanaani.ibo.data.Xtream
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** « Matchs du jour » sur l'accueil : heure (ou « en direct »), équipes, et la chaîne — un appui l'ouvre en plein écran. */
@Composable
fun MatchesRow(nav: Navigator, x: Xtream, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { while (true) { Matches.refresh(); delay(60_000L) } }
    val list = Matches.list
    if (list.isEmpty()) return
    val scope = rememberCoroutineScope()
    Column(modifier) {
        Label("⚽ " + Str.todayMatches, 16.sp, C.text, FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(list, key = { it.id }) { m ->
                Tile({ if (m.channelId != null) scope.launch { open(nav, x, m) } }, Modifier.width(270.dp).height(132.dp), RoundedCornerShape(16.dp),
                    colors = listOf(C.card), focusColors = C.live, focusScale = 1.04f) {
                    Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Label(if (m.live) "● ${Str.liveNow}" else "${clock(m.start)} · ${m.league}", 12.sp, if (m.live) Color(0xFFFF6B6B) else C.text2, FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Team(m.home, m.homeLogo, Modifier.weight(1f))
                            Label("×", 16.sp, C.text3, FontWeight.Bold)
                            Team(m.away, m.awayLogo, Modifier.weight(1f))
                        }
                        Label(m.channelName?.let { "▶ $it" } ?: Str.chSoon, 12.sp, if (m.channelName != null) Color(0xFF4ADE80) else C.text3, FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Team(name: String, logo: String?, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            if (logo != null) AsyncImage(logo, null, Modifier.fillMaxSize()) else Label("⚽", 22.sp)
        }
        Spacer(Modifier.height(4.dp))
        Label(name, 12.sp, C.text, FontWeight.SemiBold)
    }
}

/** Ouvre la chaîne du match en plein écran, avec sa catégorie pour zapper. */
private suspend fun open(nav: Navigator, x: Xtream, m: Match) {
    val id = m.channelId ?: return
    val list = runCatching { x.channels(m.channelCat?.takeIf { it.isNotBlank() }) }.getOrDefault(emptyList())
    val i = list.indexOfFirst { it.id == id }
    if (i >= 0) nav.go(Screen.Play(list[i].name, x.liveUrl(id), live = true, channels = list, index = i))
    else nav.go(Screen.Play(m.channelName ?: "", x.liveUrl(id), live = true))
}
