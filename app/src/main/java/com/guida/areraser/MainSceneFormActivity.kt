package com.guida.areraser

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.view.updateLayoutParams
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Light
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.PlaneRenderer
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.ArFragment
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.Map
import kotlin.experimental.and

class MainSceneFormActivity : AppCompatActivity(), Scene.OnUpdateListener {

    private var TAG = "SCENEFORMACTIVITY"
    private var arFragment: ArFragment? = null
    private var planeManager = PlaneManager()
    private var tfLiteUtils = TFLiteUtils()
    private var removeButton: FloatingActionButton? = null
    private var editSwitch: Switch? = null
    private var cardView: CardView? = null
    private var lightsCheckBox: Switch? = null
    private var estimatedLightsCheckBox: CheckBox? = null
    private var capturePreview: ImageView? = null
    private var previewSwitch: Switch? = null
    private var captureButton: FloatingActionButton? = null
    private var globalBoundingPoints: MutableList<Vector3> = mutableListOf()
    private var session: Session? = null
    private var config: Config? = null
    private var intensity: FloatArray? = null
    private var direction: FloatArray? = null
    var lightmaps: Array<ArImage>? = null


    override// CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }

        val permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        ActivityCompat.requestPermissions(this, permissions,0)

        tfLiteUtils.initialize(this)

