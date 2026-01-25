package com.botpa.turbophotos.screens.display

import android.annotation.SuppressLint
import android.app.ComponentCaller
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper

import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Album
import com.botpa.turbophotos.gallery.CoonItem
import com.botpa.turbophotos.gallery.GalleryActivity
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.Library.ActionEvent
import com.botpa.turbophotos.gallery.actions.Action
import com.botpa.turbophotos.gallery.options.OptionsAdapter
import com.botpa.turbophotos.gallery.options.OptionsItem
import com.botpa.turbophotos.gallery.views.ZoomableLayout
import com.botpa.turbophotos.util.BackManager
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Orion.ResizeHeightAnimation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("SetTextI18n", "NotifyDataSetChanged")
class DisplayActivity : GalleryActivity() {

     /*$$$$$$  /$$                     /$$
    | $$__  $$|__/                    | $$
    | $$  \ $$ /$$  /$$$$$$$  /$$$$$$ | $$  /$$$$$$  /$$   /$$
    | $$  | $$| $$ /$$_____/ /$$__  $$| $$ |____  $$| $$  | $$
    | $$  | $$| $$|  $$$$$$ | $$  \ $$| $$  /$$$$$$$| $$  | $$
    | $$  | $$| $$ \____  $$| $$  | $$| $$ /$$__  $$| $$  | $$
    | $$$$$$$/| $$ /$$$$$$$/| $$$$$$$/| $$|  $$$$$$$|  $$$$$$$
    |_______/ |__/|_______/ | $$____/ |__/ \_______/ \____  $$
                            | $$                     /$$  | $$
                            | $$                    |  $$$$$$/
                            |__/                     \_____*/

    //Activity
    private lateinit var backManager: BackManager
    private var isInit = false
    private var isViewingExternal: Boolean = false

    //Events
    private val onAction = ActionEvent { action -> this.manageAction(action) }

    //List
    private lateinit var displayLayoutManager: DisplayLayoutManager
    private lateinit var displayAdapter: DisplayAdapter

    private lateinit var displayGallery: List<CoonItem>
    private var currentIndexInGallery = -1

    private val displayItems: MutableList<CoonItem> = ArrayList()
    private var currentIndexInDisplay = -1

    private lateinit var currentItem: CoonItem

    private lateinit var displayList: RecyclerView

      /*$$$$$              /$$     /$$
     /$$__  $$            | $$    |__/
    | $$  \ $$  /$$$$$$  /$$$$$$   /$$  /$$$$$$  /$$$$$$$   /$$$$$$$
    | $$  | $$ /$$__  $$|_  $$_/  | $$ /$$__  $$| $$__  $$ /$$_____/
    | $$  | $$| $$  \ $$  | $$    | $$| $$  \ $$| $$  \ $$|  $$$$$$
    | $$  | $$| $$  | $$  | $$ /$$| $$| $$  | $$| $$  | $$ \____  $$
    |  $$$$$$/| $$$$$$$/  |  $$$$/| $$|  $$$$$$/| $$  | $$ /$$$$$$$/
     \______/ | $$____/    \___/  |__/ \______/ |__/  |__/|_______/
              | $$
              | $$
              |_*/

    private val options: MutableList<OptionsItem> = ArrayList()
    private lateinit var optionsAdapter: OptionsAdapter

    private val optionSeparator: OptionsItem = OptionsItem()
    private lateinit var optionRename: OptionsItem
    private lateinit var optionEdit: OptionsItem
    private lateinit var optionShare: OptionsItem
    private lateinit var optionMove: OptionsItem
    private lateinit var optionCopy: OptionsItem
    private lateinit var optionTrash: OptionsItem
    private lateinit var optionRestore: OptionsItem
    private lateinit var optionDelete: OptionsItem

    private lateinit var optionsLayout: View
    private lateinit var optionsList: RecyclerView

      /*$$$$$    /$$     /$$
     /$$__  $$  | $$    | $$
    | $$  \ $$ /$$$$$$  | $$$$$$$   /$$$$$$   /$$$$$$
    | $$  | $$|_  $$_/  | $$__  $$ /$$__  $$ /$$__  $$
    | $$  | $$  | $$    | $$  \ $$| $$$$$$$$| $$  \__/
    | $$  | $$  | $$ /$$| $$  | $$| $$_____/| $$
    |  $$$$$$/  |  $$$$/| $$  | $$|  $$$$$$$| $$
     \______/    \___/  |__/  |__/ \_______/|_*/

    //Views (overlay)
    private lateinit var overlayLayout: View
    private lateinit var overlayName: TextView
    private lateinit var overlayInfo: View
    private lateinit var overlayOptions: View

