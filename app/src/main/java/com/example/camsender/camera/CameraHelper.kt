package com.example.camsender.camera

import android.content.Context
import android.util.Log
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraHelper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var orientationEventListener: OrientationEventListener? = null

    private var flashMode: Int = ImageCapture.FLASH_MODE_OFF

    interface OnImageSavedListener {
        fun onImageSaved(file: File)
        fun onError(exception: Exception)
    }

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Flexible resolution strategy to avoid IllegalArgumentException on limited hardware
            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1920, 1440),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()

            preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setResolutionSelector(resolutionSelector)
                .setJpegQuality(80)
                .setFlashMode(flashMode)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider?.unbindAll()
                // Attempt to bind both use cases
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageCapture
                )
                Log.d("CameraHelper", "Binding succeeded for Preview and ImageCapture")
                setupTouchToFocus()
                setupOrientationListener()
            } catch (exc: Exception) {
                Log.e("CameraHelper", "Full binding failed, attempting fallback to Preview only", exc)
                try {
                    cameraProvider?.unbindAll()
                    // Fallback: Bind only Preview so the user can at least see the scene
                    camera = cameraProvider?.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview
                    )
                    Log.d("CameraHelper", "Fallback binding succeeded for Preview only")
                } catch (e: Exception) {
                    Log.e("CameraHelper", "Critical: All binding attempts failed", e)
                }
            }

        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupTouchToFocus() {
        previewView.setOnTouchListener { view, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val factory = previewView.meteringPointFactory
                val point = factory.createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(point).build()
                camera?.cameraControl?.startFocusAndMetering(action)
                view.performClick()
            }
            true
        }
    }

    private fun setupOrientationListener() {
        orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                imageCapture?.targetRotation = rotation
            }
        }
        orientationEventListener?.enable()
    }

    fun toggleFlash(): Int {
        flashMode = when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }
        imageCapture?.flashMode = flashMode
        return flashMode
    }

    fun takePicture(listener: OnImageSavedListener) {
        val imageCapture = imageCapture ?: run {
            listener.onError(IllegalStateException("ImageCapture not bound to camera"))
            return
        }

        val photoFile = File(
            context.cacheDir,
            "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraHelper", "Photo capture failed: ${exc.message}", exc)
                    listener.onError(exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("CameraHelper", "Photo capture succeeded: ${photoFile.absolutePath}")
                    listener.onImageSaved(photoFile)
                }
            }
        )
    }

    fun stopCamera() {
        orientationEventListener?.disable()
        cameraExecutor.shutdown()
    }
}
