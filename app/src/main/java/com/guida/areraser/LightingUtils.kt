package com.guida.areraser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF.length
import android.media.Image
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import androidx.palette.graphics.Palette
import com.google.ar.core.ArImage
import com.google.ar.core.Pose
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Light
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import java.nio.ByteBuffer
import kotlin.math.max


object LightingUtils {
    private val TAG = "LightingUtils"
    private var newSun = Node()

    fun setDirectionalLightValues(
        referenceNode: Node?,
        intensity: FloatArray,
        direction: FloatArray,
        isChecked: Boolean,
        context: Context
    ){
        var normalIntensities = normalizeIntensity(intensity)
        var ambientColor = Color(normalIntensities[0], normalIntensities[1], normalIntensities[2])
        //Log.d(TAG, "Ambient color: ${ambientColor.r}, ${ambientColor.g}, ${ambientColor.b}")
        var light = Light.builder(Light.Type.DIRECTIONAL)
            //.setIntensity(420.0f) //the default for directional lights of indoor spaces
            .setShadowCastingEnabled(true)
            //.setColor(ambientColor)
            .build()
        newSun.setParent(null)
        newSun = Node()
        newSun.setParent(referenceNode)
        var newPosition: Pose? = null
        if(referenceNode is AnchorNode){
            Log.d(TAG, "Anchor Node found")
            newPosition = referenceNode.anchor!!.pose.compose(Pose.makeTranslation(direction[0], direction[1], direction[2]))
            newSun.worldPosition = Vector3(
                newPosition.tx(),
                newPosition.ty(),
                newPosition.tz()
            )
        }

        newSun.setLookDirection(Vector3(
            direction[0],
            direction[1],
            direction[2]
        ))

        MaterialFactory.makeOpaqueWithColor(
            context,
            ambientColor
        ).thenAccept { material ->
            val modelRenderable = ShapeFactory.makeCube(
                Vector3(0.03f, 0.03f, 0.15f),
                Vector3(0.0f, 0.0f, 0.0f),
                material
            )
            modelRenderable.isShadowCaster = false
            modelRenderable.isShadowReceiver = false
            newSun.renderable = modelRenderable
            newSun.light = light
            newSun.isEnabled = true
            //Log.d(TAG, "${direction[0]}, ${direction[1]}, ${direction[2]}")
            //Log.d(TAG, "${intensity[0]}, ${intensity[1]}, ${intensity[2]}")
        }



    }

    fun toggleLights(lightnode: Node, setting: Boolean){
        newSun.isEnabled = setting
    }

    //future work
    fun setAmbientSphericalHarmonicsLightValues(lightnode: Node, harmonics: FloatArray){}

    //future work
    fun applyEnvironmentalLightingCubeMap(images: Array<ArImage>, imageView: ImageView){}

    //Normalize intensity values
    //Can be used as a generous estimation of the ambient light color
    private fun normalizeIntensity(intensity: FloatArray): FloatArray{
        var max = max(max(intensity[0], intensity[1]), intensity[2])
        return floatArrayOf(intensity[0]/max, intensity[1]/max, intensity[2]/max)
    }

    //future work Averages the (normalized) intensity values and sets as a value between 0 and 120000 (no light and full sunlight)
    private fun interpolatedIntensity(intensity: FloatArray): Float{
        return (intensity[0] + intensity [1] + intensity[2])/3.0f * 120000.0f
    }
}