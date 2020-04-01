package com.example.livestreamingclient

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable

class ARActivity : AppCompatActivity() {

    lateinit var arFragment: UpdArFragment
    lateinit var andyRenderable: Renderable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }

        setContentView(R.layout.activity_ar)
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as UpdArFragment

        ModelRenderable.builder()
            .setSource(this, R.raw.andy)
            .build()
            .thenAccept {
                andyRenderable = it
            }
            .exceptionally {
                Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_SHORT).show()
                null
            }

        arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            if (!::andyRenderable.isLateinit) {
                return@setOnTapArPlaneListener
            }
//            arFragment.addNodeByHit(hitResult, andyRenderable)
            arFragment.addNodeByXY(0.5, 0.5, andyRenderable)
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

}