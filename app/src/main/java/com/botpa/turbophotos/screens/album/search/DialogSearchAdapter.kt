package com.botpa.turbophotos.screens.album.search

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.screens.home.filters.Filter

class DialogSearchAdapter(context: Context, items: List<Library.SearchMethod>) : ArrayAdapter<Library.SearchMethod>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        //Inflate
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.dialog_search_item, parent, false)

        //Get views
        val name = view.findViewById<TextView>(R.id.methodName)
        val description = view.findViewById<TextView>(R.id.methodDescription)

        //Get item
        val item = getItem(position)

        //Update name
        if (item != null) {
            when (item) {
                Library.SearchMethod.ContainsText -> {
                    name.text = "Contains text"
                    description.text = "Checks if the whole text is contained."
                }
                Library.SearchMethod.ContainsWords -> {
                    name.text = "Contains words"
                    description.text = "Checks if all words in the search are contained."
                }
            }
        }

        return view
    }

}