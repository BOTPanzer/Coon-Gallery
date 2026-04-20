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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.botpa.turbophotos.R
import com.botpa.turbophotos.screens.display.DisplayActivity
import com.botpa.turbophotos.gallery.Album
import com.botpa.turbophotos.gallery.Item
import com.botpa.turbophotos.gallery.BaseActivity
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.Library.RefreshEvent
import com.botpa.turbophotos.gallery.Library.ActionEvent
import com.botpa.turbophotos.gallery.LoadingIndicator
import com.botpa.turbophotos.gallery.SearchMethod
import com.botpa.turbophotos.gallery.actions.Action
import com.botpa.turbophotos.gallery.options.OptionsItem
import com.botpa.turbophotos.gallery.StoragePairs
import com.botpa.turbophotos.util.BackManager
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage
import com.botpa.turbophotos.gallery.fastscroller.FastScroller
import com.botpa.turbophotos.gallery.fastscroller.FastScrollerBuilder
import com.botpa.turbophotos.gallery.options.OptionsGroup
import com.botpa.turbophotos.gallery.options.OptionsManager
import com.botpa.turbophotos.screens.album.search.SearchDialog

@SuppressLint("SetTextI18n", "NotifyDataSetChanged")
class AlbumActivity : BaseActivity() {

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

    private val gallery: List<Item>
        get() = Library.gallery

    private val selectedIndexes: MutableSet<Int> = HashSet()
    private lateinit var currentAlbum: Album
    private var inTrash = false

    private lateinit var albumRefreshLayout: SwipeRefreshLayout
    private lateinit var albumList: RecyclerView
    private lateinit var albumFastScroller: FastScroller

    //Search
    private var currentSearchMethod: SearchMethod = SearchMethod.ContainsWords
    private var currentSearch: String = ""

    //Display
    private var displayIndex: Int = -1

    private val onDisplayClosed = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        //Not OK
        if (result.resultCode != RESULT_OK) return@registerForActivityResult

        //Move to last opened item on display when it closes
        val intent = result.data
        val newDisplayIndex = intent?.getIntExtra("index", displayIndex) ?: displayIndex
        if (newDisplayIndex != displayIndex) {
            albumList.scrollToPosition(newDisplayIndex)
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

    //Options
    private val options: MutableList<OptionsGroup> = ArrayList()
    private lateinit var optionsManager: OptionsManager

    private lateinit var optionRename: OptionsItem
    private lateinit var optionEdit: OptionsItem
    private lateinit var optionShare: OptionsItem
    private lateinit var optionSetAs: OptionsItem
    private lateinit var optionFavourite: OptionsItem
    private lateinit var optionUnfavourite: OptionsItem
    private lateinit var optionMove: OptionsItem
    private lateinit var optionCopy: OptionsItem
    private lateinit var optionTrash: OptionsItem
    private lateinit var optionRestore: OptionsItem
    private lateinit var optionRestoreAll: OptionsItem
    private lateinit var optionDelete: OptionsItem
    private lateinit var optionDeleteAll: OptionsItem

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
    private lateinit var searchSearch: View
    private lateinit var searchMethodName: TextView
    private lateinit var searchMethod: View
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
        optionsManager = OptionsManager(this, options, backManager) { onUpdateOptions() }
        initViews()
        initListeners()
        initAlbumList()

        //Init activity
        initActivity()
    }

    override fun onDestroy() {
        super.onDestroy()

        //Remove events
        Library.removeOnRefreshEvent(onRefresh)
        Library.removeOnActionEvent(onAction)
    }

