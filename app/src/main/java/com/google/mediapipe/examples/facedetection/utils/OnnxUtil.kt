package com.google.mediapipe.examples.facedetection.utils

import ai.onnxruntime.NodeInfo
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
import ai.onnxruntime.TensorInfo
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import com.jeremyliao.liveeventbus.LiveEventBus
import java.io.ByteArrayOutputStream
import java.nio.FloatBuffer
import java.util.Collections
import kotlin.math.exp

object OnnxUtil {
    // onnxruntime 环境
    lateinit var env: OrtEnvironment
    lateinit var session: OrtSession

    // 模型输入
    var w = 0
    var h = 0
    var c = 3
    var radius = 1
    var tickness = 2

    // 模型加载
    fun loadModule(assetManager: AssetManager) {
        w = 224
        h = 224
        c = 3
        try {

//            InputStream inputStream = assetManager.open("hand_landmark_sparse_Nx3x224x224.onnx");
            val inputStream = assetManager.open("mediapipe_shuffleNet_va_prune.onnx")
            val buffer = ByteArrayOutputStream()
            var nRead: Int
            val data = ByteArray(1024)
            while (inputStream.read(data, 0, data.size).also { nRead = it } != -1) {
                buffer.write(data, 0, nRead)
            }
            buffer.flush()
            val module = buffer.toByteArray()
            println("开始加载模型")
            env = OrtEnvironment.getEnvironment()
            session = env.createSession(module, SessionOptions())
            session.getInputInfo().entries.stream()
                .forEach { (inputName, inputInfo): Map.Entry<String, NodeInfo> ->
                    val shape = (inputInfo.info as TensorInfo).shape
                    val javaType = (inputInfo.info as TensorInfo).type.toString()
                    println("模型输入:  " + inputName + " -> " + shape.contentToString() + " -> " + javaType)
                }
            session.getOutputInfo().entries.stream()
                .forEach { (outputName, outputInfo): Map.Entry<String, NodeInfo> ->
                    val shape = (outputInfo.info as TensorInfo).shape
                    val javaType = (outputInfo.info as TensorInfo).type.toString()
                    println("模型输出:  " + outputName + " -> " + shape.contentToString() + " -> " + javaType)
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun softmax(x: FloatArray, dim: Int = 0): FloatArray {
        val xMax = x.maxOrNull() ?: 0.0f
        val xExp = x.map { exp(it - xMax) }.toFloatArray()
        val xSum = xExp.sum()
        val softmaxValue = xExp.map { it / xSum }.toFloatArray()
        return softmaxValue
    }


    fun inferencr2(copy: Bitmap?) {
        if (copy == null) return
        val w = 224
        val h = 224
        val c = 3

        // 修改图像尺寸
        var bitmap = Bitmap.createScaledBitmap(copy, w, h, false)

        // 提取rgb(chw存储)并做归一化
        val rgb = FloatArray(c * h * w)

        // 从bitmap中提取像素数据,每个位置可以分别计算rgb三个分量
        val pixels = IntArray(w * h)
        bitmap!!.getPixels(pixels, 0, w, 0, 0, w, h)

        // chw的排放在一维数组中是这样的,以5个像素点为例
        // rrrrr ggggg bbbbb

        // 遍历每个像素点 w*h个
        for (i in pixels.indices) {
            val pixel = pixels[i]
            // rgb分量归一化处理
            val r = (pixel shr 16 and 0xFF) / 255f
            val g = (pixel shr 8 and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f
            // 存储到一维数组中
            rgb[i] = r
            rgb[i + w * h] = g
            rgb[i + w * h + w * h] = b
        }

        // 创建张量并进行推理
        try {
            val tensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(rgb),
                longArrayOf(1, c.toLong(), w.toLong(), h.toLong())
            )
            println("张量创建成功")
            var inputName = ""
            if (session.inputNames != null && session.inputNames.iterator()
                    .hasNext()
            ) {
                inputName = session.inputNames.iterator().next()
            }


            //            OrtSession.Result output = session.run(Collections.singletonMap("input", tensor));
            val output = session.run(Collections.singletonMap(inputName, tensor))
            println("推理成功")

            // 解析输出,处理目标框(模型做了合并nms处理)
            // 8种表情
            val batchno_classid_0 = output[0].value as Array<FloatArray>
            val emotions = batchno_classid_0[0]
//            val a1 = emotions[0]
//            val a2 = emotions[1]
//            val a3 = emotions[2]
//            val a4 = emotions[3]
//            val a5 = emotions[4]
//            val a6 = emotions[5]
//            val a7 = emotions[6]
//            val a8 = emotions[7]
//            println("HHH a1: $a1, a2: $a2, a3: $a3, a4: $a4, a5: $a5, a6: $a6, a7: $a7, a8: $a8")

            val result_softmax = softmax(emotions)
            println("HHH softmax" + result_softmax.contentToString())


            val maxByOrNull = result_softmax.maxByOrNull { it }
            val maxIndex = result_softmax.indices.maxByOrNull { result_softmax[it] }
                ?: -1 // 获取最大值所在的索引
            Log.d("HHH max", "MAX index:${maxByOrNull.toString()}" + " 角标：" + maxIndex)


            // v and a
            val batchno_classid_4 = output[4].value as Array<FloatArray>

//            println("batchno_classid_4: [${batchno_classid_4.size}, ${batchno_classid_4[0].size}]") // 打印 mfcc 的形状
//            for (i in batchno_classid_4.indices) {
//                println("Dimension ${i + 1}: ${batchno_classid_4[i].size}")
//            }
            val valence = batchno_classid_4[0][0]
            val arousal = batchno_classid_4[0][1]
            // 打印结果
//            println("HHH valence: $valence")
//            println("HHH arousal: $arousal")

            Log.d("tensor", "batchno_classid_4:${batchno_classid_4.toString()}")
            if (maxIndex > -1)
                LiveEventBus.get<String>(Constants.facialExpression, String::class.java)
                    .post(Constants.ONNXArray[maxIndex])

            LiveEventBus.get<FloatArray>(Constants.facialExpression2, FloatArray::class.java)
                .post(batchno_classid_4[0])
        } catch (e: Exception) {
            e.printStackTrace()
        }
        bitmap.recycle()
        bitmap = null
    }
}
