package com.example.sportstats

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker

data class FramePoses(
    val timestampMs: Long,
    val players: List<List<NormalizedLandmark>>
)

class PoseAnalyzer(private val context: Context) {

    private val landmarker: PoseLandmarker

    init {
        val base = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker_full.task")
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(base)
            .setRunningMode(RunningMode.VIDEO)
            .setNumPoses(2)
            .build()

        landmarker = PoseLandmarker.createFromOptions(context, options)
    }

    fun analyze(
        videoUri: Uri,
        startMs: Long = 0L,
        endMs: Long? = null,
        everyNthMs: Long = 50
    ): List<FramePoses> {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)

        val dureeTotale = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!
            .toLong()
        val fin = endMs ?: dureeTotale

        val results = mutableListOf<FramePoses>()
        var t = startMs
        while (t < fin) {
            val bitmap = retriever.getFrameAtTime(
                t * 1000, MediaMetadataRetriever.OPTION_CLOSEST
            )
            if (bitmap != null) {
                val mpImage = BitmapImageBuilder(bitmap).build()
                val result = landmarker.detectForVideo(mpImage, t)
                results.add(FramePoses(t, result.landmarks()))
            }
            t += everyNthMs
        }
        retriever.release()
        return results
    }

    fun close() {
        landmarker.close()
    }
}