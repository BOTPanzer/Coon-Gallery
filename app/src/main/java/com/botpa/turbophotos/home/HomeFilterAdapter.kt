package com.botpa.turbophotos.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.botpa.turbophotos.R

class HomeFilterAdapter(
    context: Context,
    items: MutableList<HomeFilter>
) : ArrayAdapter<HomeFilter>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        //Inflate
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.home_filter_item, parent, false)

        //Get item
        val item = getItem(position)

        //Get views
        val name = view.findViewById<TextView>(R.id.filterName)

        //Update name
        if (item != null) name.text = item.name

        return view
    }

}