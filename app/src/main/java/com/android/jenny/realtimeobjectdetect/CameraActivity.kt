package com.android.jenny.realtimeobjectdetect

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_camera.*
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity: AppCompatActivity() {

    companion object {
        private const val TAG = "CameraActivity"
        private const val MODEL_FILE_NAME = "ssd_mobilenet_v1.tflite"
        private const val LABEL_FILE_NAME = "dataset_labels.txt"
        private const val REQ_PERMISSION_CAMERA = 0
    }

    private lateinit var overlaySurfaceView: OverlaySurfaceView
    private lateinit var cameraExecutor: ExecutorService

    private val interpreter: Interpreter by lazy {
        Interpreter(loadModel())
    }

    private val labels: List<String> by lazy {
        loadLabels()
    }

    private val yuvToRgbConverter: YuvToRgbConverter by lazy {
        YuvToRgbConverter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "onCreate()")
        setContentView(R.layout.activity_camera)
        overlaySurfaceView = OverlaySurfaceView(resultView)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQ_PERMISSION_CAMERA)
        } else {
            Log.e(TAG, "Permissions granted!")
            setUpCamera()
        }
    }

    private fun loadModel(fileName: String = MODEL_FILE_NAME): ByteBuffer {
        Log.e(TAG, "loadModel()")
        lateinit var modelBuffer: ByteBuffer
        var file: AssetFileDescriptor? = null
        try {
            file = assets.openFd(fileName)
            val inputStream = FileInputStream(file.fileDescriptor)
            val fileChannel = inputStream.channel
            modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY,
                file.startOffset, file.declaredLength)
        } catch (e: Exception) {
            Log.e(TAG, "loadModel()_try-catch: ${e.message}")
            finish()
        } finally {
            file?.close()
        }
        return modelBuffer
    }

    private fun loadLabels(fileName: String = LABEL_FILE_NAME): List<String> {
        Log.e(TAG, "loadLabels()")
        var labels = listOf<String>()
        var inputStream: InputStream? = null
        try {
            inputStream = assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            labels = reader.readLines()
        } catch (e: Exception) {
            Log.e(TAG, "loadLabels()_try-catch: ${e.message}")
            finish()
        } finally {
            inputStream?.close()
        }
        return labels
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(cameraView.surfaceProvider) }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetRotation(cameraView.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        cameraExecutor,
                        ObjectDetector(
                            yuvToRgbConverter,
                            interpreter,
                            labels,
                            Size(resultView.width, resultView.height)
                        ) { detectedObjectList ->
                            overlaySurfaceView.draw(detectedObjectList)
                        }
                    )
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (e: Exception) {
                Log.e(TAG, "setUpCamera()_try-catch: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQ_PERMISSION_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setUpCamera()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
                this.finish()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

}