package com.kanaani.ibo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanaani.ibo.Brand
import com.kanaani.ibo.BuildConfig
import com.kanaani.ibo.data.Account
import com.kanaani.ibo.data.Xtream
import kotlinx.coroutines.delay

/** Infos d'abonnement, lues une fois par session (accueil, compte). */
object AccountCache {
    var value by mutableStateOf<Account?>(null)
    suspend fun load(x: Xtream) { if (value == null) value = x.account() }
}

fun expiryLine(a: Account?): String {
    a ?: return ""
    // Compte d'essai : c'est la fin de l'essai qui compte, pas celle de la ligne du serveur.
    val trialEnd = if (com.kanaani.ibo.data.Store.trialLogin() != null) com.kanaani.ibo.data.Trial.expires.takeIf { it > 0 } else null
    val exp = trialEnd ?: a.expiresAt ?: return "${Str.expires}: ${Str.unlimited}"
    val days = ((exp - System.currentTimeMillis()).coerceAtLeast(0) + 86_399_999L) / 86_400_000L
    return "${Str.expires} ${date(exp)} · $days ${Str.daysLeft}"
}

@Composable
fun HomeScreen(nav: Navigator, x: Xtream) {
    LaunchedEffect(x) { AccountCache.load(x) }
    // « Tous » films et séries préparés en arrière-plan pendant qu'on est sur l'accueil.
    LaunchedEffect(x) { x.prefetch() }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(60_000L - now % 60_000L) } }
    LaunchedEffect(Unit) { com.kanaani.ibo.data.Ads.refresh() }
    LaunchedEffect(Unit) { while (true) { com.kanaani.ibo.data.Matches.refresh(); delay(60_000L) } }
    val first = remember { FocusRequester() }
    InitialFocus(first)
    val ads = com.kanaani.ibo.data.Ads.billboard

    // Kanaani Sport : un seul thème — pub + direct à gauche, matchs du jour à droite, petits boutons en bas.
    Backdrop {
        Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Logo(64.dp)
                Spacer(Modifier.width(14.dp))
                Text(Brand.name, color = C.text, fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 28.sp, maxLines = 1, modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(clock(now), color = C.text, fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 26.sp, maxLines = 1)
                    Label(date(now, "EEEE d MMMM"), 12.sp, C.text2)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1.45f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (ads.isNotEmpty()) AdBox(ads, com.kanaani.ibo.data.Ads.interval, Modifier.fillMaxWidth().weight(1f))
                    BigTile(Str.live, Icons.Live, C.live, (if (ads.isEmpty()) Modifier.weight(1f) else Modifier.height(120.dp)).fillMaxWidth().focusRequester(first)) { nav.go(Screen.Live()) }
                }
                MatchesPanel(nav, x, Modifier.weight(1f).fillMaxHeight())
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().height(60.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SmallTile(Str.favorites, Icons.Heart, Modifier.weight(1f)) { nav.go(Screen.Favorites) }
                SmallTile(Str.account, Icons.Account, Modifier.weight(1f)) { nav.go(Screen.Account) }
                SmallTile(Str.settings, Icons.Settings, Modifier.weight(1f)) { nav.go(Screen.Settings) }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Label(expiryLine(AccountCache.value), 12.sp, C.text2, modifier = Modifier.weight(1f))
                Label("v${BuildConfig.VERSION_NAME}", 11.sp, C.text3)
            }
        }
    }
}

/** Image de pub du panneau (tourne toutes les [interval] s). */
@Composable
private fun AdBox(list: List<String>, interval: Int, modifier: Modifier) {
    var i by remember(list) { mutableStateOf(0) }
    LaunchedEffect(list, interval) { while (list.size > 1) { delay(interval.coerceAtLeast(3) * 1000L); i = (i + 1) % list.size } }
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(C.card)) {
        coil3.compose.AsyncImage(list[i % list.size], null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    }
}

@Composable
private fun BigTile(label: String, icon: String, colors: List<Color>, modifier: Modifier, onClick: () -> Unit) {
    Tile(onClick, modifier.fillMaxSize(), RoundedCornerShape(20.dp), colors = colors) {
        // Icône et texte côte à côte : le texte reste visible même quand la tuile est basse (sous la pub).
        Row(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, 56.dp, Color.White, stroke = 1.4f)
            Spacer(Modifier.width(18.dp))
            Text(label, color = Color.White, fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 30.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SmallTile(label: String, icon: String, modifier: Modifier, onClick: () -> Unit) {
    Tile(onClick, modifier.fillMaxSize(), RoundedCornerShape(14.dp), colors = C.small, focusColors = C.live) {
        Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, 28.dp, Color.White, stroke = 1.8f)
            Spacer(Modifier.width(12.dp))
            Label(label, 16.sp, weight = FontWeight.SemiBold)
        }
    }
}
