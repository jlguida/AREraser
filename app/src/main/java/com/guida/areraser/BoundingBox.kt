
package com.guida.areraser
/*
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.MotionEvent
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode

//A class for defining the bounding box that the user places
class BoundingBox {
    //Box dimensions
    val height = 5.0f
    val width = 5.0f
    val depth = 5.0f

    //Box color
    val color = Color(255.0f, 0.0f, 0.0f, 0.7f)

    //Distance from the user the box is placed
    var rayDistance = 30.0f

    //Node to place the box on
    var transformableNode: TransformableNode? = null
    var transformableCast: TransformableNode? = null

    fun placeBoxRayCast(context: Context, xpos: Float, ypos: Float, arFragment: ArFragment, distance: Float): TransformableNode? {
        val touchRay = arFragment.arSceneView.scene.camera.screenPointToRay(xpos, ypos)
        MaterialFactory.makeTransparentWithColor(context, color)
        .thenAccept{ material ->
            val cubeVector = Vector3(width, height, depth)
            val positionVector = touchRay.getPoint(distance)
            val modelRenderable = ShapeFactory.makeCube(
                    cubeVector,
                    positionVector,
                    material)
            modelRenderable.setShadowCaster(true)
            modelRenderable.setShadowReceiver(false)

            transformableCast?.setParent(null)
            transformableCast = TransformableNode(arFragment.getTransformationSystem())
            transformableCast?.setParent(arFragment.getArSceneView().getScene())
            transformableCast?.setRenderable(modelRenderable)
            arFragment.arSceneView.scene.addChild(transformableCast)
            transformableCast?.select()
        }
        return transformableCast
    }

    fun placeBoxOnPlane(arFragment: ArFragment, hitResult: HitResult, motionEvent: MotionEvent): TransformableNode? {
        var returnNode:TransformableNode? = null
        var anchor = hitResult.trackable.createAnchor(
                hitResult.hitPose.compose(Pose.makeTranslation(0.0f,-0.2f,0.0f))
        )
        ViewRenderable
                .builder()
                .setView(arFragment.context, R.layout.bounding_box)
                .build()
                .thenAccept { it ->
                    it.isShadowCaster = false
                    it.isShadowReceiver = false
                    val anchorNode = AnchorNode(anchor)
                    anchorNode.setParent(arFragment.arSceneView.scene)
                    transformableNode?.setParent(null)
                    transformableNode = TransformableNode(arFragment.transformationSystem)
                    transformableNode?.renderable = it
                    transformableNode?.setParent(anchorNode)
                    transformableNode?.select()
                    val cameraPosition = arFragment.arSceneView.scene.camera.worldPosition
                    val direction = Vector3.subtract(cameraPosition, transformableNode?.getWorldPosition())
                    val lookRotation = Quaternion.lookRotation(direction, cameraPosition)
                    val boxPosition = Vector3(cameraPosition.x, cameraPosition.y - 0.5f, cameraPosition.z - 1.0f)
                    transformableNode?.worldRotation = Quaternion(1.0f, 0.0f, 0.0f, 1.0f)
                    Log.d("QUATERNION", lookRotation.toString())
                    //node.worldPosition = boxPosition
                    returnNode = transformableNode
                }
                .exceptionally {
                    val builder = AlertDialog.Builder(arFragment.context)
                    builder.setMessage(it.message).setTitle("${TAG}: Error")
                    builder.create().show()
                    return@exceptionally null
                }
        return returnNode
    }
}*/