package com.kanaani.ibo.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kanaani.ibo.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.Request

/** Match du jour (sans score) et la chaîne où le regarder (trouvée par le panneau). */
data class Match(
    val id: String, val league: String, val home: String, val away: String, val homeLogo: String?, val awayLogo: String?,
    val start: Long, val live: Boolean, val channelId: String?, val channelName: String?, val channelCat: String?,
)

/** « Matchs du jour » : lus depuis le panneau (/api/matches), rafraîchis toutes les 5 minutes. */
object Matches {
    var list by mutableStateOf<List<Match>>(emptyList())
        private set
    private var at = 0L

    suspend fun refresh(force: Boolean = false) {
        if (!force && System.currentTimeMillis() - at < 5 * 60_000L) return
        val base = BuildConfig.PANEL_URL.trimEnd('/')
        if (base.isBlank()) return
        val root = withContext(Dispatchers.IO) {
            runCatching {
                Http.client.newCall(Request.Builder().url("$base/api/matches").build()).execute().use { r ->
                    if (r.isSuccessful) Json.parseToJsonElement(r.body.string()) as? JsonObject else null
                }
            }.getOrNull()
        } ?: return
        at = System.currentTimeMillis()
        list = (root["leagues"] as? JsonArray).orEmpty().flatMap { l ->
            val lo = l as? JsonObject ?: return@flatMap emptyList()
            val league = (lo["name"] as? JsonPrimitive)?.contentOrNull.orEmpty()
            (lo["matches"] as? JsonArray).orEmpty().mapNotNull { e ->
                val o = e as? JsonObject ?: return@mapNotNull null
                fun s(k: String) = (o[k] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
                val ch = o["channel"] as? JsonObject
                fun c(k: String) = (ch?.get(k) as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
                Match(s("id") ?: return@mapNotNull null, league, s("home").orEmpty(), s("away").orEmpty(), s("homeLogo"), s("awayLogo"),
                    s("start")?.toLongOrNull() ?: 0L, s("live") == "true", c("id"), c("name"), c("cat"))
            }
        }
    }
}
