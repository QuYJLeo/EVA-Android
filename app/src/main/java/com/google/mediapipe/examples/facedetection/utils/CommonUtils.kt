package com.google.mediapipe.examples.facedetection.utils

import android.app.ActivityManager
import android.content.Context


/**
 * Created by 13263 on 2024/4/12
 *
 */
object CommonUtils {
    fun getMemoryUsage(context: Context): Long {
        var activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        var memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem
    }
}
