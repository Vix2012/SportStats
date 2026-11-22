package com.example.sportstats

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri

data class Exchange(val startMs: Long, val endMs: Long)

class ExchangeDetector(private val context: Context) {

    // Réglages à ajuster selon tes tests
    private val pasMs = 200L                 // une image regardée toutes les 200 ms
    private val seuilMouvement = 0.002f      // part de l'image qui doit bouger
    private val pauseFinMs = 4000L           // calme de 4 s = l'échange est fini
    private val dureeMiniMs = 3000L          // on ignore les échanges plus courts
    private val margeMs = 500L               // un peu de marge avant et après

    fun detect(videoUri: Uri): List<Exchange> {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val dureeTotale = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!
            .toLong()

        val exchanges = mutableListOf<Exchange>()

        fun ajouter(debut: Long, fin: Long) {
            if (fin - debut >= dureeMiniMs) {
                exchanges.add(
                    Exchange(
                        (debut - margeMs).coerceAtLeast(0L),
                        (fin + margeMs).coerceAtMost(dureeTotale)
                    )
                )
            }
        }

        var precedente: IntArray? = null
        var debut: Long? = null
        var dernierMouvement = 0L
        var t = 0L

        while (t < dureeTotale) {
            val grande = retriever.getFrameAtTime(
                t * 1000, MediaMetadataRetriever.OPTION_CLOSEST
            )
            if (grande != null) {
                val petite = Bitmap.createScaledBitmap(grande, 160, 90, false)
                if (petite !== grande) grande.recycle()
                val luminosites = luminosites(petite)
                petite.recycle()

                val avant = precedente
                if (avant != null) {
                    val actif = partQuiBouge(avant, luminosites) >= seuilMouvement
                    if (actif) {
                        if (debut == null) debut = t
                        dernierMouvement = t
                    } else if (debut != null && t - dernierMouvement >= pauseFinMs) {
                        ajouter(debut, dernierMouvement)
                        debut = null
                    }
                }
                precedente = luminosites
            }
            t += pasMs
        }
        if (debut != null) ajouter(debut, dernierMouvement)

        retriever.release()
        return exchanges
    }

    private fun luminosites(bmp: Bitmap): IntArray {
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        return IntArray(px.size) { i ->
            val c = px[i]
            (((c shr 16) and 0xFF) + ((c shr 8) and 0xFF) + (c and 0xFF)) / 3
        }
    }

    private fun partQuiBouge(a: IntArray, b: IntArray): Float {
        var compte = 0
        for (i in a.indices) {
            if (kotlin.math.abs(a[i] - b[i]) > 30) compte++
        }
        return compte.toFloat() / a.size
    }
}