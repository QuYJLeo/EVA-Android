/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mediapipe.examples.facedetection.fragments

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.google.mediapipe.examples.facedetection.FaceDetectorHelper
import com.google.mediapipe.examples.facedetection.MainViewModel
import com.google.mediapipe.examples.facedetection.R
import com.google.mediapipe.examples.facedetection.databinding.FragmentCameraBinding
import com.google.mediapipe.examples.facedetection.utils.CommonUtils
import com.google.mediapipe.examples.facedetection.utils.Constants
import com.google.mediapipe.examples.facedetection.utils.MemoryUtil
import com.google.mediapipe.examples.facedetection.utils.OnnxUtil
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Thread.sleep
import java.text.DecimalFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class CameraFragment : Fragment(), FaceDetectorHelper.DetectorListener {

    private val TAG = "FaceDetection"

    private var _fragmentCameraBinding: FragmentCameraBinding? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var faceDetectorHelper: FaceDetectorHelper
    private val viewModel: MainViewModel by activityViewModels()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    /** Blocking ML operations are performed using this executor */
    private lateinit var backgroundExecutor: ExecutorService

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(
                requireActivity(),
                R.id.fragment_container
            )
                .navigate(CameraFragmentDirections.actionCameraToPermissions())
        }

        backgroundExecutor.execute {
            if (faceDetectorHelper.isClosed()) {
                faceDetectorHelper.setupFaceDetector()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // save FaceDetector settings
        if (this::faceDetectorHelper.isInitialized) {
            viewModel.setDelegate(faceDetectorHelper.currentDelegate)
            viewModel.setThreshold(faceDetectorHelper.threshold)
            // Close the face detector and release resources
            backgroundExecutor.execute { faceDetectorHelper.clearFaceDetector() }
        }

    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor.
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE,
            TimeUnit.NANOSECONDS
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding =
            FragmentCameraBinding.inflate(inflater, container, false)

        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        OnnxUtil.loadModule(requireActivity().assets)
        // Initialize our background executor
        backgroundExecutor = Executors.newSingleThreadExecutor()

        // Create the FaceDetectionHelper that will handle the inference
        backgroundExecutor.execute {
            faceDetectorHelper =
                FaceDetectorHelper(
                    context = requireContext(),
                    threshold = viewModel.currentThreshold,
                    currentDelegate = viewModel.currentDelegate,
                    faceDetectorListener = this,
                    runningMode = RunningMode.LIVE_STREAM
                )

            // Wait for the views to be properly laid out
            fragmentCameraBinding.viewFinder.post {
                // Set up the camera and its use cases
                setUpCamera()
            }
        }

        // Attach listeners to UI control widgets
        initBottomSheetControls()

        LiveEventBus
            .get<String>(Constants.facialExpression, String::class.java)
            .observe(this, object : Observer<String> {
                override fun onChanged(t: String?) {
                    t?.let { fragmentCameraBinding.tvFace.text = it }
                }
            })

        LiveEventBus.get<FloatArray>(Constants.facialExpression2, FloatArray::class.java)
            .observe(this) {
                if (it != null) {
                    fragmentCameraBinding.tvT4.text =
                        Constants.Onnx4.valence.toString() + ":" + "%.2f".format(it[0])
                            .toFloat() + "  " + Constants.Onnx4.arousal + ":" + "%.2f".format(it[1])
                            .toFloat()
                }
            }
    }

    private fun initBottomSheetControls() {
        // Init bottom sheet settings
        fragmentCameraBinding.bottomSheetLayout.thresholdValue.text =
            String.format("%.2f", viewModel.currentThreshold)

        // When clicked, lower detection score threshold floor
        fragmentCameraBinding.bottomSheetLayout.thresholdMinus.setOnClickListener {
            if (faceDetectorHelper.threshold >= 0.1) {
                faceDetectorHelper.threshold -= 0.1f
                updateControlsUi()
            }
        }

        // When clicked, raise detection score threshold floor
        fragmentCameraBinding.bottomSheetLayout.thresholdPlus.setOnClickListener {
            if (faceDetectorHelper.threshold <= 0.8) {
                faceDetectorHelper.threshold += 0.1f
                updateControlsUi()
            }
        }

        // When clicked, change the underlying hardware used for inference. Current options are CPU
        // GPU, and NNAPI
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(
            viewModel.currentDelegate,
            false
        )
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    p2: Int,
                    p3: Long
                ) {
                    faceDetectorHelper.currentDelegate = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }
    }

    // Update the values displayed in the bottom sheet. Reset detector.
    private fun updateControlsUi() {
        fragmentCameraBinding.bottomSheetLayout.thresholdValue.text =
            String.format("%.2f", faceDetectorHelper.threshold)

        backgroundExecutor.execute {
            faceDetectorHelper.clearFaceDetector()
            faceDetectorHelper.setupFaceDetector()
        }

        fragmentCameraBinding.overlay.clear()
    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider =
            cameraProvider
                ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector - makes assumption that we're only using the back camera
        val cameraSelector =
            CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(
                        backgroundExecutor,
                        faceDetectorHelper::detectLivestreamFrame
                    )
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }


        val df = DecimalFormat("#0.00")

        lifecycleScope.launch {
            while (true) {
                var cpu = df.format(CommonUtils.getMemoryUsage(requireContext()).toDouble() * 100)
                    .toDouble()
                Log.i("cpu", "内存占用率0为: " + java.lang.String.valueOf(cpu) + "%")

//                val memoryUsage = df.format(MemoryUtil.getMemoryUsage())
                val memoryUsage1 = df.format(MemoryUtil.getMemoryUsage(requireContext()))
//                Log.i("cpu", "系统级别内存占用率1为:" + java.lang.String.valueOf(memoryUsage) + "%")
                Log.i(
                    "cpu",
                    "应用级别内存占用率2为:" + java.lang.String.valueOf(memoryUsage1) + "%"
                )
                fragmentCameraBinding.tvMemoryApp.text =
                    getString(R.string.app_memory_use) + java.lang.String.valueOf(memoryUsage1) + "%"
//                fragmentCameraBinding.tvMemorySystem.text =
//                    getString(R.string.app_memory_use2) + java.lang.String.valueOf(memoryUsage) + "%"
                delay(2000)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            fragmentCameraBinding.viewFinder.display.rotation
    }

    private var scaleFactor: Float = 1f

    // Update UI after faces have been detected. Extracts original image height/width
    // to scale and place bounding boxes properly through OverlayView
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResults(resultBundle: FaceDetectorHelper.ResultBundle) {

        val detectionResult = resultBundle.results[0]
        activity?.runOnUiThread {
            if (_fragmentCameraBinding != null) {
                fragmentCameraBinding.inferenceTimeLabelCamera.text =
                    getString(R.string.label_inference_time) + ":" + String.format(
                        "%d ms",
                        resultBundle.inferenceTime
                    )
                // Pass necessary information to OverlayView for drawing on the canvas
                if (isAdded) {
                    fragmentCameraBinding.overlay.setResults(
                        detectionResult,
                        resultBundle.inputImageHeight,
                        resultBundle.inputImageWidth
                    )
                }

                // Force a redraw
                fragmentCameraBinding.overlay.invalidate()
            }
        }

        lifecycleScope.launch {
            if (detectionResult.detections().size > 0) {
                scaleFactor = min(
                    _fragmentCameraBinding?.viewFinder?.width!! * 1f / resultBundle.inputImageWidth,
                    _fragmentCameraBinding?.viewFinder?.height!! * 1f / resultBundle.inputImageHeight
                )

                val boundingBox = detectionResult.detections()[0].boundingBox()
                val top = boundingBox.top * scaleFactor
                val bottom = boundingBox.bottom * scaleFactor
                val left = boundingBox.left * scaleFactor
                val right = boundingBox.right * scaleFactor


                getBitmapFromView(
                    left.toInt(),
                    top.toInt(),
                    (right - left).toInt(),
                    (bottom - top).toInt()
                )
            }
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            if (errorCode == FaceDetectorHelper.GPU_ERROR) {
                fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(
                    FaceDetectorHelper.DELEGATE_CPU, false
                )
            }
        }
    }

    private var lastTime = 0L

    @RequiresApi(Build.VERSION_CODES.O)
    fun getBitmapFromView(
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {

        val currentTime = System.currentTimeMillis()

        // 如果当前时间与上次执行方法的时间戳之间的差值大于一秒，则执行该方法
        if (currentTime - lastTime > 300) {
            // 执行方法
            lastTime = currentTime
        } else {
            return
        }

        var bitmap = fragmentCameraBinding.viewFinder.getBitmap() ?: return

        val clampedWidth = max(width, 0)
        val clampedHeight = max(height, 0)

//        val tempWidth = if (x + clampedWidth > bitmap!!.width) bitmap.width else clampedWidth
        val tempWidth = if (x + clampedWidth > bitmap.width) bitmap.width else clampedWidth

        lifecycleScope.launch(Dispatchers.IO) {
//            val bitmap1 = cropBitmap(bitmap!!, x, y, width, clampedHeight)
            var bitmap1 = cropBitmap(bitmap, x, y, tempWidth, clampedHeight)
            OnnxUtil.inferencr2(bitmap1)
            bitmap1?.recycle()
            bitmap1 = null

            bitmap.recycle()
        }
    }

    fun cropBitmap(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): Bitmap? {
        // 确保传递的坐标和尺寸在Bitmap范围内
        if (x < 0 || y < 0 || width <= 0 || height <= 0 || x + width > bitmap.width || y + height > bitmap.height) {
            return null
        }

        // 使用Bitmap.createBitmap()方法进行截取
        return Bitmap.createBitmap(bitmap, x, y, width, height)
    }
}