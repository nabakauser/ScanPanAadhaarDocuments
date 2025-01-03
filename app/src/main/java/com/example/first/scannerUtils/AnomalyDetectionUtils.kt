package com.example.first.scannerUtils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.first.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal fun handleRecognizedText(visionText: com.google.mlkit.vision.text.Text): Boolean {
    for (block in visionText.textBlocks) {
        val blockText = block.text
        Log.d("cameraLog", "Detected block text: $blockText")
        if (isPanOrAadhaarCard(blockText)) {
            Log.d("cameraLog", "Detected PAN/Aadhaar card!")
            return true
        }
    }

    return false
}

internal fun isPanOrAadhaarCard(text: String): Boolean {
    val panRegex = Regex("[A-Z]{5}[0-9]{4}[A-Z]")
    val aadhaarRegex = Regex("\\d{12}")
    return (panRegex.matches(text) || text.contains("Permanent Account Number", ignoreCase = true)) || (aadhaarRegex.matches(text) || text.contains("Government of India"))
}

internal fun detectAnomaly(
    image: Bitmap,
    context: Context
): String {
    try {
        val model = Model.newInstance(context)

        val imageSize = 224
        val resizedImage = Bitmap.createScaledBitmap(image, imageSize, imageSize, true)

        val byteBuffer = ByteBuffer.allocateDirect(imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(imageSize * imageSize)
        resizedImage.getPixels(intValues, 0, resizedImage.width, 0, 0, resizedImage.width, resizedImage.height)

        var pixel = 0
        for (i in 0 until imageSize) {
            for (j in 0 until imageSize) {
                val value = intValues[pixel++]
                byteBuffer.put(((value shr 16) and 0xFF).toByte())
                byteBuffer.put(((value shr 8) and 0xFF).toByte())
                byteBuffer.put((value and 0xFF).toByte())
            }
        }

        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.UINT8)
        inputFeature0.loadBuffer(byteBuffer)

        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        val confidences = outputFeature0.floatArray

        var maxPos = 0
        var maxConfidence = 0f
        for (i in confidences.indices) {
            if (confidences[i] > maxConfidence) {
                maxConfidence = confidences[i]
                maxPos = i
            }
        }

        val classes = arrayOf("anomaly", "original")
        val resultLabel = classes[maxPos]

        model.close()

        Log.d("cameraLog","image successfully classified - $resultLabel")
        return resultLabel
    } catch (e: Exception) {
        Log.e("cameraLog", "Error during anomaly detection: ${e.message}", e)
        return "error"
    }
}