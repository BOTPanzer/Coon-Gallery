package com.botpa.turbophotos.album

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
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

import com.botpa.turbophotos.R
import com.botpa.turbophotos.display.DisplayActivity
import com.botpa.turbophotos.gallery.Album
import com.botpa.turbophotos.gallery.CoonItem
import com.botpa.turbophotos.gallery.GalleryActivity
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.Library.RefreshEvent
import com.botpa.turbophotos.gallery.LoadingIndicator
import com.botpa.turbophotos.gallery.actions.Action
import com.botpa.turbophotos.gallery.options.OptionsAdapter
import com.botpa.turbophotos.gallery.options.OptionsItem
import com.botpa.turbophotos.util.BackManager
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

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
    private val onAction = Library.ActionEvent { action -> this.manageAction(action) }

    //Item picker for external apps
    private var isPicking = false //An app requested to pick an item

    //List
    private lateinit var albumLayoutManager: GridLayoutManager
    private lateinit var albumAdapter: AlbumAdapter

    private val selectedItems: MutableSet<Int> = HashSet()
    private lateinit var currentAlbum: Album
    private var inTrash = false

    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var albumList: FastScrollRecyclerView

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
        backManager = BackManager(this@AlbumActivity, onBackPressedDispatcher)
        initViews()
        initListeners()
        initAdapters()

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

        //Update horizontal item count
        updateHorizontalItemCount()
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

    //Components
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
        refreshLayout = findViewById(R.id.refreshLayout)

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
            refreshLayout.setProgressViewOffset(false, 0, insets.top + 50)
            albumList.setPadding(0, insets.top, 0, albumList.paddingBottom + insets.bottom)
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

        optionEdit = OptionsItem(R.drawable.edit, "Edit") {
            //Empty selection
            if (selectedItems.size != 1) return@OptionsItem

            //Edit
            Library.editItem(this@AlbumActivity, Library.gallery[selectedItems.iterator().next()])
        }

        optionShare = OptionsItem(R.drawable.share, "Share") {
            //Share
            Library.shareItems(this@AlbumActivity, getSelectedItems())
        }

        optionMove = OptionsItem(R.drawable.move, "Move to album") {
            //Move items
            Library.moveItems(this@AlbumActivity, getSelectedItems())
        }

        optionCopy = OptionsItem(R.drawable.copy, "Copy to album") {
            //Copy items
            Library.copyItems(this@AlbumActivity, getSelectedItems())
        }

        optionTrash = OptionsItem(R.drawable.delete, "Move to trash") {
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
            Library.deleteItems(this@AlbumActivity, getSelectedItems())
        }

        optionDeleteAll = OptionsItem(R.drawable.delete, "Delete all") {
            //Delete all items
            Library.deleteItems(this@AlbumActivity, currentAlbum.items.toTypedArray<CoonItem?>())
        }

        //List
        refreshLayout.setOnRefreshListener {
            //Reload library
            Library.loadLibrary(this@AlbumActivity, false) //Soft refresh to ONLY look for new files

            //Stop refreshing
            refreshLayout.isRefreshing = false
        }

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

    private fun initAdapters() {
        //Init album layout manager
        albumLayoutManager = GridLayoutManager(this@AlbumActivity, this.horizontalItemCount)
        albumList.setLayoutManager(albumLayoutManager)

        //Init album adapter
        albumAdapter = AlbumAdapter(this@AlbumActivity, Library.gallery, selectedItems, Storage.getBool("Settings.albumShowMissingMetadataIcon", false))
        albumList.setAdapter(albumAdapter)

        //Add adapter listeners
        albumAdapter.setOnClickListener { view: View, index: Int ->
            //Loading metadata -> Return
            if (!isMetadataLoaded) return@setOnClickListener

            //Check if selecting
            if (!selectedItems.isEmpty()) {
                //Selecting -> Toggle selected
                toggleSelected(index)
            } else {
                //Not selecting -> Check action
                if (isPicking) {
                    //Pick item
                    val resultIntent = Intent()
                    resultIntent.data = Orion.getFileUriFromFilePath(this@AlbumActivity, Library.gallery[index].file.absolutePath)
                    resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setResult(RESULT_OK, resultIntent)
                    finish()
                } else {
                    //Open display
                    val intent = Intent(this@AlbumActivity, DisplayActivity::class.java)
                    intent.putExtra("index", index)
                    startActivity(intent)
                }
            }
        }
        albumAdapter.setOnLongClickListener { view: View, index: Int ->
            //Toggle item selected
            toggleSelected(index)
            true
        }

        //Init options layout manager
        optionsList.setLayoutManager(LinearLayoutManager(this@AlbumActivity))

        //Init options adapter
        optionsAdapter = OptionsAdapter(this@AlbumActivity, options)
        optionsAdapter.setOnClickListener { view: View, index: Int ->
            //Get option
            val option = options.get(index)

            //Get action
            val action = option.action ?: return@setOnClickListener

            //Invoke action
            action.run()
            toggleOptions(false)
        }
        optionsList.setAdapter(optionsAdapter)
    }

    //Current album
    private fun selectAlbum(album: Album) {
        //Select album
        this.currentAlbum = album

        //Check if in trash (trash always shows options cause of "Delete all" action)
        inTrash = (album == Library.trash)
        navbarOptions.visibility = if (inTrash) View.VISIBLE else View.GONE

        //Change navbar title
        navbarTitle.text = album.name

        //Load album
        loadMetadata(album)
        filterItems()
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

    private fun getSelectedItems(): Array<CoonItem> {
        val selectedFiles = ArrayList<CoonItem>(selectedItems.size)
        for (index in selectedItems) selectedFiles.add(Library.gallery[index])
        return selectedFiles.toTypedArray<CoonItem>()
    }

    private fun toggleOptions(show: Boolean) {
        if (show) {
            //Get state info
            val isSelecting = !selectedItems.isEmpty()
            val isSelectingSingle = selectedItems.size == 1

            //Update options list
            options.clear()
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

      /*$$$$$    /$$     /$$
     /$$__  $$  | $$    | $$
    | $$  \ $$ /$$$$$$  | $$$$$$$   /$$$$$$   /$$$$$$
    | $$  | $$|_  $$_/  | $$__  $$ /$$__  $$ /$$__  $$
    | $$  | $$  | $$    | $$  \ $$| $$$$$$$$| $$  \__/
    | $$  | $$  | $$ /$$| $$  | $$| $$_____/| $$
    |  $$$$$$/  |  $$$$/| $$  | $$|  $$$$$$$| $$
     \______/    \___/  |__/  |__/ \_______/|_*/

    //List grid
    private val horizontalItemCount: Int get() {
        val isHorizontal = getResources().configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val metrics = getResources().displayMetrics
        val ratio = (metrics.widthPixels.toFloat() / metrics.heightPixels.toFloat())

        //Get size for portrait
        val size = Storage.getInt("Settings.albumItemsPerRow", 3)

        //Return size for current orientation
        return if (isHorizontal) (size * ratio).toInt() else size
    }

    private fun updateHorizontalItemCount() {
        val newHorizontalItemCount = this.horizontalItemCount
        if (albumLayoutManager.spanCount != newHorizontalItemCount) {
            albumLayoutManager.setSpanCount(newHorizontalItemCount)
        }
    }

    //Metadata
    private fun loadMetadata(album: Album?) {
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
    private fun toggleSelected(index: Int) {
        //Check if item is selected
        if (selectedItems.contains(index)) {
            //Remove item
            selectedItems.remove(index)

            //No more selected items
            if (selectedItems.isEmpty()) {
                //Remove back event
                backManager.unregister("selected")

                //Hide options button
                if (!inTrash) navbarOptions.visibility = View.GONE
            }
        } else {
            //First item to be selected
            if (selectedItems.isEmpty()) {
                //Add back event
                backManager.register("selected") { this.unselectAll() }

                //Show options button
                if (!inTrash) navbarOptions.visibility = View.VISIBLE
            }

            //Add item
            selectedItems.add(index)
        }
        albumAdapter.notifyItemChanged(index)

        //Update navbar title
        navbarTitle.text = "${currentAlbum.name}${if (selectedItems.isEmpty()) "" else " (" + selectedItems.size + " selected)"}"
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
        navbarTitle.text = currentAlbum.name
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
            Orion.showKeyboard(this@AlbumActivity)

            //Back button
            backManager.register("searchMenu") { showSearchLayout(false) }
        } else {
            //Close keyboard
            Orion.hideKeyboard(this@AlbumActivity)
            Orion.clearFocus(this@AlbumActivity)

            //Toggle search
            Orion.hideAnim(searchLayout, 50) { Orion.showAnim(navbarLayout, 50) }

            //Back button
            backManager.unregister("searchMenu")
        }
    }

}