        setContentView(R.layout.activity_ux)
        lightsCheckBox = findViewById(R.id.lightsCheckBox)
        //estimatedLightsCheckBox = findViewById(R.id.estimatedLightsCheckbox)
        removeButton = findViewById(R.id.RemoveButton)
        editSwitch = findViewById(R.id.EditSwitch)
        previewSwitch = findViewById(R.id.PreviewSwitch)
        capturePreview = findViewById(R.id.CapturePreview)
        captureButton = findViewById(R.id.CaptureButton)
        cardView = findViewById(R.id.CardView)
        captureButton?.setOnClickListener{
            generateInputPreview()
        }
        removeButton?.setOnClickListener{
            removeSelection()
        }
        editSwitch?.setOnCheckedChangeListener{buttonView, isChecked ->
            toggleEditMode(isChecked)
        }
        previewSwitch?.setOnCheckedChangeListener{buttonView, isChecked ->
            togglePreviewMode(isChecked)
        }
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?
        arFragment!!.arSceneView.scene.addOnUpdateListener(this)
        // Configure the session with the Lighting Estimation API in ENVIRONMENTAL_HDR mode.
        session = Session(this)
        config = Config(session)
        config!!.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        config!!.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE)
        arFragment!!.arSceneView.setupSession(session)
        arFragment!!.arSceneView.session!!.configure(config)
        arFragment!!.arSceneView.scene.sunlight!!.light!!.intensity = 0.0f
        lightsCheckBox?.setOnCheckedChangeListener{buttonView, isChecked ->
            LightingUtils.toggleLights(arFragment!!.arSceneView.scene.sunlight!!, isChecked)
        }
        /*
        estimatedLightsCheckBox?.setOnCheckedChangeListener{buttonView, isChecked ->
            LightingUtils.applyEnvironmentalLightingCubeMap(lightmaps!!, capturePreview!!)
            //Log.d(TAG, "Direction before value: ${arFragment!!.arSceneView.scene.sunlight!!.worldPosition}")

            LightingUtils.setDirectionalLightValues(planeManager.transformableNode?.parent as AnchorNode, intensity!!, direction!!, isChecked, arFragment?.context!!)
            //Log.d(TAG, "Direction after value: ${direction!![0]}, ${direction!![1]}, ${direction!![2]}")

        }

         */
        arFragment!!.setOnTapArPlaneListener {
                hitResult: HitResult,
                plane: Plane,
                motionEvent: MotionEvent ->
                    globalBoundingPoints = planeManager.getCombinedGlobalBoundingPoints(arFragment!!)
                    planeManager.placeAllBoundingNodes(arFragment!!)
                    planeManager.placeBoxOnPlane(
                        arFragment!!,
                        hitResult
                    )
        }
    }

    fun generateInputPreview(){
        maskSelection()
        var screenMaxMinPoints = mutableListOf<FloatArray>(
            ImageUtils.getScreenCoordinate(arFragment!!, globalBoundingPoints[0]),
            ImageUtils.getScreenCoordinate(arFragment!!, globalBoundingPoints[1]),
            ImageUtils.getScreenCoordinate(arFragment!!, globalBoundingPoints[2]),
            ImageUtils.getScreenCoordinate(arFragment!!, globalBoundingPoints[3])
        )
        var sortedPoints = ImageUtils.sortLRTB(screenMaxMinPoints)
        ImageUtils.captureCroppedSceneView(
            arFragment as ArFragment,
            this,
            sortedPoints
        )
    }

    fun removeSelection(){
        setupRealisticHDRLighting()
        planeManager.updateBoundingBoxTexture(arFragment?.context!!, R.drawable.sample_texture, true, 0.0f, 0.5f, 0.4f)
    }

    fun maskSelection(){
        planeManager.updateBoundingBoxTexture(arFragment?.context!!, R.drawable.white_mask, false, 0.0f, 1.0f, 0.0f)

    }

    fun toggleEditMode(isChecked: Boolean){
        arFragment?.arSceneView?.planeRenderer?.isVisible = !isChecked
    }

    fun togglePreviewMode(isChecked: Boolean){
        var layoutParams = cardView?.layoutParams
        Toast.makeText(this, "${layoutParams?.height}", Toast.LENGTH_LONG)
            .show()
        if(!isChecked){
            var height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 375.0f, resources.displayMetrics)
            capturePreview?.imageAlpha = 255
            layoutParams?.height = height.toInt()
        } else {
            var height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 175.0f, resources.displayMetrics)
            capturePreview?.imageAlpha = 0
            layoutParams?.height = height.toInt()
        }
        cardView?.layoutParams = layoutParams
    }

    fun updateCapturePreview(bitmap: Bitmap): Boolean{
        Log.d(TAG, "Updating Preview...")
        capturePreview?.setImageBitmap(bitmap)
        Log.d(TAG, "Updated...")
        return true
    }

    override fun onUpdate(frameTime: FrameTime) {
        //setupRealisticHDRLighting()

        var sampler =
            Texture.Sampler.builder()
                .setMinFilter(Texture.Sampler.MinFilter.LINEAR)
                .setMagFilter(Texture.Sampler.MagFilter.LINEAR)
                .setWrapMode(Texture.Sampler.WrapMode.REPEAT)
                .build()

        Texture.builder()
            .setSource(this, R.drawable.plane_transparent)
            .setSampler(sampler)
            .build()
            .thenAccept { texture ->
                arFragment?.arSceneView?.planeRenderer
                    ?.getMaterial()?.thenAccept{ material ->
                        material.setTexture(PlaneRenderer.MATERIAL_TEXTURE, texture)
                        material.setFloat(PlaneRenderer.MATERIAL_SPOTLIGHT_RADIUS, 100.0f)
                    }
            }
        val bitmap =
            Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
        tfLiteUtils.convertBitmapToByteBuffer(bitmap)
        tfLiteUtils.tflite?.run(tfLiteUtils.imgData, tfLiteUtils.labelProbArray)
        if (!tfLiteUtils.labelList.isEmpty()){
            //Log.d(TAG, "${frameTime.deltaSeconds}: ${tfLiteUtils.labelList[0]}: ${tfLiteUtils.getNormalizedProbability(0)}}")
        }
    }

    fun setupRealisticHDRLighting(){
        var frame: Frame = session!!.update()
        var lightEstimate: LightEstimate = frame.lightEstimate
        intensity = lightEstimate.environmentalHdrMainLightIntensity
        Log.d(TAG, "Intensity value: ${intensity!![0].toString()} of ${intensity!!.size}")
        direction = lightEstimate.environmentalHdrMainLightDirection
        Log.d(TAG, "Direction value: ${direction!![0].toString()} of ${direction!!.size}")
        //var harmonics = lightEstimate.environmentalHdrAmbientSphericalHarmonics
        //Log.d(TAG, "Harmonics value: ${harmonics[0].toString()} of ${harmonics.size}")
        //lightmaps = lightEstimate.acquireEnvironmentalHdrCubeMap()
        LightingUtils.setDirectionalLightValues(planeManager.transformableNode?.parent, intensity!!, direction!!, true, arFragment?.context!!)

    }

    companion object {
        private val MIN_OPENGL_VERSION = 3.0

        fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                Log.e("MainSceneFormActivity", "Sceneform requires Android N or later")
                Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG)
                    .show()
                activity.finish()
                return false
            }
            val openGlVersionString =
                (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                    .deviceConfigurationInfo
                    .glEsVersion
            if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
                Log.e("MainSceneFormActivity", "Sceneform requires OpenGL ES 3.0 later")
                Toast.makeText(
                    activity,
                    "Sceneform requires OpenGL ES 3.0 or later",
                    Toast.LENGTH_LONG
                )
                    .show()
                activity.finish()
                return false
            }
            return true
        }
    }
}