    override fun onResume() {
        super.onResume()

        //Not init
        if (!isInit) return

        //Update search method
        updateSearchMethod()
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
                "favourites" -> selectAlbum(Library.favourites)
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

        //Check if gallery is empty
        if (gallery.isEmpty()) {
            //Is empty -> Close display
            finish()
            return
        }

        //Renamed file
        if (action.isOfType(Action.TYPE_RENAME)) {
            //Unselect item
            unselectAll()
            return
        }

        //Update items
        for (indexInGallery in action.modifiedIndexesInGallery) {
            albumAdapter.notifyItemChanged(indexInGallery)
        }

        //Remove items
        for (indexInGallery in action.removedIndexesInGallery) {
            selectedIndexes.remove(indexInGallery)
            albumAdapter.notifyItemRemoved(indexInGallery)
        }

        //Remove select back callback if no more items are selected
        if (selectedIndexes.isEmpty()) unselectAll()
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
        searchSearch = findViewById(R.id.searchSearch)
        searchMethodName = findViewById(R.id.searchMethodName)
        searchMethod = findViewById(R.id.searchMethod)
        searchClose = findViewById(R.id.searchClose)

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
        val listMinBottomPadding = albumList.paddingBottom
        Orion.addInsetsChangedListener(
            findViewById(R.id.content),
            intArrayOf(WindowInsetsCompat.Type.systemBars())
        ) { view: View, insets: Insets, duration: Float ->
            albumRefreshLayout.setProgressViewOffset(false, 0, insets.top + 50)
            albumList.setPadding(0, insets.top, 0, listMinBottomPadding + insets.bottom)
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

        //Insets (system bars background)
        Orion.addInsetsChangedListener(
            systemNotificationsBar,
            intArrayOf(WindowInsetsCompat.Type.systemBars())
        ) { view: View, insets: Insets, duration: Float ->
            systemNotificationsBar.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, insets.top)
            systemNavigationBar.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, insets.bottom)
        }

        //Insets (close search layout when keyboard gets hidden)
        ViewCompat.setOnApplyWindowInsetsListener(searchLayout) { v, insets ->
            //Check if keyboard is visible
            val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            //Hide search layout if keyboard was closed
            if (!isKeyboardVisible && searchLayout.isVisible) {
                searchClose.performClick()
            }

            //Return insets so layout stays correct
            insets
        }

