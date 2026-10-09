package com.botpa.turbophotos.screens.album

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.BaseActivity
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.Library.ActionEvent
import com.botpa.turbophotos.gallery.Library.RefreshEvent
import com.botpa.turbophotos.gallery.LoadingIndicator
import com.botpa.turbophotos.gallery.PermissionType
import com.botpa.turbophotos.gallery.StoragePairs
import com.botpa.turbophotos.gallery.actions.ActionResult
import com.botpa.turbophotos.gallery.actions.ActionStepType
import com.botpa.turbophotos.gallery.data.Album
import com.botpa.turbophotos.gallery.data.Item
import com.botpa.turbophotos.gallery.options.OptionsGroup
import com.botpa.turbophotos.gallery.options.OptionsItem
import com.botpa.turbophotos.gallery.options.OptionsManager
import com.botpa.turbophotos.gallery.search.ListSearchHelper
import com.botpa.turbophotos.gallery.search.SearchDialog
import com.botpa.turbophotos.gallery.search.SearchMethod
import com.botpa.turbophotos.gallery.views.lists.GridHeaderLayoutManager
import com.botpa.turbophotos.gallery.views.lists.GridListSeparator
import com.botpa.turbophotos.gallery.views.lists.fastscroller.FastScroller
import com.botpa.turbophotos.gallery.views.lists.fastscroller.FastScrollerBuilder
import com.botpa.turbophotos.gallery.views.refresh.SimpleRefreshHeader
import com.botpa.turbophotos.screens.viewer.ViewerActivity
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Orion.pxToDp
import com.botpa.turbophotos.util.Storage
import com.scwang.smart.refresh.layout.SmartRefreshLayout

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
    override val permissions: List<PermissionType> = listOf(PermissionType.Storage, PermissionType.Media)
    override val askForPermissions: Boolean = false
    override val contentViewResource: Int = R.layout.album_screen

    private var isLibraryLoading = false
    private var isMetadataLoaded = false
    private var isInit = false

    private val isWorking get(): Boolean = isLibraryLoading || searchHelper.isSearching

    //Events
    private val onRefresh = RefreshEvent { updated -> this.manageRefresh(updated) }
    private val onAction = ActionEvent { action -> this.manageAction(action) }

    //Item picker for external apps
    private var isPicking = false //An app requested to pick an item

    //List
    private lateinit var albumLayoutManager: GridLayoutManager
    private lateinit var albumDecorator: GridListSeparator
    private lateinit var albumAdapter: AlbumAdapter

    private val gallery: Album get() = Library.gallery

    private val selectedIndexes: MutableSet<Int> = LinkedHashSet()
    private lateinit var currentAlbum: Album
    private var inTrash = false

    private lateinit var albumRefreshLayout: SmartRefreshLayout
    private lateinit var albumList: RecyclerView
    private lateinit var albumFastScroller: FastScroller

    //Search
    private var currentSearchMethod: SearchMethod = SearchMethod.ContainsWords

    private var searchHelper: ListSearchHelper<Item> = ListSearchHelper(true)

    //Viewer
    private var viewerIndex: Int = -1

    private val onViewerClosed = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        //Not OK
        if (result.resultCode != RESULT_OK) return@registerForActivityResult

        //Move to last opened item on viewer when it closes
        val intent = result.data
        val newViewerIndex = intent?.getIntExtra("index", viewerIndex) ?: viewerIndex
        if (newViewerIndex != viewerIndex) {
            albumList.scrollToPosition(newViewerIndex)
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
    private lateinit var navbarOptions: View
    private lateinit var navbarSearch: View

    //Views (search)
    private lateinit var searchLayout: View
    private lateinit var searchInput: EditText
    private lateinit var searchSearch: View
    private lateinit var searchMethodName: TextView
    private lateinit var searchMethod: View
    private lateinit var searchClose: View

    //Views (loading indicator)
    private lateinit var loadingIndicator: View
    private lateinit var loadingIndicatorText: TextView

    var loadingIndicatorManager: LoadingIndicator = object : LoadingIndicator {
        override fun search() {
            loadingIndicatorText.text = getString(R.string.album_loading_search)
            loadingIndicator.visibility = View.VISIBLE
        }

        override fun metadata(album: String) {
            runOnUiThread {
                loadingIndicatorText.text = getString(R.string.album_loading_metadata, album)
                loadingIndicator.visibility = View.VISIBLE
            }
        }

        override fun hide() {
            runOnUiThread {
                loadingIndicator.visibility = View.GONE
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
    override fun onBeforeInitViews() {
        //Add events
        Library.addOnRefreshEvent(onRefresh)
        Library.addOnActionEvent(onAction)

        //Init options
        optionsManager = OptionsManager(this, options, backManager) { onUpdateOptions() }
    }

    override fun onInitViews() {
        //Navbar
        navbarLayout = findViewById(R.id.navbarLayout)
        navbarTitle = findViewById(R.id.navbarTitle)
        navbarSubtitle = findViewById(R.id.navbarSubtitle)
        navbarOptions = findViewById(R.id.navbarOptions)
        navbarSearch = findViewById(R.id.navbarSearch)

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
        loadingIndicatorText = findViewById(R.id.loadingIndicatorText)
        loadingIndicator = findViewById(R.id.loadingIndicator)

        //System
        systemNavigationBar = findViewById(R.id.navigationBar)
        systemNotificationsBar = findViewById(R.id.notificationsBar)


        //Insets (content)
        val listMinBottomPadding = albumList.paddingBottom
        Orion.addInsetsChangedListener(
            findViewById(R.id.content),
            intArrayOf(WindowInsetsCompat.Type.systemBars())
        ) { view: View, insets: Insets, duration: Float ->
            //Swipe refresh top margin
            albumRefreshLayout.setHeaderInsetStart(insets.top.pxToDp.toFloat() + 16)

            //Album list search layout + keyboard margin
            albumList.setPadding(0, 0, 0, listMinBottomPadding + insets.bottom)
            albumFastScroller.setPadding(0, insets.top, 0, albumList.paddingBottom)

            //Album list notifications bar margin
            if (albumAdapter.topMargin == 0) {
                albumAdapter.topMargin = insets.top
                albumAdapter.notifyItemChanged(0)
            }
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

    override fun onInitListeners() {
        //Navbar
        navbarOptions.setOnClickListener { view: View ->
            //Not available
            if (isWorking) return@setOnClickListener

            //Open options
            optionsManager.toggle(true)
        }

        navbarSearch.setOnClickListener { view: View -> searchHelper.toggleLayout(true) }

        //List
        (albumRefreshLayout.refreshHeader as SimpleRefreshHeader).onMoved = { view, percent ->
            albumList.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = -(percent * view.height).toInt()
            }
        }

        albumRefreshLayout.setOnRefreshListener {
            //Mark as loading
            isLibraryLoading = true

            //Refresh
            Thread {
                //Load library
                Library.loadLibrary(this, true)

                //Stop refreshing
                runOnUiThread {
                    albumRefreshLayout.finishRefresh()

                    //Mark as loaded
                    isLibraryLoading = false
                }
            }.start()
        }

        albumList.addOnItemTouchListener(DragSelectTouchListener(
            this,
            albumList,
            startOffset = 1,
            onSelectRange = { from, to, min, max ->
                //Select range items
                selectRange(from..to)

                //Deselect extra items
                if (min < from) deselectRange(min..(from - 1))
                if (max > to) deselectRange((to + 1)..max)
            },
            onSingleTap = { index ->
                //Not available
                if (isWorking) return@DragSelectTouchListener

                //Perform action
                if (selectedIndexes.isNotEmpty()) {
                    //Toggle item
                    toggleSelected(index)
                } else {
                    //Open item
                    openItem(index)
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
            //Not available
            if (isWorking) return@setOnClickListener

            //Filter items with search query
            val query = searchInput.text.toString()
            searchHelper.filter(query)
        }

        searchMethod.setOnClickListener { view: View ->
            SearchDialog(this) { method ->
                //Update method
                currentSearchMethod = method
                searchMethodName.text = getSearchMethodName(currentSearchMethod)
                Storage.putString(StoragePairs.ALBUM_SEARCH_METHOD, currentSearchMethod.name)

                //Refresh search
                if (searchHelper.isFiltered) searchHelper.refresh()
            }.buildAndShow()
        }

        searchClose.setOnClickListener { view: View -> searchHelper.toggleLayout(false) }

        //Options
        optionRename = OptionsItem(R.drawable.icon_action_rename, R.string.context_option_rename) {
            //Only allow 1 selection
            if (selectedIndexes.size != 1) return@OptionsItem

            //Rename
            Library.renameItem(this, gallery.get(selectedIndexes.iterator().next()))
        }

        optionEdit = OptionsItem(R.drawable.icon_action_edit, R.string.context_option_edit) {
            //Only allow 1 selection
            if (selectedIndexes.size != 1) return@OptionsItem

            //Edit
            Library.editItem(this, gallery.get(selectedIndexes.iterator().next()))
        }

        optionShare = OptionsItem(R.drawable.icon_action_share, R.string.context_option_share) {
            //Share
            Library.shareItems(this, getSelectedItems())
        }

        optionSetAs = OptionsItem(R.drawable.icon_action_wallpaper, R.string.context_option_set) {
            //Only allow 1 selection
            if (selectedIndexes.size != 1) return@OptionsItem

            //Set as
            Library.setItemAs(this, gallery.get(selectedIndexes.iterator().next()))
        }

        optionFavourite = OptionsItem(R.drawable.icon_action_favourite_on, R.string.context_option_favourite) {
            //Add to favourites
            favouriteItems(getSelectedItems())
        }

        optionUnfavourite = OptionsItem(R.drawable.icon_action_favourite_off, R.string.context_option_unfavourite) {
            //Remove from favourites
            unfavouriteItems(getSelectedItems())
        }

        optionMove = OptionsItem(R.drawable.icon_action_move, R.string.context_option_move) {
            //Move items
            Library.moveItems(this, getSelectedItems())
        }

        optionCopy = OptionsItem(R.drawable.icon_action_copy, R.string.context_option_copy) {
            //Copy items
            Library.copyItems(this, getSelectedItems())
        }

        optionTrash = OptionsItem(R.drawable.icon_action_trash, R.string.context_option_trash) {
            //Move items to trash
            trashItems(getSelectedItems())
        }

        optionRestore = OptionsItem(R.drawable.icon_action_restore, R.string.context_option_restore) {
            //Restore items from trash
            restoreItems(getSelectedItems())
        }

        optionRestoreAll = OptionsItem(R.drawable.icon_action_restore, R.string.context_option_restore_all) {
            //Restore all items from trash
            restoreItems(currentAlbum.items.toTypedArray<Item>())
        }

        optionDelete = OptionsItem(R.drawable.icon_action_delete, R.string.context_option_delete) {
            //Delete item
            Library.deleteItems(this, getSelectedItems())
        }

        optionDeleteAll = OptionsItem(R.drawable.icon_action_delete, R.string.context_option_delete_all) {
            //Delete all items
            Library.deleteItems(this, currentAlbum.items.toTypedArray<Item>())
        }
    }

    override fun onAfterInitViews() {
        //Init components
        initAlbumList()
    }

    override fun onPermissionsGranted() {
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
                "all" -> selectAlbum(Library.all)
                "trash" -> selectAlbum(Library.trash)
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

    override fun onDestroy() {
        super.onDestroy()

        //Remove events
        Library.removeOnRefreshEvent(onRefresh)
        Library.removeOnActionEvent(onAction)

        //Reset gallery
        Library.setGalleryInfo(null, ArrayList())
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

    //Events
    private fun manageRefresh(updated: Boolean) {
        //Didn't update
        if (!updated) return

        //Check if gallery is empty
        if (currentAlbum.isEmpty()) {
            //Is empty -> Close screen
            finish()
            return
        }

        //Refresh
        runOnUiThread {
            //Unselect all
            deselectAll()

            //Refresh list
            selectAlbum(currentAlbum)
        }
    }

    private fun manageAction(action: ActionResult) {
        //Check if gallery is empty
        if (gallery.isEmpty()) {
            //Is empty -> Close screen
            finish()
            return
        }

        //Deselect items
        deselectAll()

        //Update items
        for (step in action.itemStepsInGallery) {
            //Get step position
            val position = albumAdapter.getPositionFromIndex(step.index)

            //Check type
            when (step.type) {
                //Add
                ActionStepType.ADD -> {
                    albumAdapter.notifyItemInserted(position)
                }
                //Remove
                ActionStepType.REMOVE -> {
                    albumAdapter.notifyItemRemoved(position)
                }
                //Modify
                ActionStepType.MODIFY -> {
                    albumAdapter.notifyItemChanged(position)
                }
                //Reorder
                ActionStepType.REORDER -> {
                    albumAdapter.notifyItemMoved(position, albumAdapter.getPositionFromIndex(step.index2))
                }
            }

            //Refresh banner
            updateHeaderSubtitle()
            albumAdapter.notifyItemChanged(0)
        }
    }

    //Album
    private fun initAlbumList() {
        //Init album layout manager
        albumLayoutManager = GridHeaderLayoutManager(this, listItemsPerRow) { position ->
            albumAdapter.getItemViewType(position) == 0
        }
        albumList.setLayoutManager(albumLayoutManager)
        albumDecorator = GridListSeparator(2, albumLayoutManager.spanCount, 1)
        albumList.addItemDecoration(albumDecorator)
        (albumList.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        //Init album adapter
        albumAdapter = AlbumAdapter(this, gallery.items, "", "", 0, selectedIndexes, Storage.getBool(StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON))
        albumList.setAdapter(albumAdapter)

        //Init home fast scroller
        albumFastScroller = FastScrollerBuilder(albumList)
            .setHasHeader(true)
            .build()

        //Init search helper
        searchHelper.init(this, backManager, navbarLayout, searchLayout, searchInput, albumList, this@AlbumActivity::onBeforeSearchFilter, this@AlbumActivity::onSearchFilter, this@AlbumActivity::onAfterSearchFilter)
    }

    private fun selectAlbum(album: Album) {
        //Select album
        currentAlbum = album

        //Update adapter header
        albumAdapter.title = getLocalizedAlbumName(album)

        //Check if in trash (trash always shows options cause of "Delete all" action)
        inTrash = (album == Library.trash)
        navbarOptions.visibility = if (inTrash) View.VISIBLE else View.GONE

        //Update search method
        updateSearchMethod()

        //Update navbar title
        updateNavbarTitle()

        //Load album
        loadMetadata(album)
        searchHelper.filter()
    }

    private fun getLocalizedAlbumName(album: Album): String {
        return when (album) {
            Library.all -> getString(R.string.library_album_all)
            Library.trash -> getString(R.string.library_album_trash)
            Library.favourites -> getString(R.string.library_album_favourites)
            else -> album.name
        }
    }

    private fun openItem(index: Int) {
        //Check action
        if (isPicking) {
            //Pick item
            val resultIntent = Intent()
            resultIntent.data = Orion.getFileUriFromFilePath(this, gallery.get(index).file.absolutePath)
            resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setResult(RESULT_OK, resultIntent)
            finish()
        } else {
            //Save index
            viewerIndex = index

            //Open viewer
            val intent = Intent(this, ViewerActivity::class.java)
            intent.putExtra("index", index)
            onViewerClosed.launch(intent)
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
        val isSelecting = selectedIndexes.isNotEmpty()
        val isSelectingSingle = selectedIndexes.size == 1

        //Update options list
        if (!inTrash) {
            options.add(OptionsGroup(mutableListOf<OptionsItem>().apply {
                if (isSelectingSingle) add(optionRename)
                if (isSelectingSingle) add(optionEdit)
                if (isSelecting) add(optionShare)
                if (isSelectingSingle) add(optionSetAs)
            }))
            options.add(OptionsGroup(mutableListOf<OptionsItem>().apply {
                if (selectedIndexes.all { gallery.items[it].isFavourite }) {
                    add(optionUnfavourite)
                } else if (selectedIndexes.all { !gallery.items[it].isFavourite }) {
                    add(optionFavourite)
                }
                if (isSelecting) add(optionMove)
                if (isSelecting) add(optionCopy)
            }))
        }
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
        for (index in selectedIndexes) selectedFiles.add(gallery.get(index))
        return selectedFiles.toTypedArray<Item>()
    }

    //List grid
    private val listItemsPerRow: Int get() {
        //Check if in horizontal orientation
        val isHorizontal = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        //Get portrait aspect ratio
        val metrics = resources.displayMetrics
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
            albumDecorator.spanCount = newItemsPerRow
            albumList.invalidateItemDecorations()
        }
    }

    //Navbar
    private fun updateNavbarTitle() {
        //Toggle text visibility
        val isTitleVisible = selectedIndexes.isNotEmpty()
        navbarTitle.visibility = if (isTitleVisible) View.VISIBLE else View.GONE
        if (!isTitleVisible) return

        //Update title
        navbarTitle.text = getString(if (selectedIndexes.size <= 1) R.string.album_loading_selection_item else R.string.album_loading_selection_items, selectedIndexes.size)
    }

    //Metadata
    private fun loadMetadata(album: Album) {
        //Start loading
        isMetadataLoaded = false

        //Load metadata
        Thread {
            //Load metadata
            Library.loadMetadata(loadingIndicatorManager, album)

            //Update items
            runOnUiThread {
                //Refresh album list (if missing metadata icons are enabled)
                if (albumAdapter.showMissingMetadataIcon) albumAdapter.notifyDataSetChanged()

                //Finish loading
                loadingIndicatorManager.hide()
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
            backManager.register("selected") { deselectAll() }

            //Show options
            if (!inTrash) navbarOptions.visibility = View.VISIBLE
        }

        //Select items & update adapter
        val selectedViews = ArrayList<Int>()
        for (index in range) if (selectedIndexes.add(index)) selectedViews.add(index)
        for (index in selectedViews) albumAdapter.notifyItemChanged(albumAdapter.getPositionFromIndex(index))

        //Update navbar title
        updateNavbarTitle()
    }

    private fun deselectRange(range: IntRange) {
        //Ignore if range is empty
        if (range.isEmpty()) return

        //Find the index of the first item being deselected
        val firstDeselectedIndex = selectedIndexes.indexOfFirst { it in range }
        if (firstDeselectedIndex == -1) return

        //Deselect items
        val deselectedItems = ArrayList<Int>()
        for (index in range) {
            if (selectedIndexes.remove(index)) {
                deselectedItems.add(index)
            }
        }

        //Update deselected items
        for (index in deselectedItems) {
            albumAdapter.notifyItemChanged(albumAdapter.getPositionFromIndex(index))
        }

        //Update remaining selected items after deselected items
        selectedIndexes.drop(firstDeselectedIndex).forEach { remainingIndex ->
            albumAdapter.notifyItemChanged(albumAdapter.getPositionFromIndex(remainingIndex))
        }

        //Check if no more items are selected
        if (selectedIndexes.isEmpty()) {
            //Remove back event
            backManager.unregister("selected")

            //Hide options
            if (!inTrash) navbarOptions.visibility = View.GONE
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

    private fun deselectAll() {
        //Remove back event
        backManager.unregister("selected")

        //Hide options
        if (!inTrash) navbarOptions.visibility = View.GONE

        //Deselect all
        if (!selectedIndexes.isEmpty()) {
            val temp = HashSet<Int>(selectedIndexes)
            selectedIndexes.clear()
            for (index in temp) albumAdapter.notifyItemChanged(albumAdapter.getPositionFromIndex(index))
        }

        //Update navbar title
        updateNavbarTitle()
    }

    //Items & search
    private fun updateHeaderSubtitle() {
        val id = if (searchHelper.isFiltered) R.string.album_header_search else R.string.album_header
        albumAdapter.subtitle = getString(id, gallery.items.size, searchHelper.currentQuery)
    }

    private fun updateSearchMethod() {
        //Update method
        currentSearchMethod = Storage.getEnum(StoragePairs.ALBUM_SEARCH_METHOD, SearchMethod.ContainsWords)

        //Update text
        searchMethodName.text = getSearchMethodName(currentSearchMethod)
    }

    private fun getSearchMethodName(searchMethod: SearchMethod): String {
        return getString(when (searchMethod) {
            SearchMethod.ContainsWords -> R.string.library_search_method_words
            SearchMethod.ContainsText -> R.string.library_search_method_text
            SearchMethod.NaturalLanguage -> R.string.library_search_method_natural
        })
    }

    private fun onBeforeSearchFilter(isFiltering: Boolean, query: String): Boolean {
        //Not available
        if (searchHelper.isSearching || (!isMetadataLoaded && isFiltering)) return false

        //Update UI
        searchInput.setText(query)
        searchMethodName.text = getSearchMethodName(currentSearchMethod)
        if (isFiltering) loadingIndicatorManager.search()
        searchHelper.toggleLayout(false)

        //Clear selected items
        selectedIndexes.clear()

        //Disable refresh
        albumRefreshLayout.setEnableRefresh(false)

        //Filter
        return true
    }

    private fun onSearchFilter(isFiltering: Boolean, query: String): MutableList<Item> {
        //Filter album list
        return Library.filterAlbum(this@AlbumActivity, query, currentAlbum, currentSearchMethod)
    }

    private fun onAfterSearchFilter(isFiltering: Boolean, query: String, items: MutableList<Item>) {
        //Update items
        Library.setGalleryInfo(currentAlbum, items) //List changes must be done in UI thread
        updateHeaderSubtitle()

        //Enable refresh
        albumRefreshLayout.setEnableRefresh(true)

        //Finish searching
        if (isFiltering) loadingIndicatorManager.hide()
    }

}
