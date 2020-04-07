package com.example.livestreamingclient.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.ar.sceneform.ArSceneView
import com.pedro.encoder.Frame
import com.pedro.encoder.audio.AudioEncoder
import com.pedro.encoder.audio.GetAacData
import com.pedro.encoder.input.audio.GetMicrophoneData
import com.pedro.encoder.input.audio.MicrophoneManager
import com.pedro.encoder.utils.CodecUtil.Force
import com.pedro.encoder.video.FormatVideoEncoder
import com.pedro.encoder.video.GetVideoData
import com.pedro.encoder.video.VideoEncoder
import com.pedro.rtplibrary.util.FpsListener
import com.pedro.rtplibrary.util.RecordController
import com.pedro.rtplibrary.view.GlInterface
import com.pedro.rtplibrary.view.OffScreenGlThread
import net.ossrs.rtmp.ConnectCheckerRtmp
import net.ossrs.rtmp.SrsFlvMuxer
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Wrapper to stream a MP4 file with H264 video codec. Only Video is streamed, no Audio.
 * Can be executed in background.
 *
 * API requirements:
 * API 18+.
 *
 * Created by pedro on 7/07/17.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
class RTMPStreamerAR : GetVideoData, GetAacData, GetMicrophoneData {

    lateinit var arSceneView: ArSceneView

    private var context: Context? = null
    protected var videoEncoder: VideoEncoder? = null
    private var audioEncoder: AudioEncoder? = null
    private var microphoneManager: MicrophoneManager? = null
    private var glInterface: GlInterface? = null
    private var recordController: RecordController? = null
    private val fpsListener = FpsListener()
    private var srsFlvMuxer: SrsFlvMuxer

    var isStreaming = false

    constructor(
        connectChecker: ConnectCheckerRtmp?, arSceneView: ArSceneView) {
        this.arSceneView = arSceneView
        init()
        srsFlvMuxer = SrsFlvMuxer(connectChecker)
    }

    constructor(
        context: Context?, connectChecker: ConnectCheckerRtmp?, arSceneView: ArSceneView) {
        this.context = context
        this.arSceneView = arSceneView
        glInterface = OffScreenGlThread(context)
        glInterface!!.init()
        init()
        srsFlvMuxer = SrsFlvMuxer(connectChecker)
    }

    /**
     * H264 profile.
     *
     * @param profileIop Could be ProfileIop.BASELINE or ProfileIop.CONSTRAINED
     */
    fun setProfileIop(profileIop: Byte) {
        srsFlvMuxer.setProfileIop(profileIop)
    }

    @Throws(RuntimeException::class)
    fun resizeCache(newSize: Int) {
        srsFlvMuxer.resizeFlvTagCache(newSize)
    }

    val cacheSize: Int
        get() = srsFlvMuxer.flvTagCacheSize

    val sentAudioFrames: Long
        get() = srsFlvMuxer.sentAudioFrames

    val sentVideoFrames: Long
        get() = srsFlvMuxer.sentVideoFrames

    val droppedAudioFrames: Long
        get() = srsFlvMuxer.droppedAudioFrames

    val droppedVideoFrames: Long
        get() = srsFlvMuxer.droppedVideoFrames

    fun resetSentAudioFrames() {
        srsFlvMuxer.resetSentAudioFrames()
    }

    fun resetSentVideoFrames() {
        srsFlvMuxer.resetSentVideoFrames()
    }

    fun resetDroppedAudioFrames() {
        srsFlvMuxer.resetDroppedAudioFrames()
    }

    fun resetDroppedVideoFrames() {
        srsFlvMuxer.resetDroppedVideoFrames()
    }

    fun setAuthorization(user: String?, password: String?) {
        srsFlvMuxer.setAuthorization(user, password)
    }

    protected fun prepareAudioRtp(isStereo: Boolean, sampleRate: Int) {
        srsFlvMuxer.setIsStereo(isStereo)
        srsFlvMuxer.setSampleRate(sampleRate)
    }

