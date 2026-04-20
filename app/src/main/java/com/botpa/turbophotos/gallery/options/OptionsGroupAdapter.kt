package com.botpa.turbophotos.gallery.options

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R

class OptionsGroupAdapter(
    private val context: Context,
    private val items: List<OptionsGroup>,
) : RecyclerView.Adapter<OptionsGroupAdapter.OptionHolder>() {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): OptionHolder {
        return OptionHolder(LayoutInflater.from(context).inflate(R.layout.options_group, viewGroup, false))
    }

    override fun onBindViewHolder(holder: OptionHolder, i: Int) {
        //Get holder position
        val position = holder.bindingAdapterPosition

        //Get item
        val item = items[position]

        //Create list
        val adapter = OptionsItemAdapter(context, item.options)
        adapter.setOnClickListener { option, index ->
            onClickListener?.onClick(option, index)
        }
        holder.list.layoutManager = LinearLayoutManager(context)
        holder.list.adapter = adapter
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

        val list: RecyclerView = view.findViewById(R.id.optionsList)

    }

}