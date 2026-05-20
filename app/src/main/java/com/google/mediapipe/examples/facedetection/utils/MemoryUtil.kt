package com.google.mediapipe.examples.facedetection.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Debug

object MemoryUtil {
    fun getMemoryUsage(context: Context): Double {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        // 获取可用内存和总内存大小
        val totalMemory = memoryInfo.totalMem
        val availableMemory = memoryInfo.availMem

        // 计算内存使用率
        val memoryUsage = (totalMemory - availableMemory) / totalMemory.toDouble() * 100

        return memoryUsage
    }

    fun getMemoryUsage(): Double {
        // 获取当前应用程序的内存使用情况
        val memoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memoryInfo)

        // 获取当前应用程序使用的内存大小
        val memoryUsage = memoryInfo.totalPrivateDirty * 1024L // 单位转换为字节

        // 获取当前应用程序的最大内存限制
        val maxMemory = Runtime.getRuntime().maxMemory()

        // 计算内存使用率
        val memoryUsagePercent = memoryUsage.toDouble() / maxMemory * 100

        return memoryUsagePercent
    }
}
