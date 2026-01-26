package com.botpa.turbophotos.screens.album

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

import com.botpa.turbophotos.R
import com.botpa.turbophotos.screens.display.DisplayActivity
import com.botpa.turbophotos.gallery.Album
import com.botpa.turbophotos.gallery.CoonItem
import com.botpa.turbophotos.gallery.GalleryActivity
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.Library.RefreshEvent
import com.botpa.turbophotos.gallery.Library.ActionEvent
import com.botpa.turbophotos.gallery.LoadingIndicator
import com.botpa.turbophotos.gallery.actions.Action
import com.botpa.turbophotos.gallery.options.OptionsAdapter
import com.botpa.turbophotos.gallery.options.OptionsItem
import com.botpa.turbophotos.gallery.StoragePairs
import com.botpa.turbophotos.util.BackManager
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage
import me.zhanghai.android.fastscroll.FastScroller
import me.zhanghai.android.fastscroll.FastScrollerBuilder

import java.util.Locale

@SuppressLint("SetTextI18n", "NotifyDataSetChanged")
class AlbumActivity : GalleryActivity() {

      /*$$$$$  /$$ /$$
     /$$__  $$| $$| $$
    | $$  \ $$| $$| $$$$$$$  /$$   /$$ /$$$$$$/$$$$
    | $$$$$$$$| $$| $$__  $$| $$  | $$| $$_  $$_  $$
    | $$__  $$| $$| $$  \ $$| $$  | $$| $$ \ $$ \ $$
    | $$  | $$| $$| $$  | $$| $$  | $$| $$ | $$ | $$
    | $$  | $$| $$| $$$$$$$/|  $$$$$$/| $$ | $$ | $$
    |__/  |__/|__/|_______/  \______/ |__/ |__/ |_*/

    //Activity
    private lateinit var backManager: BackManager
    private var isMetadataLoaded = false
    private var isSearching = false
    private var isInit = false

    //Events
    private val onRefresh = RefreshEvent { updated -> this.manageRefresh(updated) }
    private val onAction = ActionEvent { action -> this.manageAction(action) }

    //Item picker for external apps
    private var isPicking = false //An app requested to pick an item

    //List
    private lateinit var albumLayoutManager: GridLayoutManager
    private lateinit var albumAdapter: AlbumAdapter

    private val selectedItems: MutableSet<Int> = HashSet()
    private lateinit var currentAlbum: Album
    private var inTrash = false

    private lateinit var albumRefreshLayout: SwipeRefreshLayout
    private lateinit var albumList: RecyclerView
    private lateinit var albumFastScroller: FastScroller

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

    //Options
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
    private lateinit var optionRestoreAll: OptionsItem
    private lateinit var optionDelete: OptionsItem
    private lateinit var optionDeleteAll: OptionsItem

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

    //Views (system)
    private lateinit var systemNotificationsBar: View
    private lateinit var systemNavigationBar: View

    //Views (navbar)
    private lateinit var navbarLayout: View
    private lateinit var navbarTitle: TextView
    private lateinit var navbarSubtitle: TextView
    private lateinit var navbarSearch: View
    private lateinit var navbarOptions: View

    //Views (search)
    private lateinit var searchLayout: View
    private lateinit var searchInput: EditText
    private lateinit var searchClose: View

    //Views (loading indicator)
    private lateinit var loadIndicator: View
    private lateinit var loadIndicatorText: TextView

    var loadingIndicator: LoadingIndicator = object : LoadingIndicator {
        override fun search() {
            loadIndicatorText.text = "Searching..."
            loadIndicator.visibility = View.VISIBLE
        }

        override fun load(content: String) {
            runOnUiThread {
                loadIndicatorText.text = "Loading ${content}..."
                loadIndicator.visibility = View.VISIBLE
            }
        }

        override fun load(folder: String, type: String) {
            runOnUiThread {
                loadIndicatorText.text = "Loading \"${folder}\" ${type}..."
                loadIndicator.visibility = View.VISIBLE
            }
        }

        override fun hide() {
            runOnUiThread {
                loadIndicator.visibility = View.GONE
            }
        }
    }



      /*$$$$$  /$$ /$$
     /$$__  $$| $$| $$
    | $$  \ $$| $$| $$$$$$$  /$$   /$$ /$$$$$$/$$$$
    | $$$$$$$$| $$| $$__  $$| $$  | $$| $$_  $$_  $$
    | $$__  $$| $$| $$  \ $$| $$  | $$| $$ \ $$ \ $$
    | $$  | $$| $$| $$  | $$| $$  | $$| $$ | $$ | $$
    | $$  | $$| $$| $$$$$$$/|  $$$$$$/| $$ | $$ | $$
    |__/  |__/|__/|_______/  \______/ |__/ |__/ |_*/

