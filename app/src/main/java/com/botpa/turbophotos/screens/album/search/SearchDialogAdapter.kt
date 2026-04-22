package com.botpa.turbophotos.screens.album.search

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.SearchMethod
import com.botpa.turbophotos.gallery.modals.core.SimpleCustomAdapter

class SearchDialogAdapter(context: Context, items: List<SearchMethod>) : SimpleCustomAdapter<SearchMethod, SearchDialogAdapter.SearchHolder>(context, items) {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): SearchHolder {
        return SearchHolder(inflateView(context, R.layout.dialog_search_item, viewGroup))
    }

    override fun onInitViewHolder(holder: SearchHolder, item: SearchMethod) {
        //Update info
        when (item) {
            SearchMethod.ContainsWords -> {
                holder.name.text = "Contains words"
                holder.description.text = "Checks if all words in the search are contained."
                holder.use.text = "· Finding whole words like \"cat\", \"pink\"..."
            }
            SearchMethod.ContainsText -> {
                holder.name.text = "Contains text"
                holder.description.text = "Checks if the whole text is contained."
                holder.use.text = "· Finding a whole sentence.\n· Finding part of a word."
            }
        }

        //Add listeners
        holder.item.setOnClickListener { view ->
            onClick?.run(item, holder.bindingAdapterPosition)
        }
    }

    //Holder
    class SearchHolder(root: View) : RecyclerView.ViewHolder(root) {

        val item: View = root
        val name: TextView = root.findViewById(R.id.methodName)
        val description: TextView = root.findViewById(R.id.methodDescription)
        val use: TextView = root.findViewById(R.id.methodUse)

    }

}