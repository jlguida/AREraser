package com.guida.areraser

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.PixelCopy
import android.widget.ImageView
import com.google.ar.sceneform.ux.ArFragment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.sceneform.math.Vector3
import java.lang.Float.min
import java.lang.Integer.min
import java.lang.Integer.max
import java.util.*
import android.util.DisplayMetrics
import androidx.fragment.app.FragmentActivity
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.utils.Converters




object ImageUtils {

    var fullScene: Bitmap? = null
    private var TAG: String = "ImageUtils"

    //Creates a blank bitmap the size of the entire AR scene
    //Input: AR fragment
    //Output: blank bitmap size of AR fragment view
    fun createBlankSceneBitmap(arFragment: ArFragment): Bitmap {
        return Bitmap.createBitmap(arFragment.arSceneView.width, arFragment.arSceneView.height, Bitmap.Config.ARGB_8888)
    }

    fun captureCroppedSceneView(arFragment: ArFragment, updateUIHandler: MainSceneFormActivity, screenMaxMinPoints: FloatArray) {
        var bitmap = createBlankSceneBitmap(arFragment)
        var boundingRect = getCroppedRectangle(arFragment.activity!!, screenMaxMinPoints)
        Log.d(TAG, "Screen Rectangle: ${boundingRect.left}, ${boundingRect.right}, ${boundingRect.top}, ${boundingRect.bottom}")
        //Create handler thread
        var handlerThread = HandlerThread("PixelCopy Thread")
        handlerThread.start()
        //Make a request to copy
        if(!boundingRect.isEmpty) {
            PixelCopy.request(
                arFragment.arSceneView,
                boundingRect,
                bitmap,
                {copyResult  ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        fullScene = bitmap
                        val toast = Toast.makeText(
                            arFragment.context,
                            "Succesfuly copied pixels: $copyResult", Toast.LENGTH_LONG
                        )
                        var tl = Point(boundingRect.left.toDouble(), boundingRect.top.toDouble())
                        var tr = Point(boundingRect.right.toDouble(), boundingRect.top.toDouble())
                        var bl = Point(boundingRect.left.toDouble(), boundingRect.bottom.toDouble())
                        var br = Point(boundingRect.right.toDouble(), boundingRect.bottom.toDouble())
                        updateUIHandler.runOnUiThread(java.lang.Runnable {
                            updateUIHandler.updateCapturePreview(fullScene as Bitmap)
                        })
                        toast.show()
                    } else {
                        val toast = Toast.makeText(
                            arFragment.context,
                            "Failed to copy pixels: $copyResult", Toast.LENGTH_LONG
                        )
                        toast.show()
                    }
                },
                Handler(handlerThread.looper)
            )
        }
    }

    fun captureSceneView(arFragment: ArFragment, updateUIHandler: MainSceneFormActivity) {
        var bitmap = createBlankSceneBitmap(arFragment)
        //Create handler thread
        var handlerThread = HandlerThread("PixelCopy Thread")
        handlerThread.start()
        //Make a request to copy
        PixelCopy.request(
            arFragment.arSceneView,
            bitmap,
            {copyResult  ->
                if (copyResult == PixelCopy.SUCCESS) {
                    fullScene = bitmap
                    val toast = Toast.makeText(
                        arFragment.context,
                        "Succesfuly copied pixels: $copyResult", Toast.LENGTH_LONG
                    )
                    updateUIHandler.runOnUiThread(java.lang.Runnable {
                        updateUIHandler.updateCapturePreview(fullScene as Bitmap)
                    })
                    toast.show()
                } else {
                    val toast = Toast.makeText(
                        arFragment.context,
                        "Failed to copy pixels: $copyResult", Toast.LENGTH_LONG
                    )
                    toast.show()
                }
            },
            Handler(handlerThread.looper)
        )
    }

    fun captureTransformedPlane(arFragment: ArFragment, updateUIHandler: MainSceneFormActivity, cornerPoints: List<Point>) {
        var bitmap = createBlankSceneBitmap(arFragment)
        //Create handler thread
        var handlerThread = HandlerThread("PixelCopy Thread")
        handlerThread.start()
        //Make a request to copy
            PixelCopy.request(
                arFragment.arSceneView,
                bitmap,
                {copyResult  ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        fullScene = bitmap
                        val toast = Toast.makeText(
                            arFragment.context,
                            "Succesfuly copied pixels: $copyResult", Toast.LENGTH_LONG
                        )
                        var skewed_bitmap = transformImage(fullScene as Bitmap, cornerPoints[0], cornerPoints[1], cornerPoints[2], cornerPoints[3])
                        updateUIHandler.runOnUiThread(java.lang.Runnable {
                            updateUIHandler.updateCapturePreview(skewed_bitmap as Bitmap)
                        })
                        toast.show()
                    } else {
                        val toast = Toast.makeText(
                            arFragment.context,
                            "Failed to copy pixels: $copyResult", Toast.LENGTH_LONG
                        )
                        toast.show()
                    }
                },
                Handler(handlerThread.looper)
            )
    }
    //Gets the x,y coordinates for the world coordinate point
    //Input: the 3D point that needs to be converted
    //Output: the 2D points that lies on the screen
    fun getScreenCoordinate(arFragment: ArFragment, worldPoint: Vector3): DoubleArray {
        var screenPoint = arFragment.arSceneView.scene.camera.worldToScreenPoint(worldPoint)
        return doubleArrayOf(screenPoint.x.toDouble(), screenPoint.y.toDouble())
    }

    fun sortLRTB(points: MutableList<Point>): MutableList<Point> {
        var sortedPoints: MutableList<Point> = mutableListOf(
            points[0],
            points[1],
            points[2],
            points[3]
        )
        for(i in 0..3){
            //find least x coordinate
            if(points[i].x <= sortedPoints[0].x ){
                sortedPoints[0] = points[i]
            }
            //find greatest x coordinate
            if(points[i].x >= sortedPoints[1].x ){
                sortedPoints[1] = points[i]
            }
            //find least y coordinate
            if(points[i].y <= sortedPoints[3].y ){
                sortedPoints[3] = points[i]
            }
            //find greatest y coordinate
            if(points[i].y >= sortedPoints[2].y ){
                sortedPoints[2] = points[i]
            }
        }
        return sortedPoints
    }

    fun getCroppedRectangle(context: FragmentActivity, screenMaxMinPoints: FloatArray): Rect{
        val displayMetrics = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val SCREEN_HEIGHT = displayMetrics.heightPixels
        val SCREEN_WIDTH = displayMetrics.widthPixels
        Log.d(TAG, "Width: $SCREEN_WIDTH")
        Log.d(TAG, "Height: $SCREEN_HEIGHT")
        Log.d(TAG, "Screen Points: ${screenMaxMinPoints[0]}, ${screenMaxMinPoints[1]}, ${screenMaxMinPoints[2]}, ${screenMaxMinPoints[3]}")
        var left = max(screenMaxMinPoints[0].toInt() - 25, 0)
        var right = min(screenMaxMinPoints[1].toInt() + 25, SCREEN_WIDTH)
        var bottom = min(screenMaxMinPoints[2].toInt() + 25, SCREEN_HEIGHT)
        var top = max(screenMaxMinPoints[3].toInt() - 25, 0)
        var rect = Rect(left, top, right, bottom)
        Log.d(TAG, "Screen Points: ${rect.left}, ${rect.right}, ${rect.top}, ${rect.bottom}")

        return rect
    }

    fun shapeImageForInput(bitmap: Bitmap): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, 128, 128, true)
    }


    fun transformImage(bitmap: Bitmap, tl: Point, tr: Point, bl:Point, br: Point): Bitmap {
        //var sorted = sortLRTB(mutableListOf(tl, tr, bl, br))
        Log.d(TAG, tl.x.toString() + ", " + tl.y.toString())
        Log.d(TAG, tr.x.toString() + ", " + tr.y.toString())
        Log.d(TAG, bl.x.toString() + ", " + bl.y.toString())
        Log.d(TAG, br.x.toString()  + ", " +  br.y.toString())

        var result_width = (tr.x - tl.x).toInt()
        var bottom_width = (br.x - bl.x).toInt()
        if(bottom_width > result_width){
            result_width = bottom_width
        }

        var result_height = (bl.y - tl.y).toInt()
        var bottom_height = (br.y - tr.y).toInt()
        if(bottom_height > result_height){
            result_height = bottom_height
        }

        Log.d(TAG, result_width.toString())
        Log.d(TAG, result_height.toString())
        var inputMat: Mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(bitmap, inputMat)
        val outputMat = Mat(result_width, result_width, CvType.CV_8UC4)

        val ocvPIn1 = Point(tl.x, tl.y)
        val ocvPIn2 = Point(tr.x, tr.y)
        val ocvPIn3 = Point(bl.x, bl.y)
        val ocvPIn4 = Point(br.x, br.y)
        val source = ArrayList<Point>()
        source.add(ocvPIn1)
        source.add(ocvPIn2)
        source.add(ocvPIn3)
        source.add(ocvPIn4)
        val startM = Converters.vector_Point2f_to_Mat(source)

        val ocvPOut1 = Point(0.0, 0.0)
        val ocvPOut2 = Point(result_width.toDouble(), 0.0)
        val ocvPOut3 = Point(0.0, result_width.toDouble())
        val ocvPOut4 = Point(result_width.toDouble(), result_width.toDouble())
        val dest = ArrayList<Point>()
        dest.add(ocvPOut1)
        dest.add(ocvPOut2)
        dest.add(ocvPOut3)
        dest.add(ocvPOut4)
        val endM = Converters.vector_Point2f_to_Mat(dest)

        val perspectiveTransform = Imgproc.getPerspectiveTransform(startM, endM)

        Imgproc.warpPerspective(
            inputMat,
            outputMat,
            perspectiveTransform,
            Size(result_width.toDouble(), result_width.toDouble())
        )

        val output = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(outputMat, output)
        return output
    }
}