package com.example.livestreamingclient

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.shuyu.gsyvideoplayer.GSYBaseActivityDetail
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.utils.OrientationUtils
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import kotlinx.android.synthetic.main.activity_receiver.*

class ReceiverActivity : AppCompatActivity() {
    lateinit var videoPlayer: StandardGSYVideoPlayer
    lateinit var orientationUtils: OrientationUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receiver)

        videoPlayer = rec_surface
        rec_stream_btn.setOnClickListener {
            videoPlayer.setUp("rtmp://106.14.221.157:8935/live/jerry", true, "测试视频")
            videoPlayer.startPlayLogic()
        }

        videoPlayer.titleTextView.visibility = View.VISIBLE

        videoPlayer.backButton.visibility = View.VISIBLE

        orientationUtils = OrientationUtils(this, videoPlayer)

        videoPlayer.fullscreenButton.setOnClickListener {
            orientationUtils.resolveByClick()
        }

        videoPlayer.setIsTouchWiget(true)

        videoPlayer.backButton.setOnClickListener {
            onBackPressed()
        }

    }

    override fun onPause() {
        super.onPause()
        videoPlayer.onVideoPause()
    }

    override fun onResume() {
        super.onResume()
        videoPlayer.onVideoResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        GSYVideoManager.releaseAllVideos()
        orientationUtils.releaseListener()
    }

    override fun onBackPressed() {
        if (orientationUtils.screenType == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            videoPlayer.fullscreenButton.performClick()
            return
        }
        videoPlayer.setVideoAllCallBack(null)
        super.onBackPressed()
    }
}