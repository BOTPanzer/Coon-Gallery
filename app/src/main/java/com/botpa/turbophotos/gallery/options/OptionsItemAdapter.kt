package com.botpa.turbophotos.gallery.options

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R

class OptionsItemAdapter(
    private val context: Context,
    private val items: List<OptionsItem>,
) : RecyclerView.Adapter<OptionsItemAdapter.OptionHolder>() {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): OptionHolder {
        return OptionHolder(LayoutInflater.from(context).inflate(R.layout.options_item, viewGroup, false))
    }

    override fun onBindViewHolder(holder: OptionHolder, i: Int) {
        //Get holder position
        val position = holder.bindingAdapterPosition

        //Get item
        val item = items[position]

        //Update info
        holder.icon.setImageResource(item.icon)
        holder.name.text = item.name

        //Add click listeners
        holder.item.setOnClickListener { view: View ->
            onClickListener?.onClick(item, holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    //Listeners
    private var onClickListener: OnClickListener? = null

    fun interface OnClickListener {
        fun onClick(option: OptionsItem, index: Int)
    }

    fun setOnClickListener(onClickListener: OnClickListener?) {
        this.onClickListener = onClickListener
    }

    //Holder
    class OptionHolder(view: View) : RecyclerView.ViewHolder(view) {

        val item: View = view.findViewById(R.id.optionItem)
        val icon: ImageView = view.findViewById(R.id.optionIcon)
        val name: TextView = view.findViewById(R.id.optionName)

    }

}