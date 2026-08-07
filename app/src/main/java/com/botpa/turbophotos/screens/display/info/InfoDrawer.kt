package com.botpa.turbophotos.screens.display.info

import android.app.Activity
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.text.format.DateFormat
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Item
import com.botpa.turbophotos.gallery.LocalGeocoder
import com.botpa.turbophotos.gallery.modals.core.CustomDrawer
import com.botpa.turbophotos.gallery.views.ListSeparator
import com.botpa.turbophotos.util.Orion
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.round

class InfoDrawer(
    private val activity: Activity,
    private val item: Item
) : CustomDrawer(activity, R.layout.drawer_display_info) {

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
                Toast.makeText(context, R.string.drawer_edit_error_missing_metadata, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //Update edit info
            for (i in infoSearchItems.indices) {
                val item = infoSearchItems[i]
                when (item.name) {
                    R.string.drawer_info_search_caption -> {
                        editCaption.text = item.info
                    }
                    R.string.drawer_info_search_labels -> {
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

            //Get metadata info
            val key = item.name
            val metadata = item.getMetadata() ?: Orion.emptyJson
            val hasMetadata = item.hasMetadata()

            //Update metadata key
            metadata.put("caption", caption)
            metadata.set<JsonNode>("labels", Orion.arrayToJson(labelsArray))
            if (!hasMetadata) item.album.metadata!!.set<JsonNode>(key, metadata)

            //Save
            val saved = item.album.saveMetadata()
            Toast.makeText(
                context,
                if (saved) R.string.drawer_edit_message_save else R.string.drawer_edit_error_save,
                Toast.LENGTH_SHORT
            ).show()

            //Close menu
            dialog.cancel()
        }
    }

    override fun onInitEnd() {
        //Toggle edit button
        infoEdit.isEnabled = item.album.hasMetadata()

        //Load info
        loadInfo(ExifInterface(item.file.absolutePath))
    }

    //Helpers
    fun loadInfo(exif: ExifInterface) {
        //Get info (file)
        val path = item.file.parent ?: ""
        val date = Date(item.lastModified * 1000)
        val dateFormatter = SimpleDateFormat(if (DateFormat.is24HourFormat(context)) "dd/MM/yyyy, HH:mm.ss" else "dd/MM/yyyy, hh:mm.ss a", Locale.ENGLISH)
        val size = round(item.size.toFloat() / 10) / 100
        val resolution = getItemResolution(item, exif)
        val location = exif.latLong

        //Create items list (file)
        infoFileItems.add(Info(R.string.drawer_info_file_name, item.name))
        infoFileItems.add(Info(R.string.drawer_info_file_path, path))
        infoFileItems.add(Info(R.string.drawer_info_file_date, dateFormatter.format(date)))
        infoFileItems.add(Info(R.string.drawer_info_file_size, if (size > 1000) "${round(size / 10) / 100} MB" else "$size KB"))
        if (resolution.first > 0 && resolution.second > 0) {
            infoFileItems.add(Info(R.string.drawer_info_file_resolution, "${resolution.first}x${resolution.second}"))
        }
        if (location != null) {
            //Get location info
            val latitude = location[0]
            val latitudeText = String.format(Locale.getDefault(), "%.4f", latitude)
            val longitude = location[1]
            val longitudeText = String.format(Locale.getDefault(), "%.4f", longitude)
            val locationCoordinates = "${latitudeText}º N, ${longitudeText}º W"
            val item = Info(R.string.drawer_info_file_location, "${context.getString(R.string.drawer_info_file_location_finding)}\n$locationCoordinates")
            infoFileItems.add(item)

            //Async load location name
            Thread {
                val cityLabel = LocalGeocoder.getInstance(context).getLocationLabel(latitude, longitude, maxCityDistanceKm = 30.0)
                activity.runOnUiThread {
                    synchronized(this) {
                        item.info = "$cityLabel\n$locationCoordinates"
                        infoFileList.adapter?.notifyItemChanged(infoFileItems.indexOf(item))
                    }
                }
            }.start()
        }

        //Init list (file)
        initList(infoFileLayout, infoFileList, infoFileItems)

        //Get info (camera)
        val cameraBrand = exif.getAttribute(ExifInterface.TAG_MAKE)
        val cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL)
        val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
        val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
        val shutterSpeed = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)

        //Create items list (camera)
        if (cameraBrand != null) {
            infoCameraItems.add(Info(R.string.drawer_info_camera_brand, cameraBrand))
        }
        if (cameraModel != null) {
            infoCameraItems.add(Info(R.string.drawer_info_camera_model, cameraModel))
        }
        if (iso != null) {
            infoCameraItems.add(Info(R.string.drawer_info_camera_iso, iso))
        }
        if (aperture != null) {
            infoCameraItems.add(Info(R.string.drawer_info_camera_aperture, aperture))
        }
        if (shutterSpeed != null) {
            infoCameraItems.add(Info(R.string.drawer_info_camera_shutter, shutterSpeed))
        }

        //Init list (camera)
        initList(infoCameraLayout, infoCameraList, infoCameraItems)

        //Get info (search metadata)
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

        //Create items list (search metadata)
        if (caption.isNotEmpty()) {
            infoSearchItems.add(Info(R.string.drawer_info_search_caption, caption))
        }
        if (labels.isNotEmpty()) {
            infoSearchItems.add(Info(R.string.drawer_info_search_labels, labels))
        }
        if (text.isNotEmpty()) {
            infoSearchItems.add(Info(R.string.drawer_info_search_text, text))
        }

        //Init list (search metadata)
        initList(infoSearchLayout, infoSearchList, infoSearchItems)
    }

    fun initList(layout: View, list: RecyclerView, items: List<Info>) {
        synchronized(this) {
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

    fun getItemResolution(item: Item, exif: ExifInterface): Pair<Int, Int> {
        //Get info from exif
        try {
            val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
            val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
            if (width > 0 && height > 0) {
                return Pair(width, height)
            }
        } catch (e: Exception) {
            //File has no exif data
        }

        //Exif failed -> Use alternative methods
        return if (item.isVideo) {
            getVideoResolution(item)
        } else {
            getImageResolution(item)
        }
    }

    fun getImageResolution(item: Item): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(item.file.absolutePath, options)
        return Pair(options.outWidth, options.outHeight)
    }

    fun getVideoResolution(item: Item): Pair<Int, Int> {
        val retriever = MediaMetadataRetriever()
        try {
            //Read file
            FileInputStream(item.file).use { inputStream ->
                retriever.setDataSource(inputStream.fd)

                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0

                //Check rotation metadata
                val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                return if (rotation == 90 || rotation == 270) {
                    Pair(height, width)
                } else {
                    Pair(width, height)
                }
            }
        } catch (e: Exception) {
            //Failed
        } finally {
            retriever.release()
        }
        return Pair(0, 0)
    }

}