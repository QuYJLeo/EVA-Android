package com.google.mediapipe.examples.facedetection.utils

object Constants {

    val facialExpression = "facialExpression"
    val facialExpression2 = "facialExpression2"
    val ONNXArray =
        arrayOf(
//            "Neutral", "Happy", "Sad", "Surprise", "Fear", "Disgust", "Angry", "Contempt"
            "中性", "高兴", "悲伤", "惊讶", "恐惧", "厌恶", "生气", "蔑视"
        )
    enum class Onnx4 {
        valence, arousal,
    }
}