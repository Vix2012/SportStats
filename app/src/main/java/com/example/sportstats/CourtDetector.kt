package com.example.sportstats

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.NormalizedKeypoint
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.interactivesegmenter.InteractiveSegmenter
import com.google.mediapipe.tasks.vision.interactivesegmenter.InteractiveSegmenterOptions
import com.google.mediapipe.tasks.vision.interactivesegmenter.Stroke
import java.nio.ByteOrder

object CourtDetector {

    // xNorm et yNorm : le point touché, entre 0 et 1
    // Renvoie 4 coins (haut-gauche, haut-droite, bas-droite, bas-gauche), entre 0 et 1
    fun detect(context: Context, bitmap: Bitmap, xNorm: Float, yNorm: Float): List<Offset>? {
        val base = BaseOptions.builder()
            .setModelAssetPath("interactive_segmentation.task")
            .build()
        val options = InteractiveSegmenterOptions.builder()
            .setBaseOptions(base)
            .build()
        val segmenter = InteractiveSegmenter.createFromOptions(context, options)

        try {
            segmenter.setImage(BitmapImageBuilder(bitmap).build())
            val trait = Stroke.builder()
                .setBrushMode(Stroke.BrushMode.POSITIVE)
                .setPoints(listOf(NormalizedKeypoint.create(xNorm, yNorm)))
                .setCompleted(true)
                .build()
            val masque = segmenter.segment(listOf(trait))
            return coinsDuMasque(masque)
        } finally {
            segmenter.close()
        }
    }

    private fun coinsDuMasque(masque: MPImage): List<Offset>? {
        val w = masque.width
        val h = masque.height
        val tampon = ByteBufferExtractor.extract(masque)
        tampon.order(ByteOrder.nativeOrder())
        val valeurs = tampon.asFloatBuffer()
        if (valeurs.remaining() < w * h) return null

        var minSomme = Float.MAX_VALUE
        var maxSomme = -Float.MAX_VALUE
        var minDiff = Float.MAX_VALUE
        var maxDiff = -Float.MAX_VALUE
        var hautGauche = Offset.Zero
        var basDroite = Offset.Zero
        var hautDroite = Offset.Zero
        var basGauche = Offset.Zero
        var nombre = 0

        for (y in 0 until h) {
            for (x in 0 until w) {
                if (valeurs.get(y * w + x) > 0.5f) {
                    nombre++
                    val somme = (x + y).toFloat()
                    val diff = (x - y).toFloat()
                    val p = Offset(x.toFloat(), y.toFloat())
                    if (somme < minSomme) { minSomme = somme; hautGauche = p }
                    if (somme > maxSomme) { maxSomme = somme; basDroite = p }
                    if (diff > maxDiff) { maxDiff = diff; hautDroite = p }
                    if (diff < minDiff) { minDiff = diff; basGauche = p }
                }
            }
        }

        // Zone détectée trop petite : on abandonne
        if (nombre < (w * h) / 50) return null

        return listOf(hautGauche, hautDroite, basDroite, basGauche)
            .map { Offset(it.x / w, it.y / h) }
    }
}