    //Views (info)
    private lateinit var infoLayout: View
    private lateinit var infoName: TextView
    private lateinit var infoDate: TextView
    private lateinit var infoCaption: TextView
    private lateinit var infoLabelsScroll: HorizontalScrollView
    private lateinit var infoLabels: TextView
    private lateinit var infoTextScroll: HorizontalScrollView
    private lateinit var infoText: TextView
    private lateinit var infoEdit: View

    //Views (edit)
    private lateinit var editLayout: View
    private lateinit var editCaption: TextView
    private lateinit var editLabels: TextView
    private lateinit var editSave: View
    private lateinit var editSpace: View



     /*$$$$$$  /$$                     /$$
    | $$__  $$|__/                    | $$
    | $$  \ $$ /$$  /$$$$$$$  /$$$$$$ | $$  /$$$$$$  /$$   /$$
    | $$  | $$| $$ /$$_____/ /$$__  $$| $$ |____  $$| $$  | $$
    | $$  | $$| $$|  $$$$$$ | $$  \ $$| $$  /$$$$$$$| $$  | $$
    | $$  | $$| $$ \____  $$| $$  | $$| $$ /$$__  $$| $$  | $$
    | $$$$$$$/| $$ /$$$$$$$/| $$$$$$$/| $$|  $$$$$$$|  $$$$$$$
    |_______/ |__/|_______/ | $$____/ |__/ \_______/ \____  $$
                            | $$                     /$$  | $$
                            | $$                    |  $$$$$$/
                            |__/                     \_____*/

    //Activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.display_screen)

        //Enable HDR
        window.colorMode = ActivityInfo.COLOR_MODE_HDR

        //Add events
        Library.addOnActionEvent(onAction)

        //Init components
        backManager = BackManager(this@DisplayActivity, onBackPressedDispatcher)
        initViews()
        initListeners()
        initLists()