    //Activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.album_screen)

        //Enable HDR
        window.colorMode = ActivityInfo.COLOR_MODE_HDR

        //Add events
        Library.addOnRefreshEvent(onRefresh)
        Library.addOnActionEvent(onAction)

        //Init components
        backManager = BackManager(this, onBackPressedDispatcher)
        initViews()
        initListeners()
        initAlbumList()
        initOptionsList()

        //Init activity
        initActivity()
    }

    override fun onDestroy() {
        super.onDestroy()

        //Remove events
        Library.removeOnRefreshEvent(onRefresh)
        Library.removeOnActionEvent(onAction)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        //Update list items per row
        updateListItemsPerRow()
    }

    private fun initActivity() {
        //Check if intent is valid
        val intent = getIntent()
        if (intent == null) {
            finish()
            return
        }

        //Update is picking
        isPicking = intent.getBooleanExtra("isPicking", false)

        //Check if intent has album name or index
        if (intent.hasExtra("albumName")) {
            //Has name -> Check it
            when (intent.getStringExtra("albumName")) {
                "trash" -> selectAlbum(Library.trash)
                "all" -> selectAlbum(Library.all)
                else -> finish()
            }
        } else {
            //No name -> Check index
            val index = intent.getIntExtra("albumIndex", -1)
            if (index < 0 || index >= Library.albums.size) {
                finish()
                return
            }
            selectAlbum(Library.albums.get(index))
        }

        //Mark as init
        isInit = true
    }

    //Events
    private fun manageRefresh(updated: Boolean) {
        runOnUiThread {
            //Nothing updated
            if (!updated) return@runOnUiThread

            //Unselect all
            unselectAll()

            //Refresh list
            selectAlbum(currentAlbum)
        }
    }

    private fun manageAction(action: Action) {
        //No action
        if (action.isOfType(Action.TYPE_NONE)) return

        //Renamed file
        if (action.isOfType(Action.TYPE_RENAME)) {
            //Unselect item
            unselectAll()
            return
        }

        //Check if gallery is empty
        if (Library.gallery.isEmpty()) {
            //Is empty -> Close display
            finish()
            return
        }

        //Remove selected items & update adapter
        for (indexInGallery in action.removedIndexesInGallery) {
            selectedItems.remove(indexInGallery)
            albumAdapter.notifyItemRemoved(indexInGallery)
        }

        //Remove select back callback if no more items are selected
        if (selectedItems.isEmpty()) unselectAll()
    }

    //Views
    private fun initViews() {
        //Navbar
        navbarLayout = findViewById(R.id.navbarLayout)
        navbarTitle = findViewById(R.id.navbarTitle)
        navbarSubtitle = findViewById(R.id.navbarSubtitle)
        navbarSearch = findViewById(R.id.navbarSearch)
        navbarOptions = findViewById(R.id.navbarOptions)

        //Search
        searchLayout = findViewById(R.id.searchLayout)
        searchInput = findViewById(R.id.searchInput)
        searchClose = findViewById(R.id.searchClose)

        //Options
        optionsList = findViewById(R.id.optionsList)
        optionsLayout = findViewById(R.id.optionsLayout)

        //List
        albumList = findViewById(R.id.list)
        albumRefreshLayout = findViewById(R.id.refreshLayout)

        //Loading indicator
        loadIndicatorText = findViewById(R.id.loadIndicatorText)
        loadIndicator = findViewById(R.id.loadIndicator)

        //System
        systemNavigationBar = findViewById(R.id.navigationBar)
        systemNotificationsBar = findViewById(R.id.notificationsBar)


        //Insets (content)
        Orion.addInsetsChangedListener(
            findViewById(R.id.content),
            intArrayOf(WindowInsetsCompat.Type.systemBars())
        ) { view: View, insets: Insets, duration: Float ->
            albumRefreshLayout.setProgressViewOffset(false, 0, insets.top + 50)
            albumList.setPadding(0, insets.top, 0, albumList.paddingBottom + insets.bottom)
            albumFastScroller.setPadding(0, albumList.paddingTop, 0, albumList.paddingBottom)
        }

        //Insets (layout)
        Orion.addInsetsChangedListener(
            findViewById(R.id.layout),
            intArrayOf(WindowInsetsCompat.Type.systemBars(), WindowInsetsCompat.Type.ime()),
            200f
        ) { view: View, insets: Insets, percent: Float ->
            //Local insets var
            var insets = insets

            //Check if keyboard is open
            val windowInsets = ViewCompat.getRootWindowInsets(view)
            if (windowInsets != null && windowInsets.isVisible(WindowInsetsCompat.Type.ime())) {
                //Keyboard is open -> Only use keyboard insets
                insets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            }

            //Update insets
            Orion.onInsetsChangedDefault.run(view, insets, percent)
        }

        //Insets (options layout)
        Orion.addInsetsChangedListener(
            optionsLayout,
            intArrayOf(WindowInsetsCompat.Type.systemBars())
        )

        //Insets (system bars background)
        Orion.addInsetsChangedListener(
            systemNotificationsBar,
            intArrayOf(WindowInsetsCompat.Type.systemBars())
        ) { view: View, insets: Insets, duration: Float ->
            systemNotificationsBar.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, insets.top)
            systemNavigationBar.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, insets.bottom)
        }
    }

    private fun initListeners() {
        //Navbar
        navbarSearch.setOnClickListener { view: View -> showSearchLayout(true) }

        navbarOptions.setOnClickListener { view: View -> toggleOptions(true) }

        //Options
        optionsLayout.setOnClickListener { view: View -> toggleOptions(false) }

        optionRename = OptionsItem(R.drawable.rename, "Rename") {
            //Only allow 1 selection
            if (selectedItems.size != 1) return@OptionsItem

            //Rename
            Library.renameItem(this, Library.gallery[selectedItems.iterator().next()])
        }

        optionEdit = OptionsItem(R.drawable.edit, "Edit") {
            //Only allow 1 selection
            if (selectedItems.size != 1) return@OptionsItem

            //Edit
            Library.editItem(this, Library.gallery[selectedItems.iterator().next()])
        }

        optionShare = OptionsItem(R.drawable.share, "Share") {
            //Share
            Library.shareItems(this, getSelectedItems())
        }

        optionMove = OptionsItem(R.drawable.move, "Move to album") {
            //Move items
            Library.moveItems(this, getSelectedItems())
        }

        optionCopy = OptionsItem(R.drawable.copy, "Copy to album") {
            //Copy items
            Library.copyItems(this, getSelectedItems())
        }

        optionTrash = OptionsItem(R.drawable.trash, "Move to trash") {
            //Move items to trash
            trashItems(getSelectedItems())
        }

        optionRestore = OptionsItem(R.drawable.restore, "Restore") {
            //Restore items from trash
            restoreItems(getSelectedItems())
        }

        optionRestoreAll = OptionsItem(R.drawable.restore, "Restore all") {
            //Restore all items from trash
            restoreItems(currentAlbum.items.toTypedArray<CoonItem>())
        }

        optionDelete = OptionsItem(R.drawable.delete, "Delete") {
            //Delete item
            Library.deleteItems(this, getSelectedItems())
        }

        optionDeleteAll = OptionsItem(R.drawable.delete, "Delete all") {
            //Delete all items
            Library.deleteItems(this, currentAlbum.items.toTypedArray<CoonItem>())
        }

        //List
        albumRefreshLayout.setOnRefreshListener {
            //Refresh library
            Library.loadLibrary(this, false)

            //Stop refreshing
            albumRefreshLayout.isRefreshing = false
        }

        albumList.addOnItemTouchListener(DragSelectTouchListener(
            this,
            albumList,
            onSelectRange = { from, to, min, max ->
                //Select range items
                selectRange(from..to)

                //Deselect extra items
                if (min < from) deselectRange(min..(from - 1))
                if (max > to) deselectRange((to + 1)..max)
            },
            onSingleTap = { position ->
                if (selectedItems.isNotEmpty()) {
                    //Toggle item
                    toggleSelected(position)
                } else {
                    //Open item
                    openItem(position)
                }
            },
            onDragSelectingChanged = { isDragSelecting ->
                //Disable swipe refresh layout when drag selecting
                albumRefreshLayout.requestDisallowInterceptTouchEvent(isDragSelecting)
            }
        ))

        //Search
        searchInput.setOnKeyListener { view: View, i: Int, keyEvent: KeyEvent ->
            if (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                //Get search text
                val search = searchInput.text.toString()

                //Filter items with search
                filterItems(search, true)
            }
            false
        }

        searchClose.setOnClickListener { view: View -> showSearchLayout(false) }
    }

    //Album
    private fun initAlbumList() {
        //Init album layout manager
        albumLayoutManager = GridLayoutManager(this, listItemsPerRow)
        albumList.setLayoutManager(albumLayoutManager)

        //Init album adapter
        albumAdapter = AlbumAdapter(this, Library.gallery, selectedItems, Storage.getBool(StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON))
        albumList.setAdapter(albumAdapter)

        //Init home fast scroller
        albumFastScroller = FastScrollerBuilder(albumList).apply {
            setThumbDrawable(ContextCompat.getDrawable(this@AlbumActivity, R.drawable.scrollbar_thumb)!!)
            setTrackDrawable(ContextCompat.getDrawable(this@AlbumActivity, R.drawable.scrollbar_track)!!)
        }.build()
    }

    private fun selectAlbum(album: Album) {
        //Select album
        this.currentAlbum = album

        //Check if in trash (trash always shows options cause of "Delete all" action)
        inTrash = (album == Library.trash)
        navbarOptions.visibility = if (inTrash) View.VISIBLE else View.GONE

        //Update navbar title
        updateNavbarTitle()

        //Load album
        loadMetadata(album)
        filterItems()
    }

    private fun openItem(index: Int) {
        //Check action
        if (isPicking) {
            //Pick item
            val resultIntent = Intent()
            resultIntent.data = Orion.getFileUriFromFilePath(this, Library.gallery[index].file.absolutePath)
            resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setResult(RESULT_OK, resultIntent)
            finish()
        } else {
            //Open display
            val intent = Intent(this, DisplayActivity::class.java)
            intent.putExtra("index", index)
            startActivity(intent)
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

    private fun initOptionsList() {
        //Init options layout manager
        optionsList.setLayoutManager(LinearLayoutManager(this))

        //Init options adapter
        optionsAdapter = OptionsAdapter(this, options)
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

    private fun toggleOptions(show: Boolean) {
        if (show) {
            //Get state info
            val isSelecting = !selectedItems.isEmpty()
            val isSelectingSingle = selectedItems.size == 1

            //Update options list
            options.clear()
            if (!inTrash && isSelectingSingle) options.add(optionRename)
            if (!inTrash && isSelectingSingle) options.add(optionEdit)
            if (!inTrash && isSelecting) options.add(optionShare)
            if (!inTrash && isSelecting) options.add(optionMove)
            if (!inTrash && isSelecting) options.add(optionCopy)
            if (!inTrash) options.add(optionSeparator)
            if (!inTrash && isSelecting) options.add(optionTrash)
            if (inTrash && isSelecting) options.add(optionRestore)
            if (inTrash && !isSelecting) options.add(optionRestoreAll)
            if (isSelecting) options.add(optionDelete)
            if (inTrash && !isSelecting) options.add(optionDeleteAll)
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

    private fun getSelectedItems(): Array<CoonItem> {
        val selectedFiles = ArrayList<CoonItem>(selectedItems.size)
        for (index in selectedItems) selectedFiles.add(Library.gallery[index])
        return selectedFiles.toTypedArray<CoonItem>()
    }

      /*$$$$$    /$$     /$$
     /$$__  $$  | $$    | $$
    | $$  \ $$ /$$$$$$  | $$$$$$$   /$$$$$$   /$$$$$$
    | $$  | $$|_  $$_/  | $$__  $$ /$$__  $$ /$$__  $$
    | $$  | $$  | $$    | $$  \ $$| $$$$$$$$| $$  \__/
    | $$  | $$  | $$ /$$| $$  | $$| $$_____/| $$
    |  $$$$$$/  |  $$$$/| $$  | $$|  $$$$$$$| $$
     \______/    \___/  |__/  |__/ \_______/|_*/

    //List grid
    private val listItemsPerRow: Int get() {
        //Check if in horizontal orientation
        val isHorizontal = getResources().configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        //Get portrait aspect ratio
        val metrics = getResources().displayMetrics
        val ratio = (metrics.widthPixels.toFloat() / metrics.heightPixels.toFloat())

        //Get portrait items per row
        val itemsPerRow = Storage.getInt(StoragePairs.ALBUM_ITEMS_PER_ROW)

        //Return items per row for current orientation
        return if (isHorizontal) (itemsPerRow * ratio).toInt() else itemsPerRow
    }

    private fun updateListItemsPerRow() {
        val newItemsPerRow = listItemsPerRow
        if (albumLayoutManager.spanCount != newItemsPerRow) {
            albumLayoutManager.setSpanCount(newItemsPerRow)
        }
    }

    //Navbar
    private fun updateNavbarTitle() {
        //Update navbar title
        navbarTitle.text = "${currentAlbum.name}${if (selectedItems.isEmpty()) "" else " (${selectedItems.size} selected)"}"
    }

    //Metadata
    private fun loadMetadata(album: Album) {
        //Start loading
        isMetadataLoaded = false

        //Load metadata
        Thread {
            //Load metadata
            Library.loadMetadata(loadingIndicator, album)

            //Update items
            runOnUiThread {
                //Update album list
                albumAdapter.notifyDataSetChanged()

                //Finish loading
                loadingIndicator.hide()
                isMetadataLoaded = true
            }
        }.start()
    }

    //Selection
    private fun selectRange(range: IntRange) {
        //Ignore if range is empty
        if (range.isEmpty()) return

        //Check if its the first item to be selected
        if (selectedItems.isEmpty()) {
            //Add back event
            backManager.register("selected") { this.unselectAll() }

            //Show options button
            if (!inTrash) navbarOptions.visibility = View.VISIBLE
        }

        //Select items & update adapter
        val selected = ArrayList<Int>()
        for (index in range) if (selectedItems.add(index)) selected.add(index)
        for (index in selected) albumAdapter.notifyItemChanged(index)

        //Update navbar title
        updateNavbarTitle()
    }

    private fun deselectRange(range: IntRange) {
        //Ignore if range is empty
        if (range.isEmpty()) return

        //Deselect items & update adapter
        val deselected = ArrayList<Int>()
        for (index in range) if (selectedItems.remove(index)) deselected.add(index)
        for (index in deselected) albumAdapter.notifyItemChanged(index)

        //Check if no more items are selected
        if (selectedItems.isEmpty()) {
            //Add back event
            backManager.register("selected") { this.unselectAll() }

            //Show options button
            if (!inTrash) navbarOptions.visibility = View.VISIBLE
        }

        //Update navbar title
        updateNavbarTitle()
    }

    private fun toggleSelected(index: Int) {
        //Check if item is selected
        if (selectedItems.contains(index)) {
            //Deselect item
            deselectRange(index..index)
        } else {
            //Select item
            selectRange(index..index)
        }
    }

    private fun unselectAll() {
        //Remove back event
        backManager.unregister("selected")

        //Hide options
        if (!inTrash) navbarOptions.visibility = View.GONE

        //Unselect all
        if (!selectedItems.isEmpty()) {
            val temp = HashSet<Int>(selectedItems)
            selectedItems.clear()
            for (index in temp) albumAdapter.notifyItemChanged(index)
        }

        //Update navbar title
        updateNavbarTitle()
    }

    //Items & search
    private fun filterItems(filterText: String = "", scrollToTop: Boolean = false) {
        //Ignore case
        val filter = filterText.lowercase(Locale.getDefault())

        //Check if filtering
        val isFiltering = !filter.isEmpty()

        //Loading or searching
        if (isSearching || (!isMetadataLoaded && isFiltering)) return

        //Update subtitle
        navbarSubtitle.text = if (isFiltering) "Search: $filterText" else ""
        navbarSubtitle.visibility = if (isFiltering) View.VISIBLE else View.GONE

        //Start search
        isSearching = true
        searchInput.setText(filterText)
        if (isFiltering) loadingIndicator.search()
        showSearchLayout(false)

        //Update back manager
        if (isFiltering) backManager.register("search") { this.filterItems() }
        else backManager.unregister("search")

        //Clear selected items
        selectedItems.clear()

        //Filter items
        Thread {
            //Filter library gallery list
            Library.filterGallery(filter, currentAlbum)

            //Update items
            runOnUiThread {
                //Update adapter
                albumAdapter.notifyDataSetChanged()

                //Scroll to top
                albumList.stopScroll()
                if (scrollToTop) albumList.scrollToPosition(0)

                //Finish searching
                if (isFiltering) loadingIndicator.hide()
                isSearching = false
            }
        }.start()
    }

    private fun showSearchLayout(show: Boolean) {
        if (show) {
            //Loading or searching
            if (isSearching) return

            //Toggle search
            Orion.hideAnim(navbarLayout, 50) { Orion.showAnim(searchLayout, 50) }

            //Focus text & show keyboard
            searchInput.requestFocus()
            searchInput.selectAll()
            Orion.showKeyboard(this)

            //Back button
            backManager.register("searchMenu") { showSearchLayout(false) }
        } else {
            //Close keyboard
            Orion.hideKeyboard(this)
            Orion.clearFocus(this)

            //Toggle search
            Orion.hideAnim(searchLayout, 50) { Orion.showAnim(navbarLayout, 50) }

            //Back button
            backManager.unregister("searchMenu")
        }
    }

}
