package com.botpa.turbophotos.screens.video.tracks

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.modals.core.SimpleCustomAdapter
import com.botpa.turbophotos.screens.video.MediaTrackInfo

class TracksDialogAdapter(context: Context, items: List<MediaTrackInfo>) : SimpleCustomAdapter<MediaTrackInfo, TracksDialogAdapter.TrackHolder>(context, items) {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): TrackHolder {
        return TrackHolder(inflateView(context, R.layout.dialog_tracks_item, viewGroup))
    }

    override fun onInitItemHolder(holder: TrackHolder, item: MediaTrackInfo) {
        //Update info
        holder.selected.visibility = if (item.isSelected) View.VISIBLE else View.GONE
        holder.name.text = item.name

        //Add listeners
        holder.item.setOnClickListener { view ->
            onClick?.run(item, holder.bindingAdapterPosition)
        }
    }

    //Holder
    class TrackHolder(root: View) : RecyclerView.ViewHolder(root) {

        val item: View = root
        val selected: View = root.findViewById(R.id.trackSelected)
        val name: TextView = root.findViewById(R.id.trackName)

    }

}