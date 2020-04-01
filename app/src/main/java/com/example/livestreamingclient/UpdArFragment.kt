package com.example.livestreamingclient

import android.util.ArraySet
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.ArFragment

class UpdArFragment : ArFragment {
    val anchorSet: ArraySet<AnchorNode> = ArraySet()

    constructor() : super() {}

    override fun onUpdate(frameTime: FrameTime) {
        super.onUpdate(frameTime)
        this.anchorSet.forEach {
            val pos = it.worldPosition
            val screenPos = this.arSceneView.scene.camera.worldToScreenPoint(pos)
            val x: Float = screenPos.x / 1080
            val y: Float = screenPos.y / 1920
            val onScreen = x > 0 && x < 1 && y > 0 && y < 1
            println("Anchor Update: $x, $y, $onScreen")
        }
    }

    fun addNodeByHit(hitResult: HitResult, renderable: Renderable) {
        this.placeRenderable(hitResult.createAnchor(), renderable)
    }

    fun addNodeByXY(x: Double, y: Double, renderable: Renderable) {
        val hitResult = this.arSceneView.arFrame?.hitTest(x.toFloat() * 1080, y.toFloat() * 1920)?.get(0)
        hitResult?.let { this.addNodeByHit(hitResult, renderable) }
    }

    fun placeRenderable(anchor: Anchor, renderable: Renderable) {
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(this.arSceneView.scene)

        val model = Node()
        model.setParent(anchorNode)
        model.renderable = renderable
        model.setOnTapListener { hitResult, motionEvent ->
            this.anchorSet.remove(anchorNode)
            anchorNode.anchor?.detach()
        }

        this.anchorSet.add(anchorNode)
    }
}