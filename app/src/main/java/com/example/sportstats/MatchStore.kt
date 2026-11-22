package com.example.sportstats

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class MatchRecord(
    val id: Long,
    val dateMs: Long,
    val titre: String,
    val echanges: List<Exchange>,
    val images: Int,
    val imagesAvecJoueurs: Int
)
object MatchStore {

    private const val FICHIER = "matches.json"

    @Synchronized
    fun charger(context: Context): List<MatchRecord> {
        val f = File(context.filesDir, FICHIER)
        if (!f.exists()) return emptyList()
        return try {
            val liste = JSONArray(f.readText())
            List(liste.length()) { i ->
                val o = liste.getJSONObject(i)
                val ech = o.getJSONArray("echanges")
                MatchRecord(
                    id = o.getLong("id"),
                    dateMs = o.getLong("dateMs"),
                    titre = o.getString("titre"),
                    echanges = List(ech.length()) { j ->
                        val e = ech.getJSONObject(j)
                        Exchange(e.getLong("debut"), e.getLong("fin"))
                    },
                    images = o.getInt("images"),
                    imagesAvecJoueurs = o.getInt("imagesAvecJoueurs")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun ajouter(context: Context, match: MatchRecord) {
        ecrire(context, charger(context) + match)
    }

    @Synchronized
    fun supprimer(context: Context, id: Long) {
        ecrire(context, charger(context).filter { it.id != id })
    }

    private fun ecrire(context: Context, matchs: List<MatchRecord>) {
        val liste = JSONArray()
        for (m in matchs) {
            val ech = JSONArray()
            for (e in m.echanges) {
                ech.put(JSONObject().put("debut", e.startMs).put("fin", e.endMs))
            }
            liste.put(
                JSONObject()
                    .put("id", m.id)
                    .put("dateMs", m.dateMs)
                    .put("titre", m.titre)
                    .put("echanges", ech)
                    .put("images", m.images)
                    .put("imagesAvecJoueurs", m.imagesAvecJoueurs)
            )
        }
        // On écrit dans un fichier temporaire puis on le renomme,
        // pour ne jamais se retrouver avec un fichier à moitié écrit
        val temporaire = File(context.filesDir, "$FICHIER.tmp")
        temporaire.writeText(liste.toString())
        temporaire.renameTo(File(context.filesDir, FICHIER))
    }
}