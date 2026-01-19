package com.botpa.turbophotos.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

import com.botpa.turbophotos.R
import com.botpa.turbophotos.album.AlbumActivity
import com.botpa.turbophotos.gallery.Album
import com.botpa.turbophotos.gallery.GalleryActivity
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.Library.RefreshEvent
import com.botpa.turbophotos.gallery.Library.ActionEvent
import com.botpa.turbophotos.gallery.actions.Action
import com.botpa.turbophotos.gallery.options.OptionsAdapter
import com.botpa.turbophotos.gallery.options.OptionsItem
import com.botpa.turbophotos.home.filters.DialogFilters
import com.botpa.turbophotos.home.filters.Filter
import com.botpa.turbophotos.settings.SettingsActivity
import com.botpa.turbophotos.settings.SettingsPairs
import com.botpa.turbophotos.sync.SyncActivity
import com.botpa.turbophotos.util.BackManager
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

@SuppressLint("SetTextI18n", "NotifyDataSetChanged")
class HomeActivity : GalleryActivity() {

     /*$   /$$
    | $$  | $$
    | $$  | $$  /$$$$$$  /$$$$$$/$$$$   /$$$$$$
    | $$$$$$$$ /$$__  $$| $$_  $$_  $$ /$$__  $$
    | $$__  $$| $$  \ $$| $$ \ $$ \ $$| $$$$$$$$
    | $$  | $$| $$  | $$| $$ | $$ | $$| $$_____/
    | $$  | $$|  $$$$$$/| $$ | $$ | $$|  $$$$$$$
    |__/  |__/ \______/ |__/ |__/ |__/ \______*/