    protected fun startStreamRtp(url: String?) {
        if (videoEncoder!!.rotation == 90 || videoEncoder!!.rotation == 270) {
            srsFlvMuxer.setVideoResolution(videoEncoder!!.height, videoEncoder!!.width)
        } else {
            srsFlvMuxer.setVideoResolution(videoEncoder!!.width, videoEncoder!!.height)
        }
        srsFlvMuxer.start(url)
    }

    protected fun stopStreamRtp() {
        srsFlvMuxer.stop()
    }

    fun setReTries(reTries: Int) {
        srsFlvMuxer.setReTries(reTries)
    }

    fun shouldRetry(reason: String?): Boolean {
        return srsFlvMuxer.shouldRetry(reason)
    }

    fun reConnect(delay: Long) {
        srsFlvMuxer.reConnect(delay)
    }

    protected fun onSpsPpsVpsRtp(
        sps: ByteBuffer?,
        pps: ByteBuffer?,
        vps: ByteBuffer?
    ) {
        srsFlvMuxer.setSpsPPs(sps, pps)
    }

    protected fun getH264DataRtp(
        h264Buffer: ByteBuffer?,
        info: MediaCodec.BufferInfo?
    ) {
        srsFlvMuxer.sendVideo(h264Buffer, info)
    }

    protected fun getAacDataRtp(
        aacBuffer: ByteBuffer?,
        info: MediaCodec.BufferInfo?
    ) {
        srsFlvMuxer.sendAudio(aacBuffer, info)
    }

    private fun init() {
        videoEncoder = VideoEncoder(this)
        microphoneManager = MicrophoneManager(this)
        audioEncoder = AudioEncoder(this)
        recordController = RecordController()
    }

    /**
     * @param callback get fps while record or stream
     */
    fun setFpsListener(callback: FpsListener.Callback?) {
        fpsListener.setCallback(callback)
    }

    /**
     * @param filePath to video MP4 file.
     * @param bitRate H264 in kb.
     * @return true if success, false if you get a error (Normally because the encoder selected
     * doesn't support any configuration seated or your device hasn't a H264 encoder).
     * @throws IOException Normally file not found.
     */
    @JvmOverloads
    @Throws(IOException::class)
    fun prepareVideo(
        width: Int, height: Int, fps: Int = 30, bitrate: Int = 4200 * 1024, rotation: Int = 0,
        avcProfile: Int = -1, avcProfileLevel: Int = -1
    ): Boolean {
        val result = videoEncoder!!.prepareVideoEncoder(
            width, height, fps, bitrate, rotation, true, 2,
            FormatVideoEncoder.SURFACE, avcProfile, avcProfileLevel
        )
        if (glInterface != null) {
            glInterface = OffScreenGlThread(context)
            glInterface!!.init()
            glInterface!!.setEncoderSize(videoEncoder!!.width, videoEncoder!!.height)
        }
        return result
    }

    fun prepareVideo(): Boolean {
        return prepareVideo(640, 480)
    }

    /**
     * Call this method before use @startStream. If not you will do a stream without audio.
     *
     * @param bitrate AAC in kb.
     * @param sampleRate of audio in hz. Can be 8000, 16000, 22500, 32000, 44100.
     * @param isStereo true if you want Stereo audio (2 audio channels), false if you want Mono audio
     * (1 audio channel).
     * @param echoCanceler true enable echo canceler, false disable.
     * @param noiseSuppressor true enable noise suppressor, false  disable.
     * @return true if success, false if you get a error (Normally because the encoder selected
     * doesn't support any configuration seated or your device hasn't a AAC encoder).
     */
    fun prepareAudio(
        bitrate: Int, sampleRate: Int, isStereo: Boolean, echoCanceler: Boolean,
        noiseSuppressor: Boolean
    ): Boolean {
        microphoneManager!!.createMicrophone(sampleRate, isStereo, echoCanceler, noiseSuppressor)
        prepareAudioRtp(isStereo, sampleRate)
        return audioEncoder!!.prepareAudioEncoder(
            bitrate, sampleRate, isStereo,
            microphoneManager!!.getMaxInputSize()
        )
    }

