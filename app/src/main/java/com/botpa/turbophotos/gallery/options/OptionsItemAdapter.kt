package com.botpa.turbophotos.gallery.options

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.modals.core.CustomAdapter

class OptionsItemAdapter(
    context: Context,
    items: List<OptionsItem>,
) : CustomAdapter<OptionsItem, OptionsItemAdapter.OptionHolder>(context, items) {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): OptionHolder {
        return OptionHolder(inflateView(context, R.layout.options_item, viewGroup))
    }

    override fun onInitViewHolder(holder: OptionHolder, item: OptionsItem) {
        //Update info
        holder.icon.setImageResource(item.icon)
        holder.name.text = item.name

        //Add listeners
        holder.item.setOnClickListener { view: View ->
            onClick?.run(item, holder.bindingAdapterPosition)
        }
    }

    //Listeners
    var onClick: ClickListener? = null

    fun interface ClickListener {
        fun run(option: OptionsItem, position: Int)
    }

    //Holder
    class OptionHolder(root: View) : RecyclerView.ViewHolder(root) {

        val item: View = root.findViewById(R.id.optionItem)
        val icon: ImageView = root.findViewById(R.id.optionIcon)
        val name: TextView = root.findViewById(R.id.optionName)

    }

}