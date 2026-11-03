package com.botpa.turbophotos.gallery.modals

import android.app.Activity
import android.widget.Button
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.modals.core.CustomAdapter
import com.botpa.turbophotos.gallery.modals.core.CustomDrawer
import com.botpa.turbophotos.gallery.views.lists.ListSeparator
import java.util.Collections

class ReorderableDrawer<T, VH: RecyclerView.ViewHolder>(
    activity: Activity,
    private val adapter: CustomAdapter<T, VH>,
    private val items: MutableList<T>,
    private val onConfirm: () -> Unit,
    private val canSwipeToRemove: Boolean = false
) : CustomDrawer(activity, R.layout.drawer_reorderable) {

    //Views
    private lateinit var reorderableList: RecyclerView
    private lateinit var reorderableCancel: Button
    private lateinit var reorderableConfirm: Button


    //Init
    override fun initViews() {
        reorderableList = root.findViewById(R.id.reorderableList)
        reorderableCancel = root.findViewById(R.id.reorderableCancel)
        reorderableConfirm = root.findViewById(R.id.reorderableConfirm)
    }

    override fun initListeners() {
        //Cancel
        reorderableCancel.setOnClickListener {
            dialog.dismiss()
        }

        //Confirm
        reorderableConfirm.setOnClickListener {
            onConfirm.invoke()
            dialog.dismiss()
        }
    }

    override fun onInitEnd() {
        //Init list
        reorderableList.layoutManager = LinearLayoutManager(context)
        reorderableList.adapter = adapter
        reorderableList.addItemDecoration(ListSeparator(3))

        //Attach drag helper
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            if (canSwipeToRemove) 0 else ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT //0 = disabled
        ) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                //Get positions
                val fromPosition = viewHolder.bindingAdapterPosition
                val toPosition = target.bindingAdapterPosition

                //Swap items
                Collections.swap(items, fromPosition, toPosition)
                adapter.notifyItemMoved(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                //Get position
                val position = viewHolder.bindingAdapterPosition

                //Remove item
                items.removeAt(position)
                adapter.notifyItemRemoved(position)
            }
        }).attachToRecyclerView(reorderableList)
    }

}