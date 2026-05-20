package com.botpa.turbophotos.screens.display.info

import android.content.Context
import android.text.format.DateFormat
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Item
import com.botpa.turbophotos.gallery.modals.core.CustomDrawer
import com.botpa.turbophotos.gallery.views.ListSeparator
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
    private lateinit var infoClose: View
    private lateinit var infoEdit: View
    private lateinit var infoFileLayout: View
    private lateinit var infoFileList: RecyclerView
    private lateinit var infoCameraLayout: View
    private lateinit var infoCameraList: RecyclerView
    private lateinit var infoSearchLayout: View
    private lateinit var infoSearchList: RecyclerView

    //Views (edit)
    private lateinit var editLayout: View
    private lateinit var editCaption: TextView
    private lateinit var editLabels: TextView
    private lateinit var editCancel: View
    private lateinit var editSave: View

    //Info
    private val infoFileItems = ArrayList<Info>()
    private val infoCameraItems = ArrayList<Info>()
    private val infoSearchItems = ArrayList<Info>()

    //Animations
    private val animationDuration = 450


    //Init
    override fun initViews() {
        //Info
        infoLayout = root.findViewById(R.id.infoLayout)
        infoClose = root.findViewById(R.id.infoClose)
        infoEdit = root.findViewById(R.id.infoEdit)

        //Info (file)
        infoFileLayout = root.findViewById(R.id.infoFileLayout)
        infoFileList = root.findViewById(R.id.infoFileList)

        //Info (camera)
        infoCameraLayout = root.findViewById(R.id.infoCameraLayout)
        infoCameraList = root.findViewById(R.id.infoCameraList)

        //Info (search)
        infoSearchLayout = root.findViewById(R.id.infoSearchLayout)
        infoSearchList = root.findViewById(R.id.infoSearchList)

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
            for (i in 0 until infoSearchItems.size - 1) {
                val item = infoSearchItems[i]
                when (item.name) {
                    "Caption" -> {
                        editCaption.text = item.info
                    }
                    "Labels" -> {
                        editLabels.text = item.info
                    }
                }
            }

            //Hide info & show edit
            Orion.animateHide(infoLayout, animationDuration) {
                Orion.animateShow(editLayout, animationDuration)
            }
        }

        //Edit
        editCancel.setOnClickListener { view ->
            //Show info & hide edit
            Orion.animateHide(editLayout, animationDuration) {
                Orion.animateShow(infoLayout, animationDuration)

                //Scroll to bottom
                scroll.post {
                    scroll.fullScroll(View.FOCUS_DOWN)
                }
            }
        }

        editSave.setOnClickListener { view: View ->
            //Get new caption & labels
            val caption = editCaption.text.toString()
            val labels = editLabels.text.toString()
            val labelsArray: Array<String> = labels.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (i in labelsArray.indices) labelsArray[i] = labelsArray[i].trim { it <= ' ' }

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
        //Get exif
        val exif = ExifInterface(item.file.absolutePath)

        //Get info (file)
        val path = item.file.parent ?: ""
        val date = Date(item.lastModified * 1000)
        val dateFormatter = SimpleDateFormat(if (DateFormat.is24HourFormat(context)) "dd/MM/yyyy, HH:mm.ss" else "dd/MM/yyyy, hh:mm.ss a", Locale.ENGLISH)
        val size = round(item.size.toFloat() / 10) / 100
        val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
        val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)

        //Create items list (file)
        infoFileItems.add(Info("Name", item.name))
        infoFileItems.add(Info("Path", path))
        infoFileItems.add(Info("Date", dateFormatter.format(date)))
        infoFileItems.add(Info("Size", if (size > 1000) "${round(size / 10) / 100} MB" else "$size KB"))
        if (width * height > 0) {
            infoFileItems.add(Info("Resolution", "${width}x${height}"))
        }

        //Init list (file)
        initList(infoFileLayout, infoFileList, infoFileItems)

        //Get info (search: caption, labels & text)
        var caption = ""
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

        //Create items list (file)
        if (caption.isNotEmpty()) {
            infoSearchItems.add(Info("Caption", caption))
        }
        if (labels.isNotEmpty()) {
            infoSearchItems.add(Info("Labels", labels))
        }
        if (text.isNotEmpty()) {
            infoSearchItems.add(Info("Text", text))
        }

        //Init list (file)
        initList(infoSearchLayout, infoSearchList, infoSearchItems)

        //Get info (camera)
        val cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE)
        val cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL)
        val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
        val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
        val shutterSpeed = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)

        //Create items list (camera)
        if (cameraMake != null) {
            infoCameraItems.add(Info("Brand", cameraMake))
        }
        if (cameraModel != null) {
            infoCameraItems.add(Info("Model", cameraModel))
        }
        if (iso != null) {
            infoCameraItems.add(Info("ISO", iso))
        }
        if (aperture != null) {
            infoCameraItems.add(Info("Aperture", aperture))
        }
        if (shutterSpeed != null) {
            infoCameraItems.add(Info("Shutter Speed", shutterSpeed))
        }

        //Init list (camera)
        initList(infoCameraLayout, infoCameraList, infoCameraItems)
    }

    fun initList(layout: View, list: RecyclerView, items: List<Info>) {
        if (items.isEmpty()) {
            //Empty -> Hide list
            layout.visibility = View.GONE
        } else {
            //Has items -> Init list
            list.adapter = InfoAdapter(context, items)
            list.layoutManager = LinearLayoutManager(context)
            list.addItemDecoration(ListSeparator(3))
        }
    }

}