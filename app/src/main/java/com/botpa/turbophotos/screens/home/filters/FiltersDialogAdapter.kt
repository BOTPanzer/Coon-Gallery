package com.botpa.turbophotos.screens.home.filters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.modals.core.CustomAdapter

class FiltersDialogAdapter(
    context: Context,
    items: List<Filter>
) : CustomAdapter<Filter, FiltersDialogAdapter.FilterHolder>(context, items) {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): FilterHolder {
        return FilterHolder(inflateView(context, R.layout.dialog_filters_item, viewGroup))
    }

    override fun onInitViewHolder(holder: FilterHolder, item: Filter) {
        //Update info
        holder.icon.setImageResource(item.icon)
        holder.name.text = item.name

        //Add listeners
        holder.item.setOnClickListener { view ->
            onClick?.run(item, holder.bindingAdapterPosition)
        }
    }

    //Listeners
    var onClick: ClickListener? = null

    fun interface ClickListener {
        fun run(filter: Filter, position: Int)
    }

    //Holder
    class FilterHolder(root: View) : RecyclerView.ViewHolder(root) {

        val item: View = root
        val icon: ImageView = root.findViewById(R.id.filterIcon)
        val name: TextView = root.findViewById(R.id.filterName)

    }

}