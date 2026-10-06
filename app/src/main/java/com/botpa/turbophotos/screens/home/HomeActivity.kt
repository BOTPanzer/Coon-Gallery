package com.botpa.turbophotos.screens.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.BaseActivity
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.Library.ActionEvent
import com.botpa.turbophotos.gallery.Library.RefreshEvent
import com.botpa.turbophotos.gallery.PermissionType
import com.botpa.turbophotos.gallery.StoragePairs
import com.botpa.turbophotos.gallery.UpdateChecker
import com.botpa.turbophotos.gallery.actions.ActionResult
import com.botpa.turbophotos.gallery.data.Album
import com.botpa.turbophotos.gallery.data.SortDirection
import com.botpa.turbophotos.gallery.data.SortMethod
import com.botpa.turbophotos.gallery.modals.UpdateDialog
import com.botpa.turbophotos.gallery.options.OptionsGroup
import com.botpa.turbophotos.gallery.options.OptionsItem
import com.botpa.turbophotos.gallery.options.OptionsManager
import com.botpa.turbophotos.gallery.search.ListSearchHelper
import com.botpa.turbophotos.gallery.views.lists.GridHeaderLayoutManager
import com.botpa.turbophotos.gallery.views.lists.GridListSeparator
import com.botpa.turbophotos.gallery.views.lists.fastscroller.FastScroller
import com.botpa.turbophotos.gallery.views.lists.fastscroller.FastScrollerBuilder
import com.botpa.turbophotos.screens.album.AlbumActivity
import com.botpa.turbophotos.screens.home.filters.FiltersDialog
import com.botpa.turbophotos.screens.home.filters.FiltersItem
import com.botpa.turbophotos.screens.home.sorting.SortingDialog
import com.botpa.turbophotos.screens.home.sorting.SortingItem
import com.botpa.turbophotos.screens.settings.SettingsActivity
import com.botpa.turbophotos.screens.sync.SyncActivity
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Orion.pxToDp
import com.botpa.turbophotos.util.Storage
import com.scwang.smart.refresh.layout.SmartRefreshLayout

@SuppressLint("SetTextI18n", "NotifyDataSetChanged")
class HomeActivity : BaseActivity() {

     /*$   /$$
    | $$  | $$
    | $$  | $$  /$$$$$$  /$$$$$$/$$$$   /$$$$$$
    | $$$$$$$$ /$$__  $$| $$_  $$_  $$ /$$__  $$
    | $$__  $$| $$  \ $$| $$ \ $$ \ $$| $$$$$$$$
    | $$  | $$| $$  | $$| $$ | $$ | $$| $$_____/
    | $$  | $$|  $$$$$$/| $$ | $$ | $$|  $$$$$$$
    |__/  |__/ \______/ |__/ |__/ |__/ \______*/

    //Activity
    override val permissions: List<PermissionType> = listOf(PermissionType.Storage, PermissionType.Media)
    override val contentViewResource: Int = R.layout.home_screen

    private var isLibraryLoading = false
    private var isLibraryLoaded = false
    private var isInit = false

    private val isWorking get(): Boolean = isLibraryLoading || !isLibraryLoaded || searchHelper.isSearching

