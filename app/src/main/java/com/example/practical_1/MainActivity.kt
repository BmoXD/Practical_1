package com.example.practical_1

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var btnTakePhoto: Button
    private lateinit var btnTakeVideo: Button
    private lateinit var cameraExecutor: ExecutorService

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var currentRecording: Recording? = null
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // The Toolbar defined in the layout has the id "my_toolbar".
        setSupportActionBar(findViewById(R.id.my_toolbar))

        viewFinder = findViewById(R.id.viewFinder)
        btnTakePhoto = findViewById(R.id.btn_TakePhoto)
        btnTakeVideo = findViewById(R.id.btn_TakeVideo)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Set up the listeners for take photo and video buttons
        btnTakePhoto.setOnClickListener { takePhoto() }
        btnTakeVideo.setOnClickListener { toggleVideoRecording() }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.toolbar_items, menu)
        return true
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
            }
        )
    }

    private fun toggleVideoRecording() {
        val videoCapture = this.videoCapture ?: return

        if (isRecording) {
            // Stop the current recording session
            val recording = currentRecording
            if (recording != null) {
                recording.stop()
                currentRecording = null
                isRecording = false
                btnTakeVideo.text = "Take Video"
            }
        } else {
            // Start a new recording session
            btnTakeVideo.isEnabled = false

            val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
                }
            }

            val mediaStoreOutputOptions = MediaStoreOutputOptions
                .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues)
                .build()

            currentRecording = videoCapture.output
                .prepareRecording(this, mediaStoreOutputOptions)
                .apply {
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        withAudioEnabled()
                    }
                }
                .start(ContextCompat.getMainExecutor(this), captureListener)

            isRecording = true
            btnTakeVideo.text = "Stop Recording"
            btnTakeVideo.isEnabled = true
        }
    }

    private val captureListener = Consumer<VideoRecordEvent> { event ->
        when (event) {
            is VideoRecordEvent.Start -> {
                btnTakeVideo.isEnabled = true
                Log.d(TAG, "Video recording started")
            }
            is VideoRecordEvent.Finalize -> {
                if (!event.hasError()) {
                    val msg = "Video capture succeeded: " +
                            "${event.outputResults.outputUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                } else {
                    currentRecording?.close()
                    currentRecording = null
                    Log.e(TAG, "Video capture failed: ${event.error}")
                }
                btnTakeVideo.isEnabled = true
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                // Used to bind the lifecycle of cameras to the lifecycle owner
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(viewFinder.surfaceProvider)
                    }

                // Image capture with lower resolution for compatibility
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // Try to use progressive fallbacks for video quality
                val videoCaptureBuild = try {
                    // Start with a more flexible approach for video quality
                    val qualitySelector = QualitySelector.fromOrderedList(
                        listOf(
                            Quality.SD,
                            Quality.LOWEST
                        ),
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.LOWEST)
                    )

                    val recorder = Recorder.Builder()
                        .setQualitySelector(qualitySelector)
                        .build()

                    VideoCapture.withOutput(recorder)
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating video capture: ${e.message}")
                    null
                }

                // Select back camera as a default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // First bind only preview and image capture
                    if (videoCaptureBuild != null) {
                        // If we successfully created video capture, bind all together
                        videoCapture = videoCaptureBuild
                        cameraProvider.bindToLifecycle(
                            this, cameraSelector, preview, imageCapture, videoCapture
                        )
                    } else {
                        // If video capture failed, only bind preview and image capture
                        cameraProvider.bindToLifecycle(
                            this, cameraSelector, preview, imageCapture
                        )
                        // Disable video button
                        btnTakeVideo.isEnabled = false
                        btnTakeVideo.text = "Video not supported"
                        Toast.makeText(this, "Video recording not supported on this device",
                            Toast.LENGTH_LONG).show()
                    }

                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)

                    // Try with only preview as a last resort
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            this, cameraSelector, preview
                        )

                        // Disable buttons for features that didn't work
                        btnTakePhoto.isEnabled = false
                        btnTakeVideo.isEnabled = false
                        Toast.makeText(this, "Limited camera functionality available",
                            Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Camera preview binding failed", e)
                        Toast.makeText(this, "Camera initialization failed completely",
                            Toast.LENGTH_LONG).show()
                    }
                }
            } catch (exc: Exception) {
                Log.e(TAG, "Camera provider failed", exc)
                Toast.makeText(this, "Failed to start camera: ${exc.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}