package com.botpa.turbophotos.gallery.options

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.modals.core.CustomAdapter

class OptionsGroupAdapter(
    private val context: Context,
    items: List<OptionsGroup>,
) : CustomAdapter<OptionsGroup, OptionsGroupAdapter.OptionHolder>(items) {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): OptionHolder {
        return OptionHolder(inflateView(context, R.layout.options_group, viewGroup))
    }

    override fun onInitViewHolder(holder: OptionHolder, item: OptionsGroup) {
        //Create adapter
        val adapter = OptionsItemAdapter(context, item.options)
        adapter.onClick = { option, index ->
            onClick?.run(option, index)
        }

        //Init list
        holder.list.layoutManager = LinearLayoutManager(context)
        holder.list.adapter = adapter
    }

    //Listeners
    var onClick: ClickListener? = null

    fun interface ClickListener {
        fun run(option: OptionsItem, index: Int)
    }

    //Holder
    class OptionHolder(root: View) : RecyclerView.ViewHolder(root) {

        val list: RecyclerView = root.findViewById(R.id.optionsList)

    }

}