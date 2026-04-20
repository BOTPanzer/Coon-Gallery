package com.botpa.turbophotos.gallery.options

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import androidx.activity.BackEventCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.util.BackAnimationEvent
import com.botpa.turbophotos.util.BackManager
import com.botpa.turbophotos.util.Ease
import com.botpa.turbophotos.util.Orion
import kotlin.math.sign

@SuppressLint("NotifyDataSetChanged")
class OptionsManager(val activity: Activity, val options: MutableList<OptionsGroup>, private val backManager: BackManager, private val onUpdateOptions: () -> Unit) {

    //Views
    val layout: View = activity.findViewById(R.id.optionsLayout)
    val menu: View = activity.findViewById(R.id.optionsMenu)
    val list: RecyclerView = activity.findViewById(R.id.optionsList)

    //Adapter
    private val adapter: OptionsGroupAdapter


    //Constructor
    init {
        //Init options layout manager & separator gap
        list.setLayoutManager(LinearLayoutManager(activity))
        list.addItemDecoration(OptionsSeparator(5))

        //Init options adapter
        adapter = OptionsGroupAdapter(activity, options)
        adapter.setOnClickListener { option: OptionsItem, index: Int ->
            //Get action
            val action = option.action ?: return@setOnClickListener

            //Invoke action & close menu
            action.run()
            toggle(false)
        }
        list.setAdapter(adapter)
    }

    //Menu
    fun toggle(show: Boolean) {
        if (show) {
            //Update options list
            options.clear()
            onUpdateOptions.invoke()
            adapter.notifyDataSetChanged()

            //Show menu & layout
            val duration = 250
            menu.alpha = 1.0f
            menu.translationX = 100.0f
            Orion.animateMoveX(menu, 0.0f, duration)
            Orion.animateShow(layout, duration)

            //Register back event
            backManager.register("options", object : BackAnimationEvent {

                override fun onProgress(backEvent: BackEventCompat) {
                    //Get info
                    val direction = if (backEvent.swipeEdge == BackEventCompat.EDGE_LEFT) 1.0f else -1.0f
                    val width = activity.windowManager.currentWindowMetrics.bounds.width()
                    val easeOut = Ease.outCubic(backEvent.progress)

                    //Animate
                    menu.translationX = easeOut * direction * width / 4
                    menu.alpha = 1.0f - easeOut * 0.8f
                }

                override fun onInvoked() {
                    toggle(false)
                }

            })
        } else {
            //Hide menu & layout
            val direction = if (menu.translationX != 0.0f) menu.translationX.sign else 1.0f
            val width = activity.windowManager.currentWindowMetrics.bounds.width()
            Orion.animateMoveX(menu, direction * width / 4)
            Orion.animateHide(layout)

            //Unregister back event
            backManager.unregister("options")
        }
    }

}