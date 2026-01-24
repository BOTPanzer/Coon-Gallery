package com.botpa.turbophotos.screens.home.filters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.botpa.turbophotos.R

class DialogFiltersAdapter(context: Context, items: List<Filter>) : ArrayAdapter<Filter>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        //Inflate
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.dialog_filters_item, parent, false)

        //Get views
        val icon = view.findViewById<ImageView>(R.id.filterIcon)
        val name = view.findViewById<TextView>(R.id.filterName)

        //Get item
        val item = getItem(position)

        //Update name
        if (item != null) {
            icon.setImageResource(item.icon)
            name.text = item.name
        }

        return view
    }

}