    //Permissions
    private val requestPermissionMedia = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted: Map<String, Boolean> ->
        permissionManager.notifyPermissionChanged(PermissionType.Media)
        checkPermissions()
    }

    //Events
    private val onRefresh = RefreshEvent { updated -> this.manageRefresh(updated) }
    private val onAction = ActionEvent { action -> this.manageAction(action) }

    //Item picker for external apps
    private var isPicking = false //An app requested to pick an item
    private val pickerLauncher = registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
        //Check if result is valid
        if (result.resultCode != RESULT_OK || result.data == null) return@registerForActivityResult

        //Return result
        val data: Intent = result.data!!
        setResult(RESULT_OK, data)
        finish()
    }

    //List
    private var homeAlbumsList: MutableList<Album> = ArrayList()

    private lateinit var homeLayoutManager: GridLayoutManager
    private lateinit var homeDecorator: GridListSeparator
    private lateinit var homeAdapter: HomeAdapter

    private lateinit var homeRefreshLayout: SmartRefreshLayout
    private lateinit var homeList: RecyclerView
    private lateinit var homeFastScroller: FastScroller

    //Search
    private var ignoreSearchInput: Boolean = false

    private var searchHelper: ListSearchHelper<Album> = ListSearchHelper(false)

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

    private lateinit var optionSync: OptionsItem
    private lateinit var optionSettings: OptionsItem
    private lateinit var optionSorting: OptionsItem
    private lateinit var optionFilters: OptionsItem

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
    private lateinit var searchClose: View

    //Views (loading indicator)
    private lateinit var loadingIndicator: View



     /*$   /$$
    | $$  | $$
    | $$  | $$  /$$$$$$  /$$$$$$/$$$$   /$$$$$$
    | $$$$$$$$ /$$__  $$| $$_  $$_  $$ /$$__  $$
    | $$__  $$| $$  \ $$| $$ \ $$ \ $$| $$$$$$$$
    | $$  | $$| $$  | $$| $$ | $$ | $$| $$_____/
    | $$  | $$|  $$$$$$/| $$ | $$ | $$|  $$$$$$$
    |__/  |__/ \______/ |__/ |__/ |__/ \______*/

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
        searchClose = findViewById(R.id.searchClose)

        //List
        homeRefreshLayout = findViewById(R.id.refreshLayout)
        homeList = findViewById(R.id.list)

        //Loading indicator
        loadingIndicator = findViewById(R.id.loadingIndicator)

        //System
        systemNotificationsBar = findViewById(R.id.notificationsBar)
        systemNavigationBar = findViewById(R.id.navigationBar)


        //Insets (content)
        val listMinBottomPadding = homeList.paddingBottom
        Orion.addInsetsChangedListener(
            findViewById(R.id.content),
            intArrayOf(WindowInsetsCompat.Type.systemBars())
        ) { view: View, insets: Insets, duration: Float ->
            //Swipe refresh top margin
            homeRefreshLayout.setHeaderInsetStart(insets.top.pxToDp.toFloat())

            //List search layout + keyboard margin
            homeList.setPadding(homeList.paddingLeft, insets.top, homeList.paddingRight, listMinBottomPadding + insets.bottom)
            homeFastScroller.setPadding(0, homeList.paddingTop, 0, homeList.paddingBottom)
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

        //Search
        searchInput.setOnKeyListener { view: View, i: Int, keyEvent: KeyEvent ->
            if (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) searchClose.performClick()
            false
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(text: Editable?) {}
            override fun beforeTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (!ignoreSearchInput) searchHelper.filter(searchInput.text.toString())
            }
        })

        searchClose.setOnClickListener { view: View -> searchHelper.toggleLayout(false) }

        //List
        homeRefreshLayout.setOnRefreshListener { layout ->
            //Mark as loading
            isLibraryLoading = true
            isLibraryLoaded = false

            //Refresh
            Thread {
                //Load library
                Library.loadLibrary(this, true)

                //Stop refreshing
                runOnUiThread {
                    homeRefreshLayout.finishRefresh()

                    //Mark as loaded
                    isLibraryLoading = false
                    isLibraryLoaded = true
                }
            }.start()
        }

        //Options
        optionSync = OptionsItem(R.drawable.icon_sync, R.string.sync_title) {
            //Block action if library is filtered
            if (Library.isFiltered) {
                Orion.snack(this@HomeActivity, R.string.home_error_remove_filters)
                return@OptionsItem
            }

            //Open sync
            startActivity(Intent(this, SyncActivity::class.java))
        }

        optionSettings = OptionsItem(R.drawable.icon_settings, R.string.settings_title) {
            //Open sync
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        optionSorting = OptionsItem(R.drawable.icon_sort, R.string.dialog_sorting_title) {
            //Create dialog
            SortingDialog(this, listOf(
                SortingItem(SortMethod.Date, SortDirection.Ascending),
                SortingItem(SortMethod.Date, SortDirection.Descending),
                SortingItem(SortMethod.Name, SortDirection.Ascending),
                SortingItem(SortMethod.Name, SortDirection.Descending),
                SortingItem(SortMethod.Size, SortDirection.Ascending),
                SortingItem(SortMethod.Size, SortDirection.Descending)
            )) { _ ->
                //Scroll to top
                homeList.smoothScrollToPosition(0)
            }.buildAndShow()
        }

        optionFilters = OptionsItem(R.drawable.icon_filter, R.string.dialog_filters_title) {
            //Create dialog
            FiltersDialog(this, listOf(
                FiltersItem(R.drawable.icon_filter_all, R.string.dialog_filters_option_all, "*/*"),
                FiltersItem(R.drawable.icon_filter_image, R.string.dialog_filters_option_images, "image/*"),
                FiltersItem(R.drawable.icon_filter_video, R.string.dialog_filters_option_videos, "video/*")
            )) { _ ->
                //Scroll to top
                homeList.smoothScrollToPosition(0)
            }.buildAndShow()
        }
    }

    override fun onAfterInitViews() {
        //Hide UI
        homeList.visibility = View.GONE
        navbarLayout.visibility = View.GONE

        //Init components
        initHomeList()

        //Check for updates
        if (Storage.getBool(StoragePairs.APP_UPDATE_CHECK)) {
            Thread {
                val latestUpdate = UpdateChecker.checkForUpdates()
                if (latestUpdate != null) {
                    runOnUiThread {
                        UpdateDialog(this, latestUpdate).buildAndShow()
                    }
                }
            }.start()
        }
    }

    override fun onRequestPermission(permission: PermissionType) {
        when (permission) {
            //Storage
            PermissionType.Storage -> {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            //Media
            PermissionType.Media -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissionMedia.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO))
                } else {
                    requestPermissionMedia.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                }
            }
            //Other
            else -> {}
        }
    }

    override fun onPermissionsGranted() {
        //Already loading or loaded
        if (isLibraryLoading || isLibraryLoaded) return
        isLibraryLoading = true

        //Check intent
        val filter: String
        val intent = getIntent()
        val action = intent.action
        if (action == Intent.ACTION_GET_CONTENT || action == Intent.ACTION_PICK) {
            //An app requested to select an item
            isPicking = true
            filter = intent.type ?: "*/*"
        } else {
            //Regular open
            isPicking = false
            filter = "*/*"
        }
        navbarOptions.visibility = if (isPicking) View.GONE else View.VISIBLE

        //Show loading indicator
        loadingIndicator.visibility = View.VISIBLE

        //Load library
        Thread {
            //Load library
            Library.loadLibrary(this, filter)

            //Show list
            runOnUiThread {
                //Hide loading indicator
                loadingIndicator.visibility = View.GONE

                //Show UI
                Orion.animateShow(homeList)
                Orion.animateShow(navbarLayout)

                //Refresh albums list
                searchHelper.filter()
            }

            //Mark as loaded
            isLibraryLoaded = true
            isLibraryLoading = false
        }.start()

        //Mark as init
        isInit = true
    }

    override fun onDestroy() {
        super.onDestroy()

        //Remove events
        Library.removeOnRefreshEvent(onRefresh)
        Library.removeOnActionEvent(onAction)
    }

    override fun onResume() {
        super.onResume()

        //Check for permissions
        if (!permissionManager.hasAllPermissions) {
            if (!permissionManager.hasPermission(PermissionType.Storage)) {
                permissionManager.notifyPermissionChanged(PermissionType.Storage)
            }
            checkPermissions()
            return
        }

        //Not init or library not loaded
        if (!isInit || !isLibraryLoaded) return

        //Update list items per row
        updateListItemsPerRow()
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

        //Refresh
        runOnUiThread {
            //Refresh list
            searchHelper.refresh()

            //Update subtitle
            updateNavbarTitle()
        }
    }

    private fun manageAction(action: ActionResult) {
        //Check if searching or albums were sorted
        if (searchHelper.isFiltered || action.hasSortedAlbumsList) {
            //Searching or albums were sorted -> Refresh
            searchHelper.refresh()
        } else {
            //Check if albums were deleted
            if (!action.removedAlbumIndexes.isEmpty()) {
                //Albums were deleted -> Notify items removed
                for (albumIndex in action.removedAlbumIndexes) {
                    //Remove album from list
                    homeAlbumsList.removeAt(albumIndex)

                    //Notify position removed
                    homeAdapter.notifyItemRemoved(homeAdapter.getPositionFromIndex(albumIndex))
                }
            }

            //Check if albums were sorted
            var specialAlbumWasModified = false
            if (!action.modifiedAlbums.isEmpty()) {
                //Albums were modified -> Notify items changed
                for (album in action.modifiedAlbums) {
                    //Check if album is special
                    if (album.isSpecial) {
                        specialAlbumWasModified = true
                        continue
                    }

                    //Notify album position changed
                    val albumIndex = homeAdapter.getIndexFromAlbum(album)
                    homeAdapter.notifyItemChanged(homeAdapter.getPositionFromIndex(albumIndex))
                }
            }

            //Update header if an special album was modified
            if (specialAlbumWasModified) {
                homeAdapter.notifyItemChanged(0)
            }
        }
    }

    //Home
    private fun initHomeList() {
        //Init home layout manager
        homeLayoutManager = GridHeaderLayoutManager(this, listItemsPerRow) { position ->
            homeAdapter.getItemViewType(position) == 0
        }
        homeList.setLayoutManager(homeLayoutManager)
        homeDecorator = GridListSeparator(20, homeLayoutManager.spanCount, 1)
        homeList.addItemDecoration(homeDecorator)
        (homeList.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        //Init home adapter
        homeAdapter = HomeAdapter(this, homeAlbumsList)
        homeAdapter.onClick = HomeAdapter.ClickListener { view: View, album: Album ->
            //Not available
            if (isWorking) return@ClickListener

            //Create open animation
            val startX = view.left + (view.width / 2)
            val startY = view.top + (view.height / 2)
            val options = ActivityOptions.makeScaleUpAnimation(
                //The view to scale from
                homeList,
                //Starting point
                startX, startY,
                //Starting size
                0, 0
            )

            //Prepare intent info
            val intent = Intent(this, AlbumActivity::class.java)
            when (album) {
                Library.trash ->
                    intent.putExtra("albumName", "trash")
                Library.all ->
                    intent.putExtra("albumName", "all")
                Library.favourites ->
                    intent.putExtra("albumName", "favourites")
                else ->
                    intent.putExtra("albumIndex", Library.albums.indexOf(album))
            }

            //Open album
            if (isPicking) {
                //External item picker
                intent.putExtra("isPicking", true)
                pickerLauncher.launch(intent)
            } else {
                //Regular open
                startActivity(intent, options.toBundle())
                searchHelper.toggleLayout(false)
            }
        }
        homeAdapter.onLongClick = HomeAdapter.ClickListener { view: View, album: Album ->
            //Not available
            if (isWorking) return@ClickListener

            //Pin album
            Storage.putString(StoragePairs.HOME_PINNED_ALBUM, album.albumPath)
            homeAdapter.notifyItemChanged(0)
            Orion.snack(this, getString(R.string.home_pinned_album, album.name), duration = Orion.snackDurationLong)
        }
        homeList.setAdapter(homeAdapter)

        //Init home fast scroller
        homeFastScroller = FastScrollerBuilder(homeList)
            .setHasHeader(true)
            .build()

        //Init search helper
        searchHelper.init(this, backManager, navbarLayout, searchLayout, searchInput, homeList, this@HomeActivity::onBeforeSearchFilter, this@HomeActivity::onSearchFilter, this@HomeActivity::onAfterSearchFilter)
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
        options.add(OptionsGroup(mutableListOf<OptionsItem>().apply {
            add(optionSync)
            add(optionSettings)
        }))
        options.add(OptionsGroup(mutableListOf<OptionsItem>().apply {
            add(optionSorting)
            add(optionFilters)
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

    //List grid
    private val listItemsPerRow: Int get() {
        //Check if in horizontal orientation
        val isHorizontal = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        //Get portrait aspect ratio
        val metrics = resources.displayMetrics
        val ratio = (metrics.widthPixels.toFloat() / metrics.heightPixels.toFloat())

        //Get portrait items per row
        val itemsPerRow = Storage.getInt(StoragePairs.HOME_ITEMS_PER_ROW)

        //Return items per row for current orientation
        return if (isHorizontal) (itemsPerRow * ratio).toInt() else itemsPerRow
    }

    private fun updateListItemsPerRow() {
        val newItemsPerRow = listItemsPerRow
        if (homeLayoutManager.spanCount != newItemsPerRow) {
            homeLayoutManager.setSpanCount(newItemsPerRow)
            homeDecorator.spanCount = newItemsPerRow
            homeList.invalidateItemDecorations()
        }
    }

    //Navbar
    private fun updateNavbarTitle() {
        //Check if a search query is applied
        val isSearching = searchHelper.isFiltered

        //Check if a filter is applied
        val filter = Library.filter
        val isFiltered = filter != "*/*"

        //Toggle text visibility
        val isTitleVisible = isSearching || isFiltered
        val isSubtitleVisible = isSearching && isFiltered
        navbarTitle.visibility = if (isTitleVisible) View.VISIBLE else View.GONE
        navbarSubtitle.visibility = if (isSubtitleVisible) View.VISIBLE else View.GONE
        if (!isTitleVisible) return

        //Get search text
        val searchText = getString(R.string.home_search_navbar, searchHelper.currentQuery)

        //Get filter text
        val filterText = StringBuilder()
        if (isFiltered) {
            //Parse filter
            val parts = filter.split("/")
            val type = parts[0]
            val format = parts[1]
            filterText.append(getString(when (type) {
                "image" -> R.string.home_filtered_images
                "video" -> R.string.home_filtered_videos
                else -> R.string.home_filtered_custom
            }))
            if (format != "*") filterText.append(" ($format)")
        }

        //Update title & subtitle
        navbarTitle.text = if (isSearching) searchText else filterText.toString()
        navbarSubtitle.text = if (isFiltered) filterText.toString() else ""
    }

    //Search
    private fun clearSearchInput() {
        ignoreSearchInput = true
        searchInput.setText("")
        ignoreSearchInput = false
    }

    private fun onBeforeSearchFilter(isFiltering: Boolean, query: String): Boolean {
        //Clear text
        if (!isFiltering) clearSearchInput()

        //Filter
        return true
    }

    private fun onSearchFilter(isFiltering: Boolean, query: String): MutableList<Album> {
        //Lowercase query
        val lowercaseQuery = query.lowercase()

        //Filter albums
        val temp = ArrayList<Album>()
        for (album in Library.albums) {
            if (album.name.lowercase().contains(lowercaseQuery)) {
                temp.add(album)
            }
        }
        return temp
    }

    private fun onAfterSearchFilter(isFiltering: Boolean, query: String, albums: MutableList<Album>) {
        //Update albums list
        homeAlbumsList.clear()
        homeAlbumsList.addAll(albums)

        //Update navbar
        updateNavbarTitle()
    }

}
