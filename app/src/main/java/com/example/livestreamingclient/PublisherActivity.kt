package com.example.livestreamingclient

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pedro.rtplibrary.rtmp.RtmpCamera2
import kotlinx.android.synthetic.main.activity_publisher.*
import net.ossrs.rtmp.ConnectCheckerRtmp

class PublisherActivity : AppCompatActivity(), ConnectCheckerRtmp, SurfaceHolder.Callback {
    lateinit var rtmpCamera2: RtmpCamera2

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        runOnUiThread{
            Toast.makeText(this@PublisherActivity, "Change", Toast.LENGTH_LONG).show()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        runOnUiThread{
            Toast.makeText(this@PublisherActivity, "Destory", Toast.LENGTH_LONG).show()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        runOnUiThread{
            Toast.makeText(this@PublisherActivity, "Created", Toast.LENGTH_LONG).show()
        }

        if (rtmpCamera2.prepareAudio() && rtmpCamera2.prepareVideo()) {
            rtmpCamera2.startPreview()
        } else {
            Toast.makeText(this, "Wrong", Toast.LENGTH_LONG).show()
        }
    }

    override fun onAuthSuccessRtmp() {
        runOnUiThread {
            Toast.makeText(this@PublisherActivity, "Auth", Toast.LENGTH_LONG).show()
        }
    }

    override fun onNewBitrateRtmp(bitrate: Long) {
        runOnUiThread {
            Toast.makeText(this@PublisherActivity, "Bitrate", Toast.LENGTH_LONG).show()
        }
    }

    override fun onConnectionSuccessRtmp() {
        runOnUiThread {
            Toast.makeText(this@PublisherActivity, "Success", Toast.LENGTH_LONG).show()
        }
    }

    override fun onConnectionFailedRtmp(reason: String) {
        runOnUiThread {
            Toast.makeText(this@PublisherActivity, "Fail", Toast.LENGTH_LONG).show()
        }
    }

    override fun onAuthErrorRtmp() {
        runOnUiThread {
            Toast.makeText(this@PublisherActivity, "Error", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDisconnectRtmp() {
        runOnUiThread {
            Toast.makeText(this@PublisherActivity, "Disconnect", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publisher)

        rtmpCamera2 = RtmpCamera2(pub_surface, this)
        pub_surface.holder.addCallback(this)

        streamer.setOnClickListener {
            if (rtmpCamera2.isStreaming) {
                rtmpCamera2.stopStream()
            }
            if (rtmpCamera2.prepareVideo() && rtmpCamera2.prepareAudio()) {
                rtmpCamera2.startStream(addr_input.text.toString())
            }
        }

        switcher.setOnClickListener {
            rtmpCamera2.switchCamera()
        }
    }
}