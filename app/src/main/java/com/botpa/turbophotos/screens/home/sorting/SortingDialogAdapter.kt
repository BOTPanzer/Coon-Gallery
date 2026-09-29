package com.botpa.turbophotos.screens.home.sorting

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.data.SortDirection
import com.botpa.turbophotos.gallery.data.SortMethod
import com.botpa.turbophotos.gallery.modals.core.SimpleCustomAdapter

class SortingDialogAdapter(context: Context, items: List<SortingItem>) : SimpleCustomAdapter<SortingItem, SortingDialogAdapter.FilterHolder>(context, items) {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): FilterHolder {
        return FilterHolder(inflateView(context, R.layout.dialog_selectable_item, viewGroup))
    }

    override fun onInitItemHolder(holder: FilterHolder, item: SortingItem) {
        //Update info
        holder.selected.visibility = if (item.isSelected) View.VISIBLE else View.GONE
        holder.name.text = localizeMethod(item.method, item.direction)

        //Add listeners
        holder.item.setOnClickListener { view ->
            onClick?.run(item, holder.bindingAdapterPosition)
        }
    }

    //Util
    private fun localizeMethod(method: SortMethod, direction: SortDirection): String {
        return context.getString(when (method) {
            //Date
            SortMethod.Date -> {
                when (direction) {
                    SortDirection.Ascending -> R.string.dialog_sorting_date_ascending
                    SortDirection.Descending -> R.string.dialog_sorting_date_descending
                }
            }
            //Name
            SortMethod.Name -> {
                when (direction) {
                    SortDirection.Ascending -> R.string.dialog_sorting_name_ascending
                    SortDirection.Descending -> R.string.dialog_sorting_name_descending
                }
            }
            //Size
            SortMethod.Size -> {
                when (direction) {
                    SortDirection.Ascending -> R.string.dialog_sorting_size_ascending
                    SortDirection.Descending -> R.string.dialog_sorting_size_descending
                }
            }
        })
    }

    //Holder
    class FilterHolder(root: View) : RecyclerView.ViewHolder(root) {

        val item: View = root
        val selected: ImageView = root.findViewById(R.id.itemSelected)
        val name: TextView = root.findViewById(R.id.itemName)

    }

}