package com.botpa.turbophotos.backup

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R

class LogAdapter(private val context: Context, private val logs: List<String>) : RecyclerView.Adapter<LogAdapter.LogHolder>() {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): LogHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.log_item, viewGroup, false)
        return LogHolder(view)
    }

    override fun onBindViewHolder(holder: LogHolder, i: Int) {
        //Get holder position
        val position = holder.bindingAdapterPosition

        //Load log from logs
        holder.log.text = logs[position]
    }

    override fun getItemCount(): Int {
        return logs.size
    }

    //Holder
    class LogHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var log: TextView = itemView.findViewById(R.id.log)

    }

}
