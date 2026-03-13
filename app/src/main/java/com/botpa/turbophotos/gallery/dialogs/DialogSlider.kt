package com.botpa.turbophotos.gallery.dialogs

import android.content.Context
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.botpa.turbophotos.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider

class DialogSlider(
    context: Context,
    private val title: String,
    private val value: Float,
    private val min: Float,
    private val max: Float,
    private val stepSize: Float,
    private val onConfirm: (Float) -> Unit
) : CustomDialog(context, R.layout.dialog_slider) {

    //Views
    private lateinit var slider: Slider
    private lateinit var sliderText: TextView


    //Init
    override fun initViews() {
        //Init views
        slider = root.findViewById(R.id.slider)
        sliderText = root.findViewById(R.id.sliderText)

        //Update slider values
        slider.valueFrom = min
        slider.valueTo = max
        slider.value = value
        slider.stepSize = stepSize

        //Update text
        sliderText.text = value.toString()
    }

    override fun initDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        //Init dialog
        return builder
            .setTitle(title)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm", null)
    }

    override fun initListeners() {
        //Update text
        slider.addOnChangeListener { slider, value, fromUser ->
            sliderText.text = value.toString()
        }

        //Confirm (adding listener like this prevent the button from dismissing the dialog)
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener { view ->
            //Accept value
            onConfirm(slider.value)
            dialog.dismiss()
        }
    }

}