package com.example.livestreamingclient.Activity

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.livestreamingclient.R
import com.example.livestreamingclient.controller.PopupController
import com.example.livestreamingclient.fragment.UpARFragment
import com.example.livestreamingclient.utils.RTMPStreamerAR
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Session
import kotlinx.android.synthetic.main.activity_ar.*
import net.ossrs.rtmp.ConnectCheckerRtmp
import java.util.*


class ARActivity : AppCompatActivity(), ConnectCheckerRtmp {

    lateinit var arFragment: UpARFragment
    lateinit var rtmpStreamer: RTMPStreamerAR
    lateinit var mPopupController: PopupController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }

        val session = Session(this)
        val filter = CameraConfigFilter(session)
        filter.setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30))


        setContentView(R.layout.activity_ar)
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as UpARFragment
        arFragment.planeDiscoveryController.let {
            it.hide()
            it.setInstructionView(null)
        }
        arFragment.arSceneView.planeRenderer.isEnabled = false
        arFragment.setOnSessionInitializationListener {
            it.config.focusMode = Config.FocusMode.AUTO
        }

        arFragment.arSceneView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                println("Touch: ${event.x}, ${event.y}")
                arFragment.addNodeByXY(event.x, event.y)?.let {
                    layout.addView(it.btn)
                    it.btn.setOnClickListener {
                        mPopupController.showPopupWindow()
                    }
                } ?: runOnUiThread {
                    Toast.makeText(this, "Cannot find surface", Toast.LENGTH_SHORT).show()
                }
            }
            true
        }

        rtmpStreamer = RTMPStreamerAR(
            this,
            this,
            arFragment.arSceneView
        )
        startstream.setOnClickListener {
            if (rtmpStreamer.prepareVideo() && rtmpStreamer.prepareAudio()) {
                rtmpStreamer.startStream("rtmp://106.14.221.157:8935/live/jerry")
            } else {
                Toast.makeText(this, "Failed to prepare stream", Toast.LENGTH_SHORT).show()
            }
        }

        mPopupController =
            PopupController(this)
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

    override fun onAuthSuccessRtmp() {
        runOnUiThread {
            Toast.makeText(this@ARActivity, "Auth Success", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNewBitrateRtmp(bitrate: Long) {}

    override fun onConnectionSuccessRtmp() {
        runOnUiThread {
            Toast.makeText(this@ARActivity, "Connect Success", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnectionFailedRtmp(reason: String) {
        runOnUiThread {
            Toast.makeText(this@ARActivity, "Connect Failed $reason", Toast.LENGTH_SHORT).show()
            rtmpStreamer.stopStream()
        }
    }

    override fun onAuthErrorRtmp() {
        runOnUiThread {
            Toast.makeText(this@ARActivity, "Auth Error", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDisconnectRtmp() {
        runOnUiThread {
            Toast.makeText(this@ARActivity, "Stream Disconnected", Toast.LENGTH_SHORT).show()
        }
    }

}