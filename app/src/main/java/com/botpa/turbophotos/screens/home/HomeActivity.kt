package com.botpa.turbophotos.screens.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Album
import com.botpa.turbophotos.gallery.BaseActivity
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.Library.ActionEvent
import com.botpa.turbophotos.gallery.Library.RefreshEvent
import com.botpa.turbophotos.gallery.StoragePairs
import com.botpa.turbophotos.gallery.actions.Action
import com.botpa.turbophotos.gallery.fastscroller.FastScroller
import com.botpa.turbophotos.gallery.fastscroller.FastScrollerBuilder
import com.botpa.turbophotos.gallery.options.OptionsGroup
import com.botpa.turbophotos.gallery.options.OptionsItem
import com.botpa.turbophotos.gallery.options.OptionsManager
import com.botpa.turbophotos.gallery.permissions.PermissionType
import com.botpa.turbophotos.gallery.views.GridListSeparator
import com.botpa.turbophotos.screens.album.AlbumActivity
import com.botpa.turbophotos.screens.home.filters.Filter
import com.botpa.turbophotos.screens.home.filters.FiltersDialog
import com.botpa.turbophotos.screens.settings.SettingsActivity
import com.botpa.turbophotos.screens.sync.SyncActivity
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage

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

    private var isLibraryLoaded = false
    private var isLibraryLoading = false
    private var isInit = false

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

    //Permissions
    private val requestPermissionMedia = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted: Map<String, Boolean> ->
        permissionManager.notifyPermissionChanged(PermissionType.Media)
        checkPermissions()
    }

    private lateinit var permissionLayout: View
    private lateinit var permissionStorage: View
    private lateinit var permissionMedia: View

    //List
    private lateinit var homeLayoutManager: GridLayoutManager
    private lateinit var homeDecorator: GridListSeparator
    private lateinit var homeAdapter: HomeAdapter

    private lateinit var homeRefreshLayout: SwipeRefreshLayout
    private lateinit var homeList: RecyclerView
    private lateinit var homeFastScroller: FastScroller

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
    private lateinit var navbarSubtitle: TextView
    private lateinit var navbarOptions: View

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
        //Permissions
        permissionLayout = findViewById(R.id.permissionLayout)
        permissionStorage = findViewById(R.id.permissionStorage)
        permissionMedia = findViewById(R.id.permissionMedia)

        //Navbar
        navbarSubtitle = findViewById(R.id.navbarSubtitle)
        navbarOptions = findViewById(R.id.navbarOptions)

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
            homeRefreshLayout.setProgressViewOffset(false, 0, insets.top + 50)
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

        //Insets (options layout)
        Orion.addInsetsChangedListener(
            optionsManager.layout,
            intArrayOf(WindowInsetsCompat.Type.systemBars())
        )
    }

    override fun onInitListeners() {
        //Navbar
        navbarOptions.setOnClickListener { view: View -> optionsManager.toggle(true) }

        //List
        homeRefreshLayout.setOnRefreshListener {
            //Reload library
            Library.loadLibrary(this, true)

            //Stop refreshing
            homeRefreshLayout.isRefreshing = false
        }

        //Options
        optionSync = OptionsItem(R.drawable.sync, "Sync") {
            //Not loaded
            if (!isLibraryLoaded) return@OptionsItem

            //Open sync
            startActivity(Intent(this, SyncActivity::class.java))
        }

        optionSettings = OptionsItem(R.drawable.settings, "Settings") {
            //Not loaded
            if (!isLibraryLoaded) return@OptionsItem

            //Open sync
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        optionFilters = OptionsItem(R.drawable.filter, "Filters") {
            //Create filters list
            val filters = listOf(
                Filter(R.drawable.gallery_all, "All items", "*/*"),
                Filter(R.drawable.gallery_image, "Only images", "image/*"),
                Filter(R.drawable.gallery_video, "Only videos", "video/*")
            )

            //Create dialog
            FiltersDialog(this, filters).buildAndShow()
        }
    }

    override fun onAfterInitViews() {
        //Init components
        initHomeList()
    }

    override fun onPermissionsGranted() {
        //Hide permissions layout
        permissionLayout.visibility = View.GONE

        //Already loading or loaded
        if (isLibraryLoading || isLibraryLoaded) return
        isLibraryLoading = true

        //Hide list
        homeList.visibility = View.GONE

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

                //Show list
                Orion.animateShow(homeList)

                //Reload albums list
                homeAdapter.notifyDataSetChanged()
            }

            //Mark as loaded
            isLibraryLoaded = true
            isLibraryLoading = false
        }.start()

        //Mark as init
        isInit = true
    }

    override fun onPermissionsDenied() {
        //Show permissions layout
        permissionLayout.visibility = View.VISIBLE

        //Update buttons
        if (permissionManager.hasPermission(PermissionType.Storage)) {
            permissionStorage.isEnabled = false
        } else {
            permissionStorage.setOnClickListener { view: View ->
                //Already has permission
                if (permissionManager.hasPermission(PermissionType.Storage)) return@setOnClickListener

                //Ask for permission
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
        }

        if (permissionManager.hasPermission(PermissionType.Media)) {
            permissionMedia.isEnabled = false
        } else {
            permissionMedia.setOnClickListener { view: View ->
                //Already has permission
                if (permissionManager.hasPermission(PermissionType.Media)) return@setOnClickListener

                //Ask for permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissionMedia.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO))
                } else {
                    requestPermissionMedia.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                }
            }
        }
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

        //Check for permissions
        if (!permissionManager.hasAllPermissions) {
            if (!permissionManager.hasPermission(PermissionType.Storage)) {
                permissionManager.notifyPermissionChanged(PermissionType.Storage)
            }
            checkPermissions()
            return
        }

        //Library not loaded
        if (!isLibraryLoaded) return

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
        runOnUiThread {
            //Didn't update
            if (!updated) return@runOnUiThread

            //Refresh list
            homeAdapter.notifyDataSetChanged()

            //Update subtitle
            updateNavbarSubtitle()
        }
    }

    private fun manageAction(action: Action) {
        //No action
        if (action.isOfType(Action.TYPE_NONE)) return

        //Check if albums list was changed
        if (action.hasSortedAlbumsList) {
            //Sorted albums list -> Notify all
            homeAdapter.notifyDataSetChanged()
        } else {
            //Check if albums were deleted
            if (!action.removedIndexesInAlbums.isEmpty()) {
                //Albums were deleted -> Notify items removed
                for (albumIndex in action.removedIndexesInAlbums) {
                    //Notify position removed
                    homeAdapter.notifyItemRemoved(homeAdapter.getPositionFromIndex(albumIndex))
                }
            }

            //Check if albums were sorted
            var specialAlbumWasModified = false
            if (!action.modifiedAlbums.isEmpty()) {
                //Albums were sorted -> Notify items changed
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
            if (specialAlbumWasModified) homeAdapter.notifyItemChanged(0)
        }
    }

    //Home
    private fun initHomeList() {
        //Init home layout manager
        homeLayoutManager = GridLayoutManager(this, listItemsPerRow)
        homeLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (homeAdapter.getItemViewType(position) == 0) homeLayoutManager.spanCount else 1
            }
        }
        homeList.setLayoutManager(homeLayoutManager)
        homeDecorator = GridListSeparator(20, homeLayoutManager.spanCount, 1)
        homeList.addItemDecoration(homeDecorator)

        //Init home adapter
        homeAdapter = HomeAdapter(this, Library.albums)
        homeAdapter.onClick = { view: View, album: Album ->
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
            }
        }
        homeList.setAdapter(homeAdapter)

        //Init home fast scroller
        homeFastScroller = FastScrollerBuilder(homeList)
            .setHasHeader(true)
            .build()
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
        val isHorizontal = getResources().configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        //Get portrait aspect ratio
        val metrics = getResources().displayMetrics
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
    private fun updateNavbarSubtitle() {
        //Check if a filter is applied & toggle subtitle
        val filter = Library.libraryFilter
        val isFiltered = filter != "*/*"
        navbarSubtitle.visibility = if (isFiltered) View.VISIBLE else View.GONE
        if (!isFiltered) return

        //Parse filter & update subtitle
        val parts = filter.split("/")
        val type = parts[0]
        val format = parts[1]
        val subtitle = StringBuilder("Showing only ")
        when (type) {
            "image" -> subtitle.append("images ")
            "video" -> subtitle.append("videos ")
            else -> subtitle.append("custom ")
        }
        if (format != "*") subtitle.append("($format)")
        navbarSubtitle.text = subtitle.toString()
    }

}
