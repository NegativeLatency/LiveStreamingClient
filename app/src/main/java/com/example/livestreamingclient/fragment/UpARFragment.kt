package com.example.livestreamingclient.fragment

import android.util.ArraySet
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import com.example.livestreamingclient.R
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.ux.ArFragment

class UpARFragment : ArFragment {

    val BtnSize = 256

    class ArAnchorNode {
        var node: AnchorNode
        var btn: ImageButton
        var fr: UpARFragment
        var isDistoryed = false

        constructor(fr: UpARFragment, node: AnchorNode, btn: ImageButton) {
            this.node = node
            this.btn = btn
            this.fr = fr
        }

        fun destory() {
            if (!isDistoryed) {
                fr.anchorSet.remove(this)
                node.anchor?.detach()
                btn.visibility = View.GONE
            }
        }
    }
    val anchorSet: ArraySet<ArAnchorNode> = ArraySet()

    constructor() : super() {}

    override fun onUpdate(frameTime: FrameTime) {
        super.onUpdate(frameTime)
        this.anchorSet.forEach {
            val pos = it.node.worldPosition
            val screenPos = this.arSceneView.scene.camera.worldToScreenPoint(pos)
            val x: Float = screenPos.x / 1080
            val y: Float = screenPos.y / 1920
            val onScreen = x > 0 && x < 1 && y > 0 && y < 1
            it.btn.translationX = screenPos.x - BtnSize / 2
            it.btn.translationY = screenPos.y - BtnSize / 2
            println("Anchor Update: ${screenPos.x}($x), ${screenPos.y}($y), ${this.arSceneView.scene.camera.worldRotation.w}, $onScreen")
        }
    }

    fun addNodeByHit(hitResult: HitResult): ArAnchorNode {
        return this.placeRenderable(hitResult.createAnchor())
    }

    fun addNodeByXY(x: Float, y: Float): ArAnchorNode? {
        this.arSceneView.arFrame?.hitTest(x, y)?.let {
            if (it.size > 0) {
                return this.addNodeByHit(it[0])
            }
        }
        return null
    }

    fun placeRenderable(anchor: Anchor): ArAnchorNode {
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(this.arSceneView.scene)

        val newBtn = ImageButton(this.context)
        newBtn.setImageResource(R.drawable.ring)

        val lp = RelativeLayout.LayoutParams(BtnSize, BtnSize)
        newBtn.layoutParams = lp
        newBtn.scaleType = ImageView.ScaleType.CENTER_INSIDE
        newBtn.adjustViewBounds = true
        newBtn.background = null
        newBtn.alpha = .8f

        val arAnchorNode =
            ArAnchorNode(
                this,
                anchorNode,
                newBtn
            )
        this.anchorSet.add(arAnchorNode)
        return arAnchorNode
    }
}