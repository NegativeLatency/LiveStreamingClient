package com.example.livestreamingclient.controller

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.livestreamingclient.R
import razerdp.basepopup.BasePopupWindow

class PopupController: BasePopupWindow {

    var editText: EditText
    var postButton: Button

    constructor(context: Context): super(context) {
        editText = findViewById(R.id.editText)
        postButton = findViewById(R.id.postButton)
        popupGravity = Gravity.BOTTOM
        setPopupWindowFullScreen(true)
        setBackground(0)
        setAdjustInputMethod(true, WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setAutoShowInputMethod(editText, true)
        postButton.setOnClickListener {
            val text = editText.text.toString()
            editText.text.clear()
            Toast.makeText(context, "Message Sent: $text", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateContentView(): View {
        return createPopupById(R.layout.popup_dialog)
    }

    override fun onCreateShowAnimation(): Animation {
        return getTranslateVerticalAnimation(1f, 0f, 450)
    }


    override fun onCreateDismissAnimation(): Animation {
        return getTranslateVerticalAnimation(0f, 1f, 450)
    }

}