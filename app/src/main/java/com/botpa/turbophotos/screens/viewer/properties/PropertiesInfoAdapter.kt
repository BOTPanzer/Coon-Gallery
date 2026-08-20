package com.botpa.turbophotos.screens.viewer.properties

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.modals.core.SimpleCustomAdapter

class PropertiesInfoAdapter(context: Context, items: List<PropertiesInfo>) : SimpleCustomAdapter<PropertiesInfo, PropertiesInfoAdapter.ItemHolder>(context, items) {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder(inflateView(context, R.layout.drawer_properties_info_item, viewGroup))
    }

    override fun onInitItemHolder(holder: ItemHolder, item: PropertiesInfo) {
        //Update info
        holder.name.text = context.getString(item.name)
        holder.info.text = item.info

        //Add listeners
        holder.item.setOnClickListener { view ->
            onClick?.run(item, holder.bindingAdapterPosition)
        }
    }

    //Holder
    class ItemHolder(root: View) : RecyclerView.ViewHolder(root) {

        val item: View = root
        val name: TextView = root.findViewById(R.id.itemName)
        val info: TextView = root.findViewById(R.id.itemInfo)

    }

}