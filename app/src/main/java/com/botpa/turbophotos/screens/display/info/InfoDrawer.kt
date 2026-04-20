package com.botpa.turbophotos.screens.display.info

import android.content.Context
import android.text.format.DateFormat
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.TextView
import android.widget.Toast
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Item
import com.botpa.turbophotos.gallery.modals.core.CustomDrawer
import com.botpa.turbophotos.util.Orion
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.round

class InfoDrawer(
    context: Context,
    private val item: Item
) : CustomDrawer(context, R.layout.drawer_display_info) {

    //Views (info)
    private lateinit var infoLayout: View
    private lateinit var infoName: TextView
    private lateinit var infoPath: TextView
    private lateinit var infoDate: TextView
    private lateinit var infoSize: TextView
    private lateinit var infoCaption: TextView
    private lateinit var infoLabelsScroll: HorizontalScrollView
    private lateinit var infoLabels: TextView
    private lateinit var infoTextScroll: HorizontalScrollView
    private lateinit var infoText: TextView
    private lateinit var infoClose: View
    private lateinit var infoEdit: View

    //Views (edit)
    private lateinit var editLayout: View
    private lateinit var editCaption: TextView
    private lateinit var editLabels: TextView
    private lateinit var editCancel: View
    private lateinit var editSave: View


    //Init
    override fun initViews() {
        //Info
        infoLayout = root.findViewById(R.id.infoLayout)

        //Info (file)
        infoName = root.findViewById(R.id.infoName)
        infoPath = root.findViewById(R.id.infoPath)
        infoDate = root.findViewById(R.id.infoDate)
        infoSize = root.findViewById(R.id.infoSize)

        //Info (search)
        infoCaption = root.findViewById(R.id.infoCaption)
        infoLabelsScroll = root.findViewById(R.id.infoLabelsScroll)
        infoLabels = root.findViewById(R.id.infoLabels)
        infoTextScroll = root.findViewById(R.id.infoTextScroll)
        infoText = root.findViewById(R.id.infoText)
        infoClose = root.findViewById(R.id.infoClose)
        infoEdit = root.findViewById(R.id.infoEdit)

        //Edit
        editLayout = root.findViewById(R.id.editLayout)
        editCaption = root.findViewById(R.id.editCaption)
        editLabels = root.findViewById(R.id.editLabels)
        editCancel = root.findViewById(R.id.editCancel)
        editSave = root.findViewById(R.id.editSave)
    }

    override fun initListeners() {
        //Info
        infoClose.setOnClickListener { view -> dialog.cancel() }

        infoEdit.setOnClickListener { view: View ->
            //No metadata file
            if (!item.album.hasMetadata()) {
                Toast.makeText(
                    context,
                    "This item's album does not have a metadata file linked to it",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            //No metadata
            if (!item.hasMetadata()) {
                Toast.makeText(
                    context,
                    "This item does not have a key in its album metadata",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            //Update edit info
            editCaption.text = infoCaption.text
            editLabels.text = infoLabels.text

            //Hide info & show edit
            infoLayout.visibility = View.GONE
            editLayout.visibility = View.VISIBLE
        }

        //Edit
        editCancel.setOnClickListener { view ->
            //Show info & hide edit
            infoLayout.visibility = View.VISIBLE
            editLayout.visibility = View.GONE
        }

        editSave.setOnClickListener { view: View ->
            //Get new caption & labels
            val caption = editCaption.text.toString()
            val labels = editLabels.text.toString()
            val labelsArray: Array<String> = labels.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (i in labelsArray.indices) labelsArray[i] = labelsArray[i].trim { it <= ' ' }

            //Update info texts with new ones
            infoCaption.text = caption
            infoLabels.text = labels

            //Update metadata
            val key = item.name
            val hasMetadata = item.hasMetadata()
            val metadata = item.getMetadata() ?: Orion.emptyJson
            if (!hasMetadata) item.album.metadata!!.set<JsonNode>(key, metadata)
            metadata.put("caption", caption)
            metadata.set<JsonNode>("labels", Orion.arrayToJson(labelsArray))

            //Save
            val saved = item.album.saveMetadata()
            Toast.makeText(
                context,
                if (saved) "Saved successfully" else "An error occurred while saving",
                Toast.LENGTH_SHORT
            ).show()

            //Close menu
            dialog.cancel()
        }
    }

    override fun onInitEnd() {
        //Get file info
        val path = item.file.parent ?: ""
        val date = Date(item.lastModified * 1000)
        val dateFormatter = SimpleDateFormat(if (DateFormat.is24HourFormat(context)) "dd/MM/yyyy, HH:mm.ss" else "dd/MM/yyyy, hh:mm.ss a", Locale.ENGLISH)
        val size = round(item.size.toFloat() / 10) / 100

        //Update file info
        infoName.text = item.name
        infoPath.text = path
        infoDate.text = dateFormatter.format(date)
        infoSize.text = if (size > 1000) "${round(size / 10) / 100} MB" else "$size KB"

        //Get search info (caption, labels & text)
        var caption: String? = ""
        var labels = ""
        var text = ""
        try {
            //Get metadata
            val metadata: ObjectNode = item.getMetadata() ?: throw Exception()

            //Load caption
            caption = metadata.path("caption").asText()

            //Add labels
            var info = StringBuilder()
            if (metadata.has("labels")) {
                //Get labels array
                val array = metadata.path("labels")

                //Get array max & append all labels to info
                val arrayMax = array.size() - 1
                if (arrayMax >= 0 && info.isNotEmpty()) info.append("\n\n")
                for (i in 0..arrayMax) {
                    info.append(array.get(i).asText())
                    if (i != arrayMax) info.append(", ")
                }
            }
            labels = info.toString()

            //Add text
            info = StringBuilder()
            if (metadata.has("text")) {
                //Get labels array
                val array = metadata.path("text")

                //Get array max & append all labels to info
                val arrayMax = array.size() - 1
                if (arrayMax >= 0 && info.isNotEmpty()) info.append("\n\n")
                for (i in 0..arrayMax) {
                    info.append(array.get(i).asText())
                    if (i != arrayMax) info.append(", ")
                }
            }
            text = info.toString()
        } catch (_: Exception) {
            //Error while parsing JSON
        }

        //Update search info
        infoCaption.text = caption
        infoLabels.text = labels
        infoText.text = text
    }

}