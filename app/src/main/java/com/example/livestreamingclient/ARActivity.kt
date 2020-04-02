package com.example.livestreamingclient

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.helpers.TapHelper
import com.example.rendering.BackgroundRenderer
import com.example.rendering.ObjectRenderer
import com.example.rendering.PlaneRenderer
import com.example.rendering.PointCloudRenderer
import com.google.ar.core.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class ARActivity : AppCompatActivity(), GLSurfaceView.Renderer {

    lateinit var glSurfaceView: GLSurfaceView
    lateinit var session: Session
    lateinit var tapHelper: TapHelper

    val backgroundRenderer = BackgroundRenderer()
    val planeRenderer = PlaneRenderer()
    val pointCloudRenderer = PointCloudRenderer()
    val virtualObject = ObjectRenderer()
    val virtualObjectShadow = ObjectRenderer()

    val anchors = ArrayList<Anchor>()
    val anchorMatrix = FloatArray(16)

    var installRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }

        setContentView(R.layout.activity_ar)
        glSurfaceView = findViewById(R.id.pub2_surface)
        glSurfaceView.preserveEGLContextOnPause = true
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        glSurfaceView.setRenderer(this)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        glSurfaceView.setWillNotDraw(false)

        tapHelper = TapHelper(this)
        glSurfaceView.setOnTouchListener(tapHelper)

        installRequested = false

    }

    override fun onResume() {
        super.onResume()
        if (!::session.isInitialized || session == null) {
            when(ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    installRequested = true
                    return@onResume
                }
            }
            session = Session(this)
        }
        session.resume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (::session.isInitialized) {
            glSurfaceView.onPause()
            session.pause()
        }
    }

    fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Toast.makeText(this, "Android N is required", Toast.LENGTH_SHORT).show()
            activity.finish()
        }

        val activityManager: ActivityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.getDeviceConfigurationInfo()
        if (configurationInfo.reqGlEsVersion < 0x30000) {
            Toast.makeText(this, "OpenGL ES 3.0 is required", Toast.LENGTH_SHORT).show()
            activity.finish()
        }
        return true
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height);
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        try {
            backgroundRenderer.createOnGlThread(this)
            planeRenderer.createOnGlThread(this, "models/trigrid.png")
            pointCloudRenderer.createOnGlThread(this)
            virtualObject.createOnGlThread(this, "models/andy.obj", "models/andy.png")
            virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f)
            virtualObjectShadow.createOnGlThread(
                this, "models/andy_shadow.obj", "models/andy_shadow.png"
            )
            virtualObjectShadow.setBlendMode(ObjectRenderer.BlendMode.Shadow)
            virtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f)
        } catch (e: Exception) {
            println("Failed to read an asset file $e")
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (!::session.isInitialized) {
            return
        }

        try {
            session.setCameraTextureName(backgroundRenderer.textureId);

            val frame = session.update()
            val camera = frame.camera

            backgroundRenderer.draw(frame)

            val tap = tapHelper.poll()
            if (tap != null && camera.trackingState == TrackingState.TRACKING) {
                for (hit in frame.hitTest(tap)) {
                    val trackable = hit.trackable
                    if (trackable is Plane && (trackable as Plane).isPoseInPolygon(hit.hitPose) && (PlaneRenderer.calculateDistanceToPlane(hit.hitPose, camera.pose) > 0)
                        || (trackable is Point && (trackable as Point).orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)) {
                        anchors.add(hit.createAnchor())
                        break
                    }
                }
            }

//            if (camera.trackingState == TrackingState.PAUSED) {
//                return
//            }
//
//            val projmtx = FloatArray(16)
//            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f)
//
//            val viewmtx = FloatArray(16)
//            camera.getViewMatrix(viewmtx, 0)
//
//            val colorCorrectionRgba = FloatArray(4)
//            frame.lightEstimate.getColorCorrection(colorCorrectionRgba, 0)
//
//            val pointCloud = frame.acquirePointCloud()
//            pointCloudRenderer.update(pointCloud)
//            pointCloudRenderer.draw(viewmtx, projmtx)
//
//            pointCloud.release()
//
//            planeRenderer.drawPlanes(
//                session.getAllTrackables(Plane::class.java),
//                camera.displayOrientedPose,
//                projmtx
//            )
//
//            // Visualize anchors
//            val scaleFactor = 1.0f
//            for (anchor in anchors) {
//                if (anchor.trackingState != TrackingState.TRACKING) {
//                    continue
//                }
//                anchor.pose.toMatrix(anchorMatrix, 0)
//                virtualObject.updateModelMatrix(anchorMatrix, scaleFactor)
//                virtualObjectShadow.updateModelMatrix(anchorMatrix, scaleFactor)
//                virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba)
//                virtualObjectShadow.draw(viewmtx, projmtx, colorCorrectionRgba)
//            }

        } catch (e: Exception) {
            println("Draw Frame Error: $e")
        }

    }

}