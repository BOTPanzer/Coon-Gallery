package com.botpa.turbophotos.screens.display

import android.Manifest
import android.annotation.SuppressLint
import android.app.ComponentCaller
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Rational
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Album
import com.botpa.turbophotos.gallery.BaseActivity
import com.botpa.turbophotos.gallery.Item
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.Library.ActionEvent
import com.botpa.turbophotos.gallery.StoragePairs
import com.botpa.turbophotos.gallery.actions.Action
import com.botpa.turbophotos.gallery.options.OptionsGroup
import com.botpa.turbophotos.gallery.options.OptionsItem
import com.botpa.turbophotos.gallery.options.OptionsManager
import com.botpa.turbophotos.gallery.views.ZoomableLayout
import com.botpa.turbophotos.screens.display.info.InfoDrawer
import com.botpa.turbophotos.screens.video.VideoActivity
import com.botpa.turbophotos.util.BackManager
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage

@SuppressLint("SetTextI18n", "NotifyDataSetChanged")
class DisplayActivity : BaseActivity() {

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
    private var isInPiP: Boolean = false

    //Events
    private val onAction = ActionEvent { action -> this.manageAction(action) }

    //List
    private lateinit var displayLayoutManager: DisplayLayoutManager
    private lateinit var displayAdapter: DisplayAdapter

    private lateinit var displayGallery: List<Item>
    private var currentIndexInGallery = -1

    private val displayItems: MutableList<Item> = ArrayList()
    private var currentIndexInDisplay = -1

    private var useInternalVideoPlayer: Boolean = true

    private lateinit var currentItem: Item

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

    private val options: MutableList<OptionsGroup> = ArrayList()
    private lateinit var optionsManager: OptionsManager

    private lateinit var optionRename: OptionsItem
    private lateinit var optionEdit: OptionsItem
    private lateinit var optionShare: OptionsItem
    private lateinit var optionSetAs: OptionsItem
    private lateinit var optionPiP: OptionsItem
    private lateinit var optionFavourite: OptionsItem
    private lateinit var optionUnfavourite: OptionsItem
    private lateinit var optionMove: OptionsItem
    private lateinit var optionCopy: OptionsItem
    private lateinit var optionTrash: OptionsItem
    private lateinit var optionRestore: OptionsItem
    private lateinit var optionDelete: OptionsItem

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
    private lateinit var overlayTitle: TextView
    private lateinit var overlayFavourite: View
    private lateinit var overlayInfo: View
    private lateinit var overlayOptions: View



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
        backManager = BackManager(this, onBackPressedDispatcher)
        optionsManager = OptionsManager(this, options, backManager) { onUpdateOptions() }
        Storage.init(this) //Init storage cause activity is exported
        initViews()
        initListeners()
        initDisplayList()

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

        //Update settings
        useInternalVideoPlayer = Storage.getBool(StoragePairs.VIDEO_USE_INTERNAL_PLAYER)

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

    private fun hasPermissions(): Boolean {
        //External storage
        if (!Environment.isExternalStorageManager()) {
            return false
        }

        //Media
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        //All good
        return true
    }

