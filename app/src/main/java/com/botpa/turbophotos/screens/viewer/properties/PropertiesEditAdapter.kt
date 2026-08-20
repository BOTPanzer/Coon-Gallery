package com.botpa.turbophotos.screens.viewer.properties

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.modals.core.SimpleCustomAdapter

class PropertiesEditAdapter(context: Context, private val labels: MutableList<String>, private val hint: Int) : SimpleCustomAdapter<String, PropertiesEditAdapter.ItemHolder>(context, labels) {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder(inflateView(context, R.layout.drawer_properties_edit_item, viewGroup))
    }

    override fun onInitItemHolder(holder: ItemHolder, item: String) {
        //Update info
        holder.value.setText(item)
        holder.value.setHint(hint)

        //Add listeners
        holder.textWatcher?.let { holder.value.removeTextChangedListener(it) }
        val watcher = object : TextWatcher {
            override fun afterTextChanged(text: Editable?) {}
            override fun beforeTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < labels.size) {
                    labels[pos] = text.toString()
                }
            }
        }
        holder.textWatcher = watcher
        holder.value.addTextChangedListener(watcher)

        holder.item.setOnClickListener { view ->
            onClick?.run(item, holder.bindingAdapterPosition)
        }

        holder.remove.setOnClickListener { view ->
            onRemove?.run(item, holder.bindingAdapterPosition)
        }
    }

    //Listeners
    var onRemove: ClickListener<String>? = null

    //Holder
    class ItemHolder(root: View) : RecyclerView.ViewHolder(root) {

        val item: View = root
        val value: EditText = root.findViewById(R.id.itemValue)
        val remove: View = root.findViewById(R.id.itemRemove)
        var textWatcher: TextWatcher? = null

    }

}