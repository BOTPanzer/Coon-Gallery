package com.botpa.turbophotos.backup

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R

class UserAdapter(private val context: Context, private val users: List<User>) : RecyclerView.Adapter<UserAdapter.AlbumHolder>() {

    private var onClickListener: OnClickListener? = null
    private var onLongClickListener: OnClickListener? = null
    private var onDeleteListener: OnClickListener? = null


    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): AlbumHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.connect_item, viewGroup, false)
        return AlbumHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumHolder, i: Int) {
        //Get holder position
        val position = holder.bindingAdapterPosition

        //Get user from users
        val user = users[position]

        //Load album folder & metadata file names
        holder.name.text = user.name
        holder.url.text = user.URL

        //Add listeners
        holder.background.setOnClickListener { view: View ->
            onClickListener?.run(view, holder.bindingAdapterPosition)
        }

        holder.background.setOnLongClickListener { view: View ->
            onLongClickListener?.run(view, holder.bindingAdapterPosition)
            true
        }

        holder.delete.setOnClickListener { view: View ->
            onDeleteListener?.run(view, holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount(): Int {
        return users.size
    }

    //Listeners
    fun interface OnClickListener {
        fun run(view: View, index: Int)
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    fun setOnLongClickListener(onLongClickListener: OnClickListener) {
        this.onLongClickListener = onLongClickListener
    }

    fun setOnDeleteListener(onDeleteListener: OnClickListener) {
        this.onDeleteListener = onDeleteListener
    }

    //Holder
    class AlbumHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var background: View = itemView.findViewById(R.id.background)
        var name: TextView = itemView.findViewById(R.id.name)
        var url: TextView = itemView.findViewById(R.id.url)
        var delete: View = itemView.findViewById(R.id.delete)
    }
}
