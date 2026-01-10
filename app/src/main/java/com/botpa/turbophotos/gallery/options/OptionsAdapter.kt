package com.botpa.turbophotos.gallery.options

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R

class OptionsAdapter(
    private val context: Context,
    private val items: List<OptionsItem>,
) : RecyclerView.Adapter<OptionsAdapter.OptionHolder>() {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): OptionHolder {
        return OptionHolder(LayoutInflater.from(context).inflate(R.layout.options_item, viewGroup, false))
    }

    override fun onBindViewHolder(holder: OptionHolder, i: Int) {
        //Get holder position
        val position = holder.bindingAdapterPosition

        //Get item
        val item = items[position]

        //Check if is separator
        if (item.isSeparator) {
            //Toggle views
            holder.separator.visibility = View.VISIBLE
            holder.item.visibility = View.GONE
        } else {
            //Toggle views
            holder.separator.visibility = View.GONE
            holder.item.visibility = View.VISIBLE

            //Update views
            holder.icon.setImageResource(item.icon)
            holder.name.text = item.name

            //Add click listeners
            holder.item.setOnClickListener { view: View ->
                onClickListener?.onClick(view, holder.bindingAdapterPosition)
            }
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }

    //Listeners
    private var onClickListener: OnClickListener? = null

    fun interface OnClickListener {
        fun onClick(view: View, index: Int)
    }

    fun setOnClickListener(onClickListener: OnClickListener?) {
        this.onClickListener = onClickListener
    }

    //Holder
    class OptionHolder(view: View) : RecyclerView.ViewHolder(view) {

        var separator: View = view.findViewById(R.id.optionSeparator)
        var item: View = view.findViewById(R.id.optionItem)
        var icon: ImageView = view.findViewById(R.id.optionIcon)
        var name: TextView = view.findViewById(R.id.optionName)

    }

}