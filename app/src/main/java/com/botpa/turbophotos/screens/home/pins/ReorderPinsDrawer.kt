package com.botpa.turbophotos.screens.home.pins

import android.content.Context
import android.widget.Button
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.data.Album
import com.botpa.turbophotos.gallery.modals.AlbumsDialog
import com.botpa.turbophotos.gallery.modals.core.CustomDrawer
import com.botpa.turbophotos.gallery.views.lists.ListSeparator
import java.util.Collections

class ReorderPinsDrawer(
    context: Context,
    adapter: PinsAdapter,
    private val items: MutableList<Album>,
    private val onItemAdded: (position: Int) -> Unit,
    private val onItemMoved: (fromPosition: Int, toPosition: Int) -> Unit,
    private val onItemRemoved: (position: Int) -> Unit
) : CustomDrawer(context, R.layout.drawer_pins_reorder) {

    //Pins list
    private lateinit var pinsList: RecyclerView

    private val pinsAdapter: PinsAdapter = adapter

    //Add
    private lateinit var addAll: Button
    private lateinit var addTrash: Button
    private lateinit var addFavourites: Button
    private lateinit var addCustom: Button


    //Init
    override fun initViews() {
        //Pins list
        pinsList = root.findViewById(R.id.pinsList)

        //Add
        addAll = root.findViewById(R.id.pinsAddAll)
        addTrash = root.findViewById(R.id.pinsAddTrash)
        addFavourites = root.findViewById(R.id.pinsAddFavourites)
        addCustom = root.findViewById(R.id.pinsAddCustom)
    }

    override fun initListeners() {
        addAll.setOnClickListener {
            pinAlbum(Library.all)
        }

        addTrash.setOnClickListener {
            pinAlbum(Library.trash)
        }

        addFavourites.setOnClickListener {
            pinAlbum(Library.favourites)
        }

        addCustom.setOnClickListener {
            AlbumsDialog(context, Library.albums, { album ->
                pinAlbum(album)
            }).buildAndShow()
        }
    }

    override fun onInitEnd() {
        //Init list
        pinsList.layoutManager = LinearLayoutManager(context)
        pinsList.adapter = pinsAdapter
        pinsList.itemAnimator = null
        pinsList.addItemDecoration(ListSeparator(3))

        //Attach drag helper
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                //Get positions
                val fromPosition = viewHolder.bindingAdapterPosition
                val toPosition = target.bindingAdapterPosition

                //Swap items
                Collections.swap(items, fromPosition, toPosition)
                pinsAdapter.notifyItemMoved(fromPosition, toPosition)
                onItemMoved.invoke(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                //Get position
                val position = viewHolder.bindingAdapterPosition

                //Remove item
                items.removeAt(position)
                pinsAdapter.notifyItemRemoved(position)
                onItemRemoved.invoke(position)

                //Update add buttons state
                updateAddButtons()
            }
        }).attachToRecyclerView(pinsList)

        //Update add buttons state
        updateAddButtons()
    }

    //Util
    fun updateAddButtons() {
        addAll.isEnabled = !items.contains(Library.all)
        addTrash.isEnabled = !items.contains(Library.trash)
        addFavourites.isEnabled = !items.contains(Library.favourites)
    }

    fun pinAlbum(album: Album) {
        //Ignore if already contained
        if (items.contains(album)) return

        //Add item
        val position = 0
        items.add(position, album)
        pinsAdapter.notifyItemInserted(position)
        onItemAdded.invoke(position)

        //Update add buttons state
        updateAddButtons()
    }

}