        //Insets (options layout)
        Orion.addInsetsChangedListener(
            optionsManager.layout,
            intArrayOf(WindowInsetsCompat.Type.systemBars())
        )
    }

    private fun initListeners() {
        //Navbar
        navbarSearch.setOnClickListener { view: View -> showSearchLayout(true) }

        navbarOptions.setOnClickListener { view: View -> optionsManager.toggle(true) }

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
                if (selectedIndexes.isNotEmpty()) {
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
            if (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) searchSearch.performClick()
            false
        }

        searchSearch.setOnClickListener { view ->
            //Get search text
            val search = searchInput.text.toString()

            //Filter items with search
            filterItems(search, true)
        }

        searchMethod.setOnClickListener { view: View ->
            SearchDialog(this) { method ->
                //Update method
                currentSearchMethod = method
                searchMethodName.text = getSearchMethodName(currentSearchMethod)
                Storage.putString(StoragePairs.ALBUM_SEARCH_METHOD, currentSearchMethod.name)

                //Filter
                if (currentSearch.isNotEmpty()) filterItems(currentSearch, true)
            }.buildAndShow()
        }

        searchClose.setOnClickListener { view: View -> showSearchLayout(false) }

        //Options
        optionsManager.layout.setOnClickListener { view: View -> optionsManager.toggle(false) }

        optionRename = OptionsItem(R.drawable.rename, "Rename") {
            //Only allow 1 selection
            if (selectedIndexes.size != 1) return@OptionsItem

            //Rename
            Library.renameItem(this, gallery[selectedIndexes.iterator().next()])
        }

        optionEdit = OptionsItem(R.drawable.edit, "Edit") {
            //Only allow 1 selection
            if (selectedIndexes.size != 1) return@OptionsItem

            //Edit
            Library.editItem(this, gallery[selectedIndexes.iterator().next()])
        }

        optionShare = OptionsItem(R.drawable.share, "Share") {
            //Share
            Library.shareItems(this, getSelectedItems())
        }

        optionSetAs = OptionsItem(R.drawable.wallpaper, "Set as") {
            //Only allow 1 selection
            if (selectedIndexes.size != 1) return@OptionsItem

            //Set as
            Library.setItemAs(this, gallery[selectedIndexes.iterator().next()])
        }

        optionFavourite = OptionsItem(R.drawable.favourite_on, "Favourite") {
            //Add to favourites
            favouriteItems(getSelectedItems())
        }

        optionUnfavourite = OptionsItem(R.drawable.favourite_off, "Unfavourite") {
            //Remove from favourites
            unfavouriteItems(getSelectedItems())
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
            restoreItems(currentAlbum.items.toTypedArray<Item>())
        }

        optionDelete = OptionsItem(R.drawable.delete, "Delete") {
            //Delete item
            Library.deleteItems(this, getSelectedItems())
        }

        optionDeleteAll = OptionsItem(R.drawable.delete, "Delete all") {
            //Delete all items
            Library.deleteItems(this, currentAlbum.items.toTypedArray<Item>())
        }
    }

    //Album
    private fun initAlbumList() {
        //Init album layout manager
        albumLayoutManager = GridLayoutManager(this, listItemsPerRow)
        albumList.setLayoutManager(albumLayoutManager)

        //Init album adapter
        albumAdapter = AlbumAdapter(this, gallery, selectedIndexes, Storage.getBool(StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON))
        albumList.setAdapter(albumAdapter)

        //Init home fast scroller
        albumFastScroller = FastScrollerBuilder(albumList).build()
    }

    private fun selectAlbum(album: Album) {
        //Select album
        this.currentAlbum = album

        //Check if in trash (trash always shows options cause of "Delete all" action)
        inTrash = (album == Library.trash)
        navbarOptions.visibility = if (inTrash) View.VISIBLE else View.GONE

        //Update search method
        updateSearchMethod()

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
            resultIntent.data = Orion.getFileUriFromFilePath(this, gallery[index].file.absolutePath)
            resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setResult(RESULT_OK, resultIntent)
            finish()
        } else {
            //Save index
            displayIndex = index

            //Open display
            val intent = Intent(this, DisplayActivity::class.java)
            intent.putExtra("index", index)
            onDisplayClosed.launch(intent)
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
        val isSelecting = !selectedIndexes.isEmpty()
        val isSelectingSingle = selectedIndexes.size == 1

        //Update options list
        options.add(OptionsGroup(mutableListOf<OptionsItem>().apply {
            if (!inTrash && isSelectingSingle) add(optionRename)
            if (!inTrash && isSelectingSingle) add(optionEdit)
            if (!inTrash && isSelecting) add(optionShare)
            if (!inTrash && isSelectingSingle) add(optionSetAs)
        }))
        options.add(OptionsGroup(mutableListOf<OptionsItem>().apply {
            if (!inTrash) {
                if (selectedIndexes.all { gallery[it].isFavourite }) {
                    add(optionUnfavourite)
                } else if (selectedIndexes.all { !gallery[it].isFavourite }) {
                    add(optionFavourite)
                }
            }
            if (!inTrash && isSelecting) add(optionMove)
            if (!inTrash && isSelecting) add(optionCopy)
        }))
        options.add(OptionsGroup(mutableListOf<OptionsItem>().apply {
            if (!inTrash && isSelecting) add(optionTrash)
            if (inTrash && isSelecting) add(optionRestore)
            if (inTrash && !isSelecting) add(optionRestoreAll)
            if (isSelecting) add(optionDelete)
            if (inTrash && !isSelecting) add(optionDeleteAll)
        }))
    }

      /*$$$$$    /$$     /$$
     /$$__  $$  | $$    | $$
    | $$  \ $$ /$$$$$$  | $$$$$$$   /$$$$$$   /$$$$$$
    | $$  | $$|_  $$_/  | $$__  $$ /$$__  $$ /$$__  $$
    | $$  | $$  | $$    | $$  \ $$| $$$$$$$$| $$  \__/
    | $$  | $$  | $$ /$$| $$  | $$| $$_____/| $$
    |  $$$$$$/  |  $$$$/| $$  | $$|  $$$$$$$| $$
     \______/    \___/  |__/  |__/ \_______/|_*/

    //Selection
    private fun getSelectedItems(): Array<Item> {
        val selectedFiles = ArrayList<Item>(selectedIndexes.size)
        for (index in selectedIndexes) selectedFiles.add(gallery[index])
        return selectedFiles.toTypedArray<Item>()
    }

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
        navbarTitle.text = "${currentAlbum.name}${if (selectedIndexes.isEmpty()) "" else " (${selectedIndexes.size} selected)"}"
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
        if (selectedIndexes.isEmpty()) {
            //Add back event
            backManager.register("selected") { this.unselectAll() }

            //Show options button
            if (!inTrash) navbarOptions.visibility = View.VISIBLE
        }

        //Select items & update adapter
        val selected = ArrayList<Int>()
        for (index in range) if (selectedIndexes.add(index)) selected.add(index)
        for (index in selected) albumAdapter.notifyItemChanged(index)

        //Update navbar title
        updateNavbarTitle()
    }

    private fun deselectRange(range: IntRange) {
        //Ignore if range is empty
        if (range.isEmpty()) return

        //Deselect items & update adapter
        val deselected = ArrayList<Int>()
        for (index in range) if (selectedIndexes.remove(index)) deselected.add(index)
        for (index in deselected) albumAdapter.notifyItemChanged(index)

        //Check if no more items are selected
        if (selectedIndexes.isEmpty()) {
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
        if (selectedIndexes.contains(index)) {
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
        if (!selectedIndexes.isEmpty()) {
            val temp = HashSet<Int>(selectedIndexes)
            selectedIndexes.clear()
            for (index in temp) albumAdapter.notifyItemChanged(index)
        }

        //Update navbar title
        updateNavbarTitle()
    }

    //Items & search
    private fun filterItems(filter: String = "", scrollToTop: Boolean = false) {
        //Check if filtering
        val isFiltering = !filter.isEmpty()

        //Loading or searching
        if (isSearching || (!isMetadataLoaded && isFiltering)) return

        //Update subtitle
        navbarSubtitle.text = if (isFiltering) "Search: $filter" else ""
        navbarSubtitle.visibility = if (isFiltering) View.VISIBLE else View.GONE

        //Start search
        isSearching = true
        currentSearch = filter
        searchInput.setText(filter)
        searchMethodName.text = getSearchMethodName(currentSearchMethod)
        if (isFiltering) loadingIndicator.search()
        showSearchLayout(false)

        //Update back manager
        if (isFiltering) backManager.register("search") { this.filterItems() }
        else backManager.unregister("search")

        //Clear selected items
        selectedIndexes.clear()

        //Filter items
        Thread {
            //Filter library gallery list
            Library.filterGallery(filter, currentAlbum, currentSearchMethod)

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

    private fun updateSearchMethod() {
        //Update method
        currentSearchMethod = try {
            SearchMethod.valueOf(Storage.getString(StoragePairs.ALBUM_SEARCH_METHOD)?: "")
        } catch (e: IllegalArgumentException) {
            SearchMethod.ContainsWords
        }

        //Update text
        searchMethodName.text = getSearchMethodName(currentSearchMethod)
    }

    private fun getSearchMethodName(searchMethod: SearchMethod): String {
        return when (searchMethod) {
            SearchMethod.ContainsWords -> "Contains words"
            SearchMethod.ContainsText -> "Contains text"
        }
    }

    private fun showSearchLayout(show: Boolean) {
        if (show) {
            //Loading or searching
            if (isSearching) return

            //Toggle search
            Orion.animateHide(navbarLayout) { Orion.animateShow(searchLayout) }

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
            Orion.animateHide(searchLayout) { Orion.animateShow(navbarLayout) }

            //Back button
            backManager.unregister("searchMenu")
        }
    }

}