    /**
     * Same to call:
     * prepareAudio(64 * 1024, 32000, true, false, false);
     *
     * @return true if success, false if you get a error (Normally because the encoder selected
     * doesn't support any configuration seated or your device hasn't a AAC encoder).
     */
    fun prepareAudio(): Boolean {
        return prepareAudio(64 * 1024, 32000, true, false, false)
    }

    /**
     * @param forceVideo force type codec used. FIRST_COMPATIBLE_FOUND, SOFTWARE, HARDWARE
     */
    fun setForce(forceVideo: Force?, forceAudio: Force?) {
        videoEncoder!!.setForce(forceVideo)
        audioEncoder!!.setForce(forceAudio)
    }

    /**
     * Start record a MP4 video. Need be called while stream.
     *
     * @param path where file will be saved.
     * @throws IOException If you init it before start stream.
     */
    @JvmOverloads
    @Throws(IOException::class)
    fun startRecord(
        path: String?,
        listener: RecordController.Listener? = null
    ) {
        recordController!!.startRecord(path, listener)
        if (!isStreaming) {
            startEncoders()
        } else if (videoEncoder!!.isRunning) {
            resetVideoEncoder()
        }
    }

    /**
     * Stop record MP4 video started with @startRecord. If you don't call it file will be unreadable.
     */
    fun stopRecord() {
        recordController!!.stopRecord()
        if (!isStreaming) stopStream()
    }

    /**
     * Need be called after @prepareVideo.
     *
     * @param url of the stream like:
     * protocol://ip:port/application/streamName
     *
     * RTSP: rtsp://192.168.1.1:1935/live/pedroSG94
     * RTSPS: rtsps://192.168.1.1:1935/live/pedroSG94
     * RTMP: rtmp://192.168.1.1:1935/live/pedroSG94
     * RTMPS: rtmps://192.168.1.1:1935/live/pedroSG94
     */
    fun startStream(url: String?) {
        isStreaming = true
        if (!recordController!!.isRunning) {
            startEncoders()
        } else {
            resetVideoEncoder()
        }
        startStreamRtp(url)
    }

    private fun startEncoders() {
        videoEncoder!!.start()
        audioEncoder!!.start()
        prepareGlView()
        microphoneManager!!.start()
    }

    private fun prepareGlView() {
        if (glInterface != null) {
            if (glInterface is OffScreenGlThread) {
                glInterface = OffScreenGlThread(context)
                glInterface!!.init()
            }
            glInterface!!.setFps(videoEncoder!!.fps)
            if (videoEncoder!!.rotation == 90 || videoEncoder!!.rotation == 270) {
                glInterface!!.setEncoderSize(videoEncoder!!.height, videoEncoder!!.width)
            } else {
                glInterface!!.setEncoderSize(videoEncoder!!.width, videoEncoder!!.height)
            }
            glInterface!!.setRotation(0)
            glInterface!!.start()
            if (videoEncoder!!.inputSurface != null) {
                glInterface!!.addMediaCodecSurface(videoEncoder!!.inputSurface)
            }
            arSceneView.startMirroringToSurface(glInterface!!.surface, 0, 0, videoEncoder!!.width, videoEncoder!!.height)
        } else {
            arSceneView.startMirroringToSurface(videoEncoder!!.inputSurface, 0, 0, videoEncoder!!.width, videoEncoder!!.height)
        }
    }

