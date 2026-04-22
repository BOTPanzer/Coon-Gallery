package com.botpa.turbophotos.gallery.modals

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.actions.ActionError
import com.botpa.turbophotos.gallery.modals.core.SimpleCustomAdapter

class ErrorsDialogAdapter(
    context: Context,
    errors: List<ActionError>
) : SimpleCustomAdapter<ActionError, ErrorsDialogAdapter.ErrorHolder>(context, errors) {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ErrorHolder {
        return ErrorHolder(inflateView(context, R.layout.dialog_errors_item, viewGroup))
    }

    override fun onInitViewHolder(holder: ErrorHolder, error: ActionError) {
        //Update info
        holder.name.text = error.item.name
        holder.reason.text = error.reason

        //Add listeners
        holder.item.setOnClickListener { view ->
            onClick?.run(error, holder.bindingAdapterPosition)
        }
    }

    //Holder
    class ErrorHolder(root: View) : RecyclerView.ViewHolder(root) {

        val item: View = root
        val name: TextView = root.findViewById(R.id.errorItem)
        val reason: TextView = root.findViewById(R.id.errorReason)

    }

}