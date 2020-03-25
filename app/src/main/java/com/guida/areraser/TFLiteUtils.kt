package com.guida.areraser

import android.app.Activity
import android.graphics.Bitmap
import android.os.SystemClock
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.Comparator
import kotlin.experimental.and

class TFLiteUtils {
    private val TAG = "TFLiteUtils"
    /** Dimensions of inputs.  */
    private val DIM_BATCH_SIZE = 1
    var labelProbArray: Array<ByteArray>? = null
    private val DIM_PIXEL_SIZE = 3
    var tflite: Interpreter? = null

    //member variables for quantized recognition net
    var labelList: List<String> = listOf()

    var imgData: ByteBuffer = ByteBuffer.allocateDirect(
        DIM_BATCH_SIZE
                * 224
                * 224
                * DIM_PIXEL_SIZE
                * 1
    )
    fun initialize(activity: Activity){

        tflite = Interpreter(loadModelFile(activity))
        labelList = loadLabelList(activity)
        labelProbArray = Array(1) { ByteArray(1001) }
        imgData = ByteBuffer.allocateDirect(
            DIM_BATCH_SIZE
                    * 224
                    * 224
                    * DIM_PIXEL_SIZE
                    * 1
        )
        imgData?.order(ByteOrder.nativeOrder())
    }

    /** Memory-map the model file in Assets. */
    private fun loadModelFile(activity: Activity): MappedByteBuffer {
        var fileDescriptor = activity.getAssets().openFd("mobilenet_quant_v1_224.tflite")
        var inputStream = FileInputStream(fileDescriptor.getFileDescriptor())
        var fileChannel = inputStream.getChannel()
        var startOffset = fileDescriptor.getStartOffset()
        var declaredLength = fileDescriptor.getDeclaredLength()
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /** Writes Image data into a `ByteBuffer`.  */
    fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        imgData.rewind()
        var intValues = IntArray(224 * 224)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        // Convert the image to floating point.
        var pixel = 0
        val startTime = SystemClock.uptimeMillis()
        for (i in 0 until 224) {
            for (j in 0 until 224) {
                val `val` = intValues[pixel++]
                addPixelValue(`val`)
            }
        }
        val endTime = SystemClock.uptimeMillis()
        Log.d(
            TAG,
            "Timecost to put values into ByteBuffer: " + java.lang.Long.toString(endTime - startTime)
        )
    }

    protected fun addPixelValue(pixelValue: Int) {
        imgData?.put((pixelValue shr 16 and 0xFF).toByte())
        imgData?.put((pixelValue shr 8 and 0xFF).toByte())
        imgData?.put((pixelValue and 0xFF).toByte())
    }
    fun getNormalizedProbability(labelIndex: Int): Float {
        return (labelProbArray!![0][labelIndex] and 0xff.toByte()) / 255.0f
    }
    /** Reads label list from Assets.  */
    @Throws(IOException::class)
    private fun loadLabelList(activity: Activity): List<String> {
        val labelList = ArrayList<String>()
        val reader = BufferedReader(InputStreamReader(activity.assets.open("labels_mobilenet_quant_v1_224.txt")))
        var line: String
        while (reader.readLine() != null) {
            labelList.add(reader.readLine())

        }
        reader.close()
        return labelList
    }
}

/*
    val bitmap =
        Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
    convertBitmapToByteBuffer(bitmap)
    tflite.run(imgData, labelProbArray)

 */