        //Init activity
        initActivity()
    }

    override fun onDestroy() {
        super.onDestroy()

        //Remove events
        Library.removeOnActionEvent(onAction)
    }

    override fun onResume() {
        super.onResume()

        //Not init
        if (!isInit) return

        //Check if current item was modified
        if (currentItem.updateLastModified()) {
            //Item was modified -> Refresh display & sort library
            displayAdapter.notifyItemChanged(currentIndexInDisplay)
            Library.sortLibrary()
        }
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)

        //Handle intent
        handleIntent(intent)
    }

    private fun initActivity() {
        //Check if intent is valid
        val intent = getIntent()
        if (intent == null) {
            finish()
            return
        }

        //Handle intent
        handleIntent(intent)

        //Mark as init
        isInit = true
    }

    private fun handleIntent(intent: Intent) {
        //Check if intent has data (an external app requested to view a file)
        val uri: Uri? = intent.data
        if (uri != null) {
            //Viewing external file
            isViewingExternal = true

            //Init gallery list
            val gallery: MutableList<CoonItem> = ArrayList()
            displayGallery = gallery

            //Create item from uri & add it to list
            gallery.add(CoonItem.createFromUri(this, uri, Album("Temp")))

            //Select first item
            selectItem(0)
        } else {
            //Viewing gallery item
            isViewingExternal = false

            //Init gallery list
            displayGallery = Library.gallery

            //Check if intent has item index
            val index = intent.getIntExtra("index", -1)
            if (index < 0) {
                finish()
                return
            }
            selectItem(index)
        }
    }

    //Events
    private fun manageAction(action: Action) {
        //No action
        if (action.isOfType(Action.TYPE_NONE)) return

        //Check if gallery is empty
        if (displayGallery.isEmpty()) {
            //Is empty -> Close display
            finish()
            return
        }

        //Update selected item
        val originalCurrentIndex = currentIndexInGallery
        for (indexInGallery in action.removedIndexesInGallery) {
            //Check if current item index changed (an item before it was removed)
            if (indexInGallery < originalCurrentIndex) currentIndexInGallery--
        }
        selectItem(currentIndexInGallery)
    }

    //Components
    private fun initViews() {
        //Views (list)
        displayList = findViewById(R.id.list)

        //Views (overlay)
        overlayLayout = findViewById(R.id.overlayLayout)
        overlayName = findViewById(R.id.overlayName)
        overlayInfo = findViewById(R.id.overlayInfo)
        overlayOptions = findViewById(R.id.overlayOptions)

        //Views (info)
        infoLayout = findViewById(R.id.infoLayout)
        infoName = findViewById(R.id.infoName)
        infoDate = findViewById(R.id.infoDate)
        infoCaption = findViewById(R.id.infoCaption)
        infoLabelsScroll = findViewById(R.id.infoLabelsScroll)
        infoLabels = findViewById(R.id.infoLabels)
        infoTextScroll = findViewById(R.id.infoTextScroll)
        infoText = findViewById(R.id.infoText)
        infoEdit = findViewById(R.id.infoEdit)

        //Views (edit)
        editLayout = findViewById(R.id.editLayout)
        editCaption = findViewById(R.id.editCaption)
        editLabels = findViewById(R.id.editLabels)
        editSave = findViewById(R.id.editSave)
        editSpace = findViewById(R.id.editSpace)

        //Views (options)
        optionsLayout = findViewById(R.id.optionsLayout)
        optionsList = findViewById(R.id.optionsList)


        //Insets (overlay)
        Orion.addInsetsChangedListener(
            findViewById(R.id.overlayIndent),
            intArrayOf(WindowInsetsCompat.Type.systemBars())
        ) { view: View, insets: Insets, percent: Float ->
            //Ignore if no margins
            if (insets.top <= 0 && insets.bottom <= 0) return@addInsetsChangedListener

            //Update margins
            val params = view.layoutParams as MarginLayoutParams
            params.setMargins(insets.left, insets.top, insets.right, insets.bottom)
            view.layoutParams = params
        }

        //Insets (edit info layout)
        val defaultEditSpaceHeight = editSpace.minimumHeight //Height returns 0 when view is not rendered, so we store height in minimumHeight too :D
        ViewCompat.setOnApplyWindowInsetsListener(
            editSpace
        ) { view: View, windowInsets: WindowInsetsCompat ->
            //Get new bottom space height
            val insetsSystemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val insetsKeyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val keyboardOpen = insetsKeyboard.bottom != 0
            val height = if (keyboardOpen) insetsKeyboard.bottom else insetsSystemBars.bottom + defaultEditSpaceHeight

            //Update bottom space height
            if (view.height == 0) {
                //Not rendered yet -> Don't animate
                view.layoutParams.height = height
                view.requestLayout()
            } else {
                //Has height -> Animate
                val resize = ResizeHeightAnimation(editSpace, height)
                resize.duration = 100L
                view.startAnimation(resize)
            }
            windowInsets
        }

        //Insets (options layout)
        Orion.addInsetsChangedListener(
            optionsLayout,
            intArrayOf(WindowInsetsCompat.Type.systemBars())
        )
    }

    private fun initListeners() {
        //Overlay
        overlayInfo.setOnClickListener { toggleInfo(true) }

        overlayOptions.setOnClickListener { toggleOptions(true) }

        //Info
        infoLayout.setOnClickListener { toggleInfo(false) }

        infoEdit.setOnClickListener { view: View ->
            //No metadata file
            if (!currentItem.album.hasMetadata()) {
                Toast.makeText(
                    this@DisplayActivity,
                    "This item's album does not have a metadata file linked to it",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            //No metadata
            if (!currentItem.hasMetadata()) {
                Toast.makeText(
                    this@DisplayActivity,
                    "This item does not have a key in its album metadata",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            //Hide display info
            toggleInfo(false)

            //Toggle display edit
            toggleEdit(editLayout.visibility != View.VISIBLE)
        }

        //Edit
        editLayout.setOnClickListener { toggleEdit(false) }

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
            val key = currentItem.name
            val hasMetadata = currentItem.hasMetadata()
            val metadata = currentItem.getMetadata() ?: Orion.emptyJson
            if (!hasMetadata) currentItem.album.metadata!!.set<JsonNode>(key, metadata)
            metadata.put("caption", caption)
            metadata.set<JsonNode>("labels", Orion.arrayToJson(labelsArray))

            //Save
            val saved = currentItem.album.saveMetadata()
            Toast.makeText(
                this@DisplayActivity,
                if (saved) "Saved successfully" else "An error occurred while saving",
                Toast.LENGTH_SHORT
            ).show()

            //Close menu
            toggleEdit(false)
        }

        //Options
        optionsLayout.setOnClickListener { toggleOptions(false) }

        optionRename = OptionsItem(R.drawable.rename, "Rename") {
            //Rename
            Library.renameItem(this@DisplayActivity, currentItem)
        }

        optionEdit = OptionsItem(R.drawable.edit, "Edit") {
            //Edit
            Library.editItem(this@DisplayActivity, currentItem)
        }

        optionShare = OptionsItem(R.drawable.share, "Share") {
            //Share
            Library.shareItems(this@DisplayActivity, arrayOf(currentItem))
        }

        optionMove = OptionsItem(R.drawable.move, "Move to album") {
            //Move items
            Library.moveItems(this@DisplayActivity, arrayOf(currentItem))
        }

        optionCopy = OptionsItem(R.drawable.copy, "Copy to album") {
            //Copy items
            Library.copyItems(this@DisplayActivity, arrayOf(currentItem))
        }

        optionTrash = OptionsItem(R.drawable.trash, "Move to trash") {
            //Move to trash
            trashItems(arrayOf(currentItem))
        }

        optionRestore = OptionsItem(R.drawable.restore, "Restore") {
            //Restore from trash
            restoreItems(arrayOf(currentItem))
        }

        optionDelete = OptionsItem(R.drawable.delete, "Delete") {
            //Delete item
            Library.deleteItems(this@DisplayActivity, arrayOf(currentItem))
        }
    }

    private fun initLists() {
        //Init display layout manager
        displayLayoutManager = DisplayLayoutManager(this@DisplayActivity)
        displayLayoutManager.setOrientation(RecyclerView.HORIZONTAL)
        displayList.setLayoutManager(displayLayoutManager)

        //Init display adapter
        displayAdapter = DisplayAdapter(this@DisplayActivity, displayItems)
        displayList.setAdapter(displayAdapter)

        //Add adapter listeners
        displayAdapter.setOnClickListener { zoom: ZoomableLayout, image: ImageView, index: Int ->
            if (overlayLayout.isVisible) {
                Orion.hideAnim(overlayLayout)
                toggleSystemUI(false)
            } else {
                Orion.showAnim(overlayLayout)
                toggleSystemUI(true)
            }
        }
        displayAdapter.setOnZoomChangedListener { zoom: ZoomableLayout, image: ImageView, index: Int ->
            //Enable scrolling only if not zoomed and one finger is over
            displayLayoutManager.setScrollEnabled(zoom.zoom <= 1 && zoom.pointers <= 1)
        }
        displayAdapter.setOnPointersChangedListener { zoom: ZoomableLayout, image: ImageView, index: Int ->
            //Enable scrolling only if not zoomed and one finger is over
            displayLayoutManager.setScrollEnabled(zoom.zoom <= 1 && zoom.pointers <= 1)
        }
        displayAdapter.setOnPlayListener { zoom: ZoomableLayout, image: ImageView, index: Int ->
            //Play video outside
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(currentItem.file.absolutePath.toUri(), currentItem.mimeType)
            startActivity(intent)
        }

        //Create snap helper
        val snapHelper: SnapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(displayList)

        //Add snap helper listener
        displayList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE && displayLayoutManager.canScrollHorizontally()) {
                    //Get position
                    val view = snapHelper.findSnapView(displayLayoutManager)
                    val position = if (view != null) displayLayoutManager.getPosition(view) else -1
                    if (position == -1) return

                    //Check what to do
                    if (position < currentIndexInDisplay) {
                        //Previous
                        displayLayoutManager.setScrollEnabled(false)
                        selectItem(currentIndexInGallery - 1)
                    } else if (position > currentIndexInDisplay) {
                        //Next
                        displayLayoutManager.setScrollEnabled(false)
                        selectItem(currentIndexInGallery + 1)
                    }
                }
                super.onScrollStateChanged(recyclerView, newState)
            }
        })

        //Init options layout manager
        optionsList.setLayoutManager(LinearLayoutManager(this@DisplayActivity))

        //Init options adapter
        optionsAdapter = OptionsAdapter(this@DisplayActivity, options)
        optionsAdapter.setOnClickListener { view: View, index: Int ->
            //Get option
            val option = options[index]

            //Get action
            val action = option.action ?: return@setOnClickListener

            //Invoke action
            action.run()
            toggleOptions(false)
        }
        optionsList.setAdapter(optionsAdapter)
    }

    //Current item
    private fun selectItem(index: Int) {
        //Empty display gallery
        if (displayGallery.isEmpty()) {
            finish()
            return
        }

        //Fix index overflow
        var index = index
        index = Math.clamp(index.toLong(), 0, displayGallery.size - 1)

        //Reset display items
        displayItems.clear()
        currentIndexInGallery = index
        currentIndexInDisplay = 0

        //Add items to display list
        if (index > 0) {
            //Has item before -> Add it
            displayItems.add(displayGallery[index - 1])
            currentIndexInDisplay++
        }
        displayItems.add(displayGallery[index])
        if (index < displayGallery.size - 1) {
            //Has item after -> Add it
            displayItems.add(displayGallery[index + 1])
        }

        //Get current image, update adapter & select it
        currentItem = displayItems[currentIndexInDisplay]
        displayAdapter.notifyDataSetChanged()
        displayList.scrollToPosition(currentIndexInDisplay)
        displayLayoutManager.setScrollEnabled(true)

        //Change image name
        overlayName.text = currentItem.name

        //Create date text
        val date = Date(currentItem.lastModified * 1000)
        val formatter1 = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        val formatter2 = SimpleDateFormat("hh:mm.ss a", Locale.ENGLISH)

        //Load image info (caption & labels)
        var caption: String? = ""
        var labels = ""
        var text = ""
        try {
            //Get metadata
            val metadata: ObjectNode = currentItem.getMetadata() ?: throw Exception()

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

        //Update text
        infoName.text = currentItem.name
        infoDate.text = "${formatter1.format(date)}, ${formatter2.format(date)}"
        infoCaption.text = caption
        infoLabels.text = labels
        infoText.text = text
    }

      /*$$$$$              /$$     /$$
     /$$__  $$            | $$    |__/
    | $$  \ $$  /$$$$$$  /$$$$$$   /$$  /$$$$$$  /$$$$$$$   /$$$$$$$
    | $$  | $$ /$$__  $$|_  $$_/  | $$ /$$__  $$| $$__  $$ /$$_____/
    | $$  | $$| $$  \ $$  | $$    | $$| $$  \ $$| $$  \ $$|  $$$$$$
    | $$  | $$| $$  | $$  | $$ /$$| $$| $$  | $$| $$  | $$ \____  $$
    |  $$$$$$/| $$$$$$$/  |  $$$$/| $$|  $$$$$$/| $$  | $$ /$$$$$$$/
     \______/ | $$____/    \___/  |__/ \______/ |__/  |__/|_______/
              | $$
              | $$
              |_*/

    private fun toggleOptions(show: Boolean) {
        if (show) {
            //Get state info
            val isTrashed = currentItem.isTrashed

            //Update options list
            options.clear()
            if (isViewingExternal) {
                //Viewing external file
                if (!isTrashed) {
                    options.add(optionEdit)
                    options.add(optionShare)
                }
            } else {
                //Viewing gallery items
                if (!isTrashed) {
                    options.add(optionRename)
                    options.add(optionEdit)
                    options.add(optionShare)
                    options.add(optionMove)
                    options.add(optionCopy)
                    options.add(optionSeparator)
                    options.add(optionTrash)
                } else {
                    options.add(optionRestore)
                }
                options.add(optionDelete)
            }
            optionsAdapter.notifyDataSetChanged()

            //Show
            Orion.showAnim(optionsLayout)
            backManager.register("options") { toggleOptions(false) }
        } else {
            //Hide
            Orion.hideAnim(optionsLayout)
            backManager.unregister("options")
        }
    }

      /*$$$$$    /$$     /$$
     /$$__  $$  | $$    | $$
    | $$  \ $$ /$$$$$$  | $$$$$$$   /$$$$$$   /$$$$$$
    | $$  | $$|_  $$_/  | $$__  $$ /$$__  $$ /$$__  $$
    | $$  | $$  | $$    | $$  \ $$| $$$$$$$$| $$  \__/
    | $$  | $$  | $$ /$$| $$  | $$| $$_____/| $$
    |  $$$$$$/  |  $$$$/| $$  | $$|  $$$$$$$| $$
     \______/    \___/  |__/  |__/ \_______/|_*/

    //Menus
    private fun toggleInfo(show: Boolean) {
        if (show) {
            //Scroll info to start
            infoLabelsScroll.scrollTo(0, 0)
            infoTextScroll.scrollTo(0, 0)

            //Show
            Orion.showAnim(infoLayout)
            backManager.register("info") { toggleInfo(false) }
        } else {
            //Hide
            Orion.hideAnim(infoLayout)
            backManager.unregister("info")
        }
    }

    private fun toggleEdit(show: Boolean) {
        if (show) {
            //Update edit texts
            editCaption.text = infoCaption.text
            editLabels.text = infoLabels.text

            //Show
            Orion.showAnim(editLayout)
            backManager.register("edit") { toggleEdit(false) }
        } else {
            //Hide keyboard
            Orion.hideKeyboard(this@DisplayActivity)
            Orion.clearFocus(this@DisplayActivity)

            //Hide
            Orion.hideAnim(editLayout)
            backManager.unregister("edit")
        }
    }

    //Util
    private fun toggleSystemUI(show: Boolean) {
        //Get controller
        val controller = WindowCompat.getInsetsController(window, overlayLayout)

        //Toggle system UI
        if (show) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

}
