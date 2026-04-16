package com.botpa.turbophotos.gallery.modals

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.botpa.turbophotos.R

class DialogBulletpointsAdapter(
    context: Context,
    points: List<String>
) : ArrayAdapter<String>(context, 0, points) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        //Inflate
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.dialog_points_item, parent, false)

        //Get view
        val text = view.findViewById<TextView>(R.id.pointText)

        //Update item
        text.text = getItem(position)

        return view
    }

}