    private fun resetVideoEncoder() {
        try {
            if (glInterface != null) {
                glInterface!!.removeMediaCodecSurface()
                glInterface!!.stop()
            }
            videoEncoder!!.reset()
            prepareGlView()
        } catch (e: IOException) {
            Log.e(TAG, "Error", e)
        }
    }

    fun reTry(delay: Long, reason: String?): Boolean {
        val result = shouldRetry(reason)
        if (result) {
            reTry(delay)
        }
        return result
    }

    /**
     * Replace with reTry(long delay, String reason);
     */
    @Deprecated("")
    fun reTry(delay: Long) {
        resetVideoEncoder()
        reConnect(delay)
    }

    /**
     * Stop stream started with @startStream.
     */
    fun stopStream() {
        if (isStreaming) {
            isStreaming = false
            stopStreamRtp()
        }
        if (!recordController!!.isRecording) {
            if (glInterface != null) {
                glInterface!!.removeMediaCodecSurface()
                glInterface!!.stop()
            }
            videoEncoder!!.stop()
            audioEncoder!!.stop()
            recordController!!.resetFormats()
        }
    }

    fun getGlInterface(): GlInterface {
        return if (glInterface != null) {
            glInterface!!
        } else {
            throw RuntimeException("You can't do it. You are not using Opengl")
        }
    }

    val bitrate: Int
        get() = videoEncoder!!.bitRate

    val resolutionValue: Int
        get() = videoEncoder!!.width * videoEncoder!!.height

    val streamWidth: Int
        get() = videoEncoder!!.width

    val streamHeight: Int
        get() = videoEncoder!!.height

    /**
     * Set video bitrate of H264 in kb while stream.
     *
     * @param bitrate H264 in kb.
     */
    fun setVideoBitrateOnFly(bitrate: Int) {
        if (Build.VERSION.SDK_INT >= 19) {
            videoEncoder!!.setVideoBitrateOnFly(bitrate)
        }
    }

    /**
     * Set limit FPS while stream. This will be override when you call to prepareVideo method.
     * This could produce a change in iFrameInterval.
     *
     * @param fps frames per second
     */
    fun setLimitFPSOnFly(fps: Int) {
        videoEncoder!!.fps = fps
    }

    /**
     * Get record state.
     *
     * @return true if recording, false if not recoding.
     */
    val isRecording: Boolean
        get() = recordController!!.isRunning

    fun pauseRecord() {
        recordController!!.pauseRecord()
    }

    fun resumeRecord() {
        recordController!!.resumeRecord()
    }

    val recordStatus: RecordController.Status
        get() = recordController!!.status

    override fun onSpsPps(sps: ByteBuffer, pps: ByteBuffer) {
        if (isStreaming) onSpsPpsVpsRtp(sps, pps, null)
    }

    override fun onSpsPpsVps(
        sps: ByteBuffer,
        pps: ByteBuffer,
        vps: ByteBuffer
    ) {
        if (isStreaming) onSpsPpsVpsRtp(sps, pps, vps)
    }

    override fun getVideoData(
        h264Buffer: ByteBuffer,
        info: MediaCodec.BufferInfo
    ) {
        fpsListener.calculateFps()
        recordController!!.recordVideo(h264Buffer, info)
        if (isStreaming) getH264DataRtp(h264Buffer, info)
    }

    override fun onVideoFormat(mediaFormat: MediaFormat) {
        recordController!!.setVideoFormat(mediaFormat)
    }

    override fun getAacData(
        aacBuffer: ByteBuffer,
        info: MediaCodec.BufferInfo
    ) {
        recordController!!.recordAudio(aacBuffer, info)
        if (isStreaming) getAacDataRtp(aacBuffer, info)
    }

    override fun onAudioFormat(mediaFormat: MediaFormat) {
        recordController!!.setAudioFormat(mediaFormat)
    }

    override fun inputPCMData(frame: Frame) {
        audioEncoder!!.inputPCMData(frame)
    }

    companion object {
        private const val TAG = "FromFileBase"
    }
}