    //Activity
    private lateinit var backManager: BackManager
    private var isLibraryLoaded = false
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
    private var shouldCheckPermissions = false
    private var hasPermissionWrite = false
    private var hasPermissionMedia = false
    private var hasPermissionNotifications = false
    private val requestPermissionNotifications = registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
        hasPermissionNotifications = isGranted
        checkPermissions()
    }

    private lateinit var permissionLayout: View
    private lateinit var permissionWrite: View
    private lateinit var permissionMedia: View
    private lateinit var permissionNotifications: View

    //List
    private lateinit var homeLayoutManager: GridLayoutManager
    private lateinit var homeAdapter: HomeAdapter

    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var homeList: FastScrollRecyclerView

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
    private lateinit var optionSync: OptionsItem
    private lateinit var optionSettings: OptionsItem
    private lateinit var optionFilters: OptionsItem

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.home_screen)

        //Enable HDR
        window.colorMode = ActivityInfo.COLOR_MODE_HDR

        //Add events
        Library.addOnRefreshEvent(onRefresh)
        Library.addOnActionEvent(onAction)

        //Init components
        Storage.init(this@HomeActivity)
        backManager = BackManager(this@HomeActivity, onBackPressedDispatcher)
        initViews()
        initListeners()
        initAdapters()

        //Mark as init
        isInit = true

        //Check permissions
        checkPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()

        //Remove events
        Library.removeOnRefreshEvent(onRefresh)
        Library.removeOnActionEvent(onAction)
    }

    override fun onResume() {
        super.onResume()

        //Home not init
        if (!isInit) return

        //Reload
        if (shouldReloadOnResume) {
            shouldReloadOnResume = false
            recreate()
            return
        }

        //Check for permissions
        if (shouldCheckPermissions) {
            shouldCheckPermissions = false
            checkPermissions()
            return
        }

        //Library not loaded
        if (!isLibraryLoaded) return

        //Update horizontal item count
        updateHorizontalItemCount()

        //Refresh albums (check for new items)
        Library.loadLibrary(this@HomeActivity, false)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        //Update horizontal item count
        updateHorizontalItemCount()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray, deviceId: Int) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)

        //Check permissions again
        checkPermissions()
    }

    private fun checkPermissions() {
        //Update current permissions
        if (Environment.isExternalStorageManager()) {
            hasPermissionWrite = true
            permissionWrite.alpha = 0.5f
        }

        if (ContextCompat.checkSelfPermission(this@HomeActivity, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this@HomeActivity, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED) {
            hasPermissionMedia = true
            permissionMedia.alpha = 0.5f
        }

        if (NotificationManagerCompat.from(this@HomeActivity).areNotificationsEnabled()) {
            hasPermissionNotifications = true
            permissionNotifications.alpha = 0.5f
        }

        //Check if permissions are granted
        if (hasPermissionWrite && hasPermissionMedia && hasPermissionNotifications) {
            //Hide permission layout
            permissionLayout.visibility = View.GONE

            //Init activity
            loadLibrary()
        } else {
            //Show permission layout
            permissionLayout.visibility = View.VISIBLE

            //Add request permission button listeners
            permissionWrite.setOnClickListener { view: View ->
                //Already has permission
                if (hasPermissionWrite) return@setOnClickListener

                //Ask for permission
                shouldCheckPermissions = true
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }

            permissionMedia.setOnClickListener { view: View ->
                //Already has permission
                if (hasPermissionMedia) return@setOnClickListener

                //Ask for permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO),
                        0
                    )
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        0
                    )
                }
            }

            permissionNotifications.setOnClickListener { view: View ->
                //Already has permission
                if (hasPermissionNotifications) return@setOnClickListener

                //Ask for permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissionNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun loadLibrary() {
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

        //Load albums
        Thread {
            //Load albums
            Library.loadLibrary(this@HomeActivity, filter)

            //Show list
            runOnUiThread {
                //Hide loading indicator
                loadingIndicator.visibility = View.GONE

                //Show list
                Orion.showAnim(homeList)

                //Reload albums list
                homeAdapter.notifyDataSetChanged()
            }

            //Mark as loaded
            isLibraryLoaded = true
        }.start()
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
            //Check if trash was added, removed or updated
            when (action.trashAction) {
                Action.TRASH_ADDED -> homeAdapter.notifyItemInserted(0)
                Action.TRASH_REMOVED -> homeAdapter.notifyItemRemoved(0)
                Action.TRASH_UPDATED -> homeAdapter.notifyItemChanged(0)
            }

            //Check if albums were deleted
            if (!action.removedIndexesInAlbums.isEmpty()) {
                //Albums were deleted -> Notify items removed
                for (albumIndex in action.removedIndexesInAlbums) {
                    //Notify position removed
                    homeAdapter.notifyItemRemoved(homeAdapter.getPositionFromIndex(albumIndex))
                }
            }

            //Check if albums were sorted
            if (!action.modifiedAlbums.isEmpty()) {
                //Albums were sorted -> Notify items changed
                for (album in action.modifiedAlbums) {
                    //Get album index
                    val albumIndex = homeAdapter.getIndexFromAlbum(album)
                    if (albumIndex < 0 && !album.isEspecial) continue

                    //Notify position changed
                    homeAdapter.notifyItemChanged(homeAdapter.getPositionFromIndex(albumIndex))
                }
            }
        }
    }

    //Components
    private fun initViews() {
        //Permissions
        permissionLayout = findViewById(R.id.permissionLayout)
        permissionWrite = findViewById(R.id.permissionWrite);
        permissionMedia = findViewById(R.id.permissionMedia);
        permissionNotifications = findViewById(R.id.permissionNotifications);

        //Navbar
        navbarSubtitle = findViewById(R.id.navbarSubtitle)
        navbarOptions = findViewById(R.id.navbarOptions)

        //Options
        optionsLayout = findViewById(R.id.optionsLayout)
        optionsList = findViewById(R.id.optionsList)

        //List
        refreshLayout = findViewById(R.id.refreshLayout)
        homeList = findViewById(R.id.list)

        //Loading indicator
        loadingIndicator = findViewById(R.id.loadingIndicator)

        //System
        systemNotificationsBar = findViewById(R.id.notificationsBar)
        systemNavigationBar = findViewById(R.id.navigationBar)


        //Insets (keyboard & system bars)
        Orion.addInsetsChangedListener(
            findViewById(R.id.content),
            intArrayOf(WindowInsetsCompat.Type.systemBars())
        ) { view: View, insets: Insets, duration: Float ->
            refreshLayout.setProgressViewOffset(false, 0, insets.top + 50)
            homeList.setPadding(0, insets.top, 0, homeList.paddingBottom + insets.bottom)
        }
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
        navbarOptions.setOnClickListener { view: View -> toggleOptions(true) }

        //Options
        optionsLayout.setOnClickListener { view: View -> toggleOptions(false) }

        optionSync = OptionsItem(R.drawable.backup, "Sync") {
            //Not loaded
            if (!isLibraryLoaded) return@OptionsItem

            //Open sync
            startActivity(Intent(this@HomeActivity, SyncActivity::class.java))
        }

        optionSettings = OptionsItem(R.drawable.settings, "Settings") {
            //Not loaded
            if (!isLibraryLoaded) return@OptionsItem

            //Open sync
            startActivity(Intent(this@HomeActivity, SettingsActivity::class.java))
        }

        optionFilters = OptionsItem(R.drawable.filter, "Filters") {
            //Create filters list
            val filters = listOf(
                Filter(R.drawable.gallery_all, "All items", "*/*"),
                Filter(R.drawable.gallery_image, "Only images", "image/*"),
                Filter(R.drawable.gallery_video, "Only videos", "video/*")
            )

            //Create dialog
            DialogFilters(this@HomeActivity, filters).buildAndShow()
        }

        //List
        refreshLayout.setOnRefreshListener {
            //Reload library
            Library.loadLibrary(this@HomeActivity, true) //Fully refresh

            //Stop refreshing
            refreshLayout.isRefreshing = false
        }
    }

    private fun initAdapters() {
        //Init home layout manager
        homeLayoutManager = GridLayoutManager(this@HomeActivity, this.horizontalItemCount)
        homeList.setLayoutManager(homeLayoutManager)

        //Init home adapter
        homeAdapter = HomeAdapter(this@HomeActivity, Library.albums)
        homeAdapter.setOnClickListener { view: View, album: Album ->
            //Create open animation
            val startX = view.left + (view.width / 2)
            val startY = view.top + (view.height / 2)
            val options = ActivityOptions.makeScaleUpAnimation(
                homeList,  //The view to scale from
                startX, startY,  //Starting point
                0, 0 //Starting size
            )

            //Prepare intent info
            val intent = Intent(this@HomeActivity, AlbumActivity::class.java)
            when (album) {
                Library.trash ->
                    intent.putExtra("albumName", "trash")
                Library.all ->
                    intent.putExtra("albumName", "all")
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

        //Init options layout manager
        optionsList.setLayoutManager(LinearLayoutManager(this@HomeActivity))

        //Init options adapter
        optionsAdapter = OptionsAdapter(this@HomeActivity, options)
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
            //Update options list
            options.clear()
            options.add(optionSync)
            options.add(optionSettings)
            if (!isPicking) {
                options.add(optionSeparator)
                options.add(optionFilters)
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

    //List grid
    private val horizontalItemCount: Int get() {
        val isHorizontal = getResources().configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val metrics = getResources().displayMetrics
        val ratio = (metrics.widthPixels.toFloat() / metrics.heightPixels.toFloat())

        //Get size for portrait
        val size = Storage.getInt(SettingsPairs.HOME_ITEMS_PER_ROW)

        //Return size for current orientation
        return if (isHorizontal) (size * ratio).toInt() else size
    }

    private fun updateHorizontalItemCount() {
        val newHorizontalItemCount = this.horizontalItemCount
        if (homeLayoutManager.spanCount != newHorizontalItemCount) {
            homeLayoutManager.setSpanCount(newHorizontalItemCount)
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

    //Static
    companion object {

        private var shouldReloadOnResume = false

        @JvmStatic
        fun reloadOnResume() {
            //Reload on resume
            shouldReloadOnResume = true
        }

    }

}