/*
val localLRTB = planeManager.getLocalLTRB(plane)
Log.d(TAG, "GOT LTRB $localLRTB")
val globalBoundingPoints = planeManager.getGlobalBoundingPoints(localLRTB, plane.centerPose)
Log.d(TAG, "GOT GLOBAL BOUNDING POINTS $globalBoundingPoints")
planeManager.placeBoundingPoint(globalBoundingPoints[0], arFragment!!, plane, "Left Most Point")
planeManager.placeBoundingPoint(globalBoundingPoints[1], arFragment!!, plane, "Top Most Point")
planeManager.placeBoundingPoint(globalBoundingPoints[2], arFragment!!, plane, "Right Most Point")
planeManager.placeBoundingPoint(globalBoundingPoints[3], arFragment!!, plane, "Bottom Most Point")
Log.d(TAG, "PLACED BOUNDING POINTS ${arFragment?.arSceneView?.scene?.children}")
*/


/*
  if(focusedPlane != null && false){
      float extentX = focusedPlane.getExtentX();
      float extentZ = focusedPlane.getExtentZ();
      Log.d("FOCUSED_PLANE", "Got focused plane" + extentX + extentZ);
      Vector3 cubeVector = new Vector3(extentX, 0.2f, extentZ);
      focusedPlaneCenterAnchor = focusedPlane.createAnchor(focusedPlane.getCenterPose());
      AnchorNode anchorNode = new AnchorNode(focusedPlaneCenterAnchor);
      anchorNode.setParent(arFragment.getArSceneView().getScene());
      Vector3 xyPose = new Vector3(
              focusedPlane.getCenterPose().getTranslation()[0],
              focusedPlane.getCenterPose().getTranslation()[1],
              focusedPlane.getCenterPose().getTranslation()[2]
      );
      Log.d("FOCUSED_PLANE", "Making plane...");
      MaterialFactory.makeTransparentWithColor(
              getApplicationContext(),
              new Color(244, 244, 244))
              .thenAccept(
                      material -> {
                          ModelRenderable modelRenderable = ShapeFactory.makeCube(
                                  cubeVector,
                                  xyPose,
                                  material);
                          modelRenderable.setShadowCaster(true);
                          modelRenderable.setShadowReceiver(false);
                          focusedPlaneNode = new TransformableNode(arFragment.getTransformationSystem());
                          focusedPlaneNode.setParent(null);
                          Log.d("FOCUSED_PLANE", "Making plane");

                          focusedPlaneNode.setParent(anchorNode);
                          focusedPlaneNode.setWorldRotation(new Quaternion(
                                  focusedPlane.getCenterPose().getRotationQuaternion()[0],
                                  focusedPlane.getCenterPose().getRotationQuaternion()[1],
                                  focusedPlane.getCenterPose().getRotationQuaternion()[2],
                                  focusedPlane.getCenterPose().getRotationQuaternion()[3])
                          );
                          focusedPlaneNode.setName("Test Node");
                          Log.d("FOCUSED_PLANE", "Placed and rotated");
                      }
              );
      Log.d("FOCUSED_PLANE", "After shape made" + focusedPlaneNode.getName());
  } else {
      //Log.d("FOCUSED_PLANE", "Did not get focused plane");
  }
 */