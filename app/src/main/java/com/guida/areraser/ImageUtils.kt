package com.guida.areraser

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
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

    //Gets the x,y coordinates for the world coordinate point
    //Input: the 3D point that needs to be converted
    //Output: the 2D points that lies on the screen
    fun getScreenCoordinate(arFragment: ArFragment, worldPoint: Vector3): FloatArray {
        var screenPoint = arFragment.arSceneView.scene.camera.worldToScreenPoint(worldPoint)
        return floatArrayOf(screenPoint.x, screenPoint.y)
    }

    fun sortLRTB(screenPoints: MutableList<FloatArray>): FloatArray {
        Log.d(TAG, "Unsorted Points: " +
                "(${screenPoints[0][0]}, ${screenPoints[0][1]}), " +
                "(${screenPoints[1][0]}, ${screenPoints[1][1]}), " +
                "(${screenPoints[2][0]}, ${screenPoints[2][1]}), " +
                "(${screenPoints[3][0]}, ${screenPoints[3][1]}), "
        )
        var sortedPoints: MutableList<FloatArray> = mutableListOf(
            screenPoints[0],
            screenPoints[1],
            screenPoints[2],
            screenPoints[3]
        )
        var screenMaxMinPoints: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)
        for(i in 0..3){
            //find least x coordinate
            Log.d(TAG, "Point: (${screenPoints[i][0]}, ${screenPoints[i][1]}")
            if(screenPoints[i][0] <= sortedPoints[0][0] ){
                sortedPoints[0] = screenPoints[i]
                screenMaxMinPoints[0] = screenPoints[i][0]
            }
            //find greatest x coordinate
            if(screenPoints[i][0] >= sortedPoints[1][0] ){
                sortedPoints[1] = screenPoints[i]
                screenMaxMinPoints[1] = screenPoints[i][0]
            }
            //find least y coordinate
            if(screenPoints[i][1] <= sortedPoints[3][1] ){
                sortedPoints[3] = screenPoints[i]
                screenMaxMinPoints[3] = screenPoints[i][1]
            }
            //find greatest y coordinate
            if(screenPoints[i][1] >= sortedPoints[2][1] ){
                sortedPoints[2] = screenPoints[i]
                screenMaxMinPoints[2] = screenPoints[i][1]
            }
        }
        Log.d(TAG, "Sorted Points: ${screenMaxMinPoints[0]}, ${screenMaxMinPoints[1]}, ${screenMaxMinPoints[2]}, ${screenMaxMinPoints[3]}")
        return screenMaxMinPoints
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

    fun getTopBound(arFragment: ArFragment) {

    }
}