    private fun initActivity() {
        //Check for permissions
        if (!hasPermissions()) {
            Toast.makeText(this, "Missing permissions.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

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
            val gallery: MutableList<Item> = ArrayList()
            displayGallery = gallery

            //Create item from uri & add it to list
            gallery.add(Item.createFromUri(this, uri, Album("Temp")))

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

    //Views
    private fun initViews() {
        //Views (list)
        displayList = findViewById(R.id.list)

        //Views (overlay)
        overlayLayout = findViewById(R.id.overlayLayout)
        overlayTitle = findViewById(R.id.overlayTitle)
        overlayFavourite = findViewById(R.id.overlayFavourite)
        overlayInfo = findViewById(R.id.overlayInfo)
        overlayOptions = findViewById(R.id.overlayOptions)


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

        //Insets (options layout)
        Orion.addInsetsChangedListener(
            optionsManager.layout,
            intArrayOf(WindowInsetsCompat.Type.systemBars())
        )
    }

    private fun initListeners() {
        //Overlay
        overlayInfo.setOnClickListener {
            InfoDrawer(this, currentItem).buildAndShow()
        }

        overlayOptions.setOnClickListener { optionsManager.toggle(true) }

        //Options
        optionsManager.layout.setOnClickListener { optionsManager.toggle(false) }

        optionRename = OptionsItem(R.drawable.rename, "Rename") {
            //Rename
            Library.renameItem(this, currentItem)
        }

        optionEdit = OptionsItem(R.drawable.edit, "Edit") {
            //Edit
            Library.editItem(this, currentItem)
        }

        optionShare = OptionsItem(R.drawable.share, "Share") {
            //Share
            Library.shareItems(this, arrayOf(currentItem))
        }

        optionSetAs = OptionsItem(R.drawable.wallpaper, "Set as") {
            //Set as
            Library.setItemAs(this, currentItem)
        }

        optionPiP = OptionsItem(R.drawable.pip, "Open in PiP") {
            //Create params
            val p = PictureInPictureParams.Builder()
            try {
                val image = (displayList.findViewHolderForAdapterPosition(currentIndexInDisplay) as DisplayAdapter.ItemHolder).image
                p.setAspectRatio(Rational(image.width, image.height))
            } catch (_: Exception) {
                p.setAspectRatio(Rational(9, 16))
            }

            //Enter PiP
            isInPiP = enterPictureInPictureMode(p.build())
        }

        optionFavourite = OptionsItem(R.drawable.favourite_on, "Favourite") {
            //Add to favourites
            favouriteItems(arrayOf(currentItem))
        }

        optionUnfavourite = OptionsItem(R.drawable.favourite_off, "Unfavourite") {
            //Remove from favourites
            unfavouriteItems(arrayOf(currentItem))
        }

        optionMove = OptionsItem(R.drawable.move, "Move to album") {
            //Move items
            Library.moveItems(this, arrayOf(currentItem))
        }

        optionCopy = OptionsItem(R.drawable.copy, "Copy to album") {
            //Copy items
            Library.copyItems(this, arrayOf(currentItem))
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
            Library.deleteItems(this, arrayOf(currentItem))
        }
    }

    //Display
    private fun initDisplayList() {
        //Init display layout manager
        displayLayoutManager = DisplayLayoutManager(this)
        displayLayoutManager.setOrientation(RecyclerView.HORIZONTAL)
        displayList.setLayoutManager(displayLayoutManager)

        //Init display adapter
        displayAdapter = DisplayAdapter(this, displayItems)
        displayList.setAdapter(displayAdapter)

        //Add adapter listeners
        displayAdapter.onClick = { zoom: ZoomableLayout, image: ImageView, position: Int ->
            toggleOverlay(!overlayLayout.isVisible)
        }
        displayAdapter.onZoomChanged = { zoom: ZoomableLayout, image: ImageView, position: Int ->
            //Enable scrolling only if not zoomed and one finger is over
            displayLayoutManager.setScrollEnabled(zoom.zoom <= 1 && zoom.pointers <= 1)
        }
        displayAdapter.onPointersChanged = { zoom: ZoomableLayout, image: ImageView, position: Int ->
            //Enable scrolling only if not zoomed and one finger is over
            displayLayoutManager.setScrollEnabled(zoom.zoom <= 1 && zoom.pointers <= 1)
        }
        displayAdapter.onPlay = { zoom: ZoomableLayout, image: ImageView, position: Int ->
            val intent = if (useInternalVideoPlayer) {
                //Play in internal player
                Intent(this, VideoActivity::class.java)
            } else {
                //Play in external player
                Intent(Intent.ACTION_VIEW)
            }
            intent.setDataAndType(Uri.fromFile(currentItem.file), currentItem.mimeType)
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
    }

    private fun toggleOverlay(show: Boolean) {
        if (show) {
            //Show
            Orion.animateShow(overlayLayout, 500)
            toggleSystemUI(true)
        } else {
            //Hide
            Orion.animateHide(overlayLayout, 500)
            toggleSystemUI(false)
        }
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

        //Create return intent (to scroll album list to current item on close)
        val returnIntent = Intent()
        returnIntent.putExtra("index", index)
        setResult(RESULT_OK, returnIntent)

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

        //Change image name & favourite state
        overlayTitle.text = currentItem.name
        overlayFavourite.visibility = if (currentItem.isFavourite) View.VISIBLE else View.GONE
    }

    //PiP
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        //Update state
        isInPiP = isInPictureInPictureMode

        //Check state
        if (!isInPiP && lifecycle.currentState == Lifecycle.State.CREATED) {
            //PiP was destroyed -> Destroy activity
            finish()
        } else {
            //PiP was opened/closed -> Toggle overlay
            toggleOverlay(!isInPiP)
        }
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

    private fun onUpdateOptions() {
        //Get state info
        val isTrashed = currentItem.isTrashed
        val isFavourite = currentItem.isFavourite

        //Update options list
        if (isViewingExternal) {
            //Viewing external file
            if (!isTrashed) {
                options.add(OptionsGroup(mutableListOf<OptionsItem>().apply {
                    add(optionEdit)
                    add(optionShare)
                    add(optionSetAs)
                    add(optionPiP)
                }))
            }
        } else {
            //Viewing gallery items
            if (!isTrashed) {
                options.add(OptionsGroup(mutableListOf<OptionsItem>().apply {
                    add(optionRename)
                    add(optionEdit)
                    add(optionShare)
                    add(optionSetAs)
                    add(optionPiP)
                }))
                options.add(OptionsGroup(mutableListOf<OptionsItem>().apply {
                    if (isFavourite) {
                        add(optionUnfavourite)
                    } else {
                        add(optionFavourite)
                    }
                    add(optionMove)
                    add(optionCopy)
                }))
            }
            options.add(OptionsGroup(mutableListOf<OptionsItem>().apply {
                if (!isTrashed) {
                    add(optionTrash)
                } else {
                    add(optionRestore)
                }
                add(optionDelete)
            }))
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
