package com.example.livestreamingclient

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import uk.co.appoly.arcorelocation.LocationMarker
import uk.co.appoly.arcorelocation.LocationScene
import uk.co.appoly.arcorelocation.rendering.LocationNode
import uk.co.appoly.arcorelocation.rendering.LocationNodeRender
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper
import java.util.concurrent.CompletableFuture


class ARActivity : AppCompatActivity() {

    lateinit var locationScene: LocationScene
    lateinit var andyRenderable: ModelRenderable
    lateinit var popupRenderable: ViewRenderable
    lateinit var arSceneView: ArSceneView

    var hasFinishedLoading = false
    var installRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar)
        arSceneView = findViewById<ArSceneView>(R.id.ar_scene_view);

        val popupLayout =
            ViewRenderable.builder()
                .setView(this, R.layout.layout_popup_ar)
                .build();

        val andy = ModelRenderable.builder()
            .setSource(this, R.raw.andy)
            .build();

        CompletableFuture.allOf(andy, popupLayout)
            .handle { notUsed, throwable ->
                if (throwable != null) {
                    Toast.makeText(
                        this@ARActivity,
                        "Unable to load renderables",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@handle
                }
                try {
                    andyRenderable = andy.get();
                    popupRenderable = popupLayout.get()
                    hasFinishedLoading = true;
                } catch (ex: Exception) {
                    Toast.makeText(
                        this@ARActivity,
                        "Unable to fetch renderables",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        arSceneView
            .getScene()
            .addOnUpdateListener { frameTime ->
                if (!hasFinishedLoading) {
                    Toast.makeText(
                        this@ARActivity,
                        "Not Loaded",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnUpdateListener
                }
                if (!::locationScene.isInitialized) {
                    locationScene = LocationScene(this, arSceneView)
//                    val layoutLocationMarker = LocationMarker(0.0, 0.0, getPopupView())
//                    layoutLocationMarker.setRenderEvent {
//                        LocationNodeRender {
//                            fun render(node: LocationNode) {
//                                val distanceTextView = popupRenderable.view.findViewById<TextView>(R.id.textView)
//                                distanceTextView.setText(node.distance.toString() + "M")
//                            }
//                        }
//                    }
//                    locationScene.mLocationMarkers.add(layoutLocationMarker)
                    locationScene.mLocationMarkers.add(LocationMarker(0.0, 0.0, getAndy()))
                }
                val frame = arSceneView.getArFrame();
                if (frame != null && frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
                    if (locationScene != null) {
                        locationScene.processFrame(frame);
                    }

//                    if (loadingMessageSnackbar != null) {
//                        for (Plane plane : frame.getUpdatedTrackables(Plane.class)) {
//                            if (plane.getTrackingState() == TrackingState.TRACKING) {
//                                hideLoadingMessage();
//                            }
//                        }
//                    }
                }
            }

        ARLocationPermissionHelper.requestPermission(this)

    }

    fun getAndy(): Node {
        val base = Node();
        base.setRenderable(andyRenderable);
        val c: Context = this;
        base.setOnTapListener { hitTestResult, motionEvent ->
            Toast.makeText(
                c, "Andy touched.", Toast.LENGTH_SHORT)
                .show();
        }
        return base
    }

    fun getPopupView(): Node {
        val base = Node()
        base.renderable = popupRenderable
        val c: Context = this
        val eView: View = popupRenderable.getView()
        eView.setOnTouchListener { v: View?, event: MotionEvent? ->
            Toast.makeText(
                c, "Location marker touched.", Toast.LENGTH_LONG
            )
                .show()
            false
        }
        return base
    }

    @Throws(UnavailableException::class)
    fun createArSession(activity: Activity?): Session? {
        var session: Session? = null
        // if we have the camera permission, create the session
        if (ARLocationPermissionHelper.hasPermission(activity)) {
            when (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                InstallStatus.INSTALL_REQUESTED -> return null
                InstallStatus.INSTALLED -> {
                }
            }
            session = Session(activity)
            val config = Config(session)
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE)
            session.configure(config)
        }
        return session
    }

    override fun onResume() {
        super.onResume()
        if (::locationScene.isInitialized) {
            locationScene.resume()
        }
        if (arSceneView.session == null) {
            try {
                val session: Session? = this.createArSession(this)
                if (session == null) {
                    installRequested = ARLocationPermissionHelper.hasPermission(this)
                    return
                } else {
                    arSceneView.setupSession(session)
                }
            } catch (e: UnavailableException) {
                Toast.makeText(this, "Not available", Toast.LENGTH_SHORT)
            }
        }
        try {
            arSceneView.resume()
        } catch (ex: CameraNotAvailableException) {
            Toast.makeText(this, "Cannot get camera", Toast.LENGTH_SHORT)
            finish()
            return
        }
        if (arSceneView.session != null) {
            Toast.makeText(this, "Loading", Toast.LENGTH_SHORT)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::locationScene.isInitialized) {
            locationScene.pause()
        }
        arSceneView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        arSceneView.destroy()
    }

}