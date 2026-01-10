package com.botpa.turbophotos.gallery.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.actions.ActionError

class DialogErrorsAdapter(
    context: Context,
    errors: MutableList<ActionError?>
) : ArrayAdapter<ActionError?>(context, 0, errors) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        //Inflate
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.dialog_error_item, parent, false)

        //Get views
        val name = view.findViewById<TextView>(R.id.failItem)
        val reason = view.findViewById<TextView>(R.id.failReason)

        //Get error
        val error = getItem(position)

        //Update name & reason texts
        if (error != null) {
            //Update item name & reason
            name.text = error.item.name
            reason.text = error.reason
        }

        return view
    }

}