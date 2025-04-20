import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ops.ResizeOp
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

object PuzzleClassifier {
    private const val TAG = "PuzzleClassifier"
    private lateinit var interpreter: Interpreter
    private const val INPUT_SIZE = 224 // Typical size for many models
    private const val CHANNELS = 3 // RGB channels

    fun initialize(context: Context) {
        interpreter = loadModel(context)
    }

    private fun loadModel(context: Context): Interpreter {
        val assetFileDescriptor = context.assets.openFd("puzzle_classifier.tflite")
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor).apply {
            channel.position(assetFileDescriptor.startOffset)
        }
        val modelBuffer = inputStream.channel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )
        return Interpreter(modelBuffer)
    }

    fun classify(bitmap: Bitmap): Pair<String, Float> {
        // Create image processor
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f)) // Normalize to [0,1] or [-1,1] depending on your model
            .build()

        // Convert bitmap to TensorImage
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)

        // Process image
        val processedImage = imageProcessor.process(tensorImage)
        Log.d(TAG, "Processed image buffer size: ${processedImage.buffer.capacity()} bytes")

        // Verify input tensor shape
        val inputTensor = interpreter.getInputTensor(0)
        Log.d(TAG, "Model expects input shape: ${inputTensor.shape().contentToString()}")

        // Run inference
        val outputBuffer = TensorBuffer.createFixedSize(
            interpreter.getOutputTensor(0).shape(),
            DataType.FLOAT32
        )

        interpreter.run(processedImage.buffer, outputBuffer.buffer)

        // Process results
        val outputArray = outputBuffer.floatArray
        val labels = listOf("sudoku", "nonogram", "kakuro")
        val maxIndex = outputArray.indices.maxByOrNull { outputArray[it] } ?: 0
        val confidence = outputArray[maxIndex]

        return if (confidence > 0.8f) {
            labels[maxIndex] to confidence
        } else {
            "unknown" to confidence
        }
    }
}