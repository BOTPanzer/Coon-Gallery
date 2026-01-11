package com.botpa.turbophotos.gallery.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Album

class DialogAlbumsAdapter(
    context: Context,
    albums: List<Album>
) : ArrayAdapter<Album>(context, 0, albums) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        //Inflate
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.dialog_albums_item, parent, false)

        //Get views
        val name = view.findViewById<TextView>(R.id.albumName)

        //Get album
        val album = getItem(position)

        //Update name
        if (album != null) {
            name.text = album.name
        }

        return view
    }

}