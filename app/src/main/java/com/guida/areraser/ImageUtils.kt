package com.guida.areraser

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.HandlerThread
import android.view.PixelCopy
import android.widget.ImageView
import com.google.ar.sceneform.ux.ArFragment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.sceneform.math.Vector3
import java.util.*


object ImageUtils {

    var fullScene: Bitmap? = null
    private var croppedScene: Bitmap? = null
    private var warpedScene: Bitmap? = null

    //Creates a blank bitmap the size of the entire AR scene
    //Input: AR fragment
    //Output: blank bitmap size of AR fragment view
    fun createBlankSceneBitmap(arFragment: ArFragment): Bitmap {
        return Bitmap.createBitmap(arFragment.arSceneView.width, arFragment.arSceneView.height, Bitmap.Config.ARGB_8888)
    }

    fun captureCroppedSceneView(arFragment: ArFragment, updateUIHandler: MainSceneFormActivity, screenMaxMinPoints: FloatArray) {
        var bitmap = createBlankSceneBitmap(arFragment)
        var boundingRect = getCroppedRectangle(screenMaxMinPoints)
        //Create handler thread
        var handlerThread = HandlerThread("PixelCopy Thread")
        handlerThread.start()
        //Make a request to copy
        if(boundingRect != null) {
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

    fun getCroppedRectangle(screenMaxMinPoints: FloatArray): Rect{
        var rect = Rect()
        rect.left = screenMaxMinPoints[0].toInt()
        rect.right = screenMaxMinPoints[1].toInt()
        rect.top = screenMaxMinPoints[2].toInt()
        rect.bottom = screenMaxMinPoints[3].toInt()
        return rect
    }

    fun getTopBound(arFragment: ArFragment) {

    }
}