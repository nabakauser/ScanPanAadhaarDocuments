package com.example.first.scannerUtils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class CameraManager {

    private var imageCapture: ImageCapture? = null
    private var isCameraInitialised = false

    private lateinit var context: Context
    private lateinit var lifecycleOwner: LifecycleOwner
    private lateinit var previewView: PreviewView


    fun initialiseCameraComponents(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        this.context = context
        this.lifecycleOwner = lifecycleOwner
        this.previewView = previewView
    }

    fun startCamera(isCameraInitialisedState: MutableState<Boolean>) {
        if (isCameraInitialised) return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                val preview = androidx.camera.core.Preview.Builder().build()
                preview.surfaceProvider = previewView.surfaceProvider
                val imageCaptureInstance = ImageCapture.Builder().build()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

//                val imageAnalyzer = ImageAnalysis.Builder()
//                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                    .build()
//                    .also { analysis ->
//                        analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
//                            processImageProxy(imageProxy)
//                        }
//                    }


                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCaptureInstance,
//                    imageAnalyzer
                )

                imageCapture = imageCaptureInstance
                isCameraInitialisedState.value = true
                isCameraInitialised = true
            } catch (exc: Exception) {
                Log.e("cameraLog", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCamera() {
        if (!isCameraInitialised) return

        // Unbind all camera use cases
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        cameraProvider.unbindAll()
        isCameraInitialised = false
    }

    fun captureImage(
        croppedImage: MutableState<Bitmap?>,
        borderColor: MutableState<Color>
//        isAutomatic: Boolean,
    ) {
        imageCapture?.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    Log.d("cameraLog", "Capture successful")

                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                    }

                    val originalBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0,
                        0,
                        image.width,
                        image.height,
                        matrix,
                        true
                    )

                    val canvasWidth = originalBitmap.width.toFloat()
                    val canvasHeight = originalBitmap.height.toFloat()

                    val rectWidth = canvasWidth * 0.7f
                    val rectHeight = canvasHeight * 0.3f
                    val left = ((canvasWidth - rectWidth) / 2).toInt()
                    val top = ((canvasHeight - rectHeight) / 2).toInt()

                    val cropRight = (left + rectWidth).toInt()
                    val cropBottom = (top + rectHeight).toInt()

                    val adjustedLeft = if (left < 0) 0 else left
                    val adjustedTop = if (top < 0) 0 else top
                    val adjustedRight = if (cropRight > originalBitmap.width) originalBitmap.width else cropRight
                    val adjustedBottom = if (cropBottom > originalBitmap.height) originalBitmap.height else cropBottom

                    val croppedBitmap = Bitmap.createBitmap(
                        originalBitmap,
                        adjustedLeft,
                        adjustedTop,
                        adjustedRight - adjustedLeft,
                        adjustedBottom - adjustedTop
                    )

                    val inputImage = InputImage.fromBitmap(croppedBitmap, 0)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                    recognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            val imageResult = detectAnomaly(originalBitmap, context) == "original" && detectAnomaly(croppedBitmap, context) == "original"
                            if(imageResult && handleRecognizedText(visionText)) {
                                Toast.makeText(context, "Original!", Toast.LENGTH_SHORT).show()
                                croppedImage.value = croppedBitmap
                                borderColor.value = Color.Green
                            } else {
                                borderColor.value = Color.Red
                                Toast.makeText(context, "Anomaly!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("cameraLog", "Error recognizing text: ${e.message}")
                        }
                    image.close()
                    borderColor.value = Color.White
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("cameraLog", "Capture failed: $exception")
                }
            }
        )
    }

//    @OptIn(ExperimentalGetImage::class)
   /* private fun processImageProxy(
        imageProxy: ImageProxy,
        onTextDetected: (String) -> Unit = {}
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    for (block in visionText.textBlocks) {
                        val blockText = block.text
                        Log.d("cameraLog", "Detected text: $blockText")
                        if(handleRecognizedText(visionText)) {
//                        if (isPanOrAadhaarCard(blockText)) {
                            Log.d("cameraLog", "PAN/Aadhaar detected: $visionText")
//                            onTextDetected(blockText)
//                            captureImage(mutableStateOf(null))
                            imageProxy.close()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("cameraLog", "Error recognizing text: ${e.message}")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }*/
}