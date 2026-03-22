package com.botpa.turbophotos.screens.album.search

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.SearchMethod

class DialogSearchAdapter(context: Context, items: List<SearchMethod>) : ArrayAdapter<SearchMethod>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        //Inflate
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.dialog_search_item, parent, false)

        //Get views
        val name = view.findViewById<TextView>(R.id.methodName)
        val description = view.findViewById<TextView>(R.id.methodDescription)
        val use = view.findViewById<TextView>(R.id.methodUse)

        //Get item
        val item = getItem(position)

        //Update name
        if (item != null) {
            when (item) {
                SearchMethod.ContainsWords -> {
                    name.text = "Contains words"
                    description.text = "Checks if all words in the search are contained."
                    use.text = "· Finding whole words like \"cat\", \"dog\", \"pink\"..."
                }
                SearchMethod.ContainsText -> {
                    name.text = "Contains text"
                    description.text = "Checks if the whole text is contained."
                    use.text = "· Finding a whole sentence.\n· Finding part of a word."
                }
            }
        }

        return view
    }

}