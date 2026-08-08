package com.botpa.turbophotos.screens.settings

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.botpa.turbophotos.BuildConfig
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.Link
import com.botpa.turbophotos.gallery.modals.AlbumsDialog
import com.botpa.turbophotos.gallery.modals.ExplorerDialog
import com.botpa.turbophotos.gallery.modals.BulletPointsDialog
import com.botpa.turbophotos.gallery.views.Group
import com.botpa.turbophotos.gallery.views.GroupDivider
import com.botpa.turbophotos.gallery.views.GroupItem
import com.botpa.turbophotos.gallery.views.GroupItems
import com.botpa.turbophotos.gallery.views.GroupTitle
import com.botpa.turbophotos.gallery.views.Layout
import com.botpa.turbophotos.gallery.views.SimpleButton
import com.botpa.turbophotos.theme.CoonTheme

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : AppCompatActivity() {

    //View model
    private val view: SettingsViewModel by viewModels()


    //App
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //Content
        setContent {
            CoonTheme {
                SettingsLayout()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        //Check if library needs to be reset
        if (view.reloadLibraryOnExit) Library.loadLibrary(this, true)
    }

    //Routes
    object SettingsRoutes {
        const val MAIN = "main"
        const val APP = "app"
        const val METADATA = "metadata"
        const val HOME_SCREEN = "home_screen"
        const val ALBUM_SCREEN = "about_screen"
        const val VIEWER_SCREEN = "viewer_screen"
        const val VIDEO_SCREEN = "video_screen"
    }

    //Layout
    @Composable
    private fun SettingsLayout() {
        //Get useful stuff
        val activity = this
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current

        //Backup actions
        val onCreateBackup = remember {
            {
                //Show select folder dialog
                ExplorerDialog(
                    context = activity,
                    isSelectingFiles = false,
                    onSelect = { folder ->
                        view.createSettingsBackup(context, folder)
                    }
                ).buildAndShow()

                //Feedback toast
                Toast.makeText(activity, R.string.settings_message_backup_create_select, Toast.LENGTH_SHORT).show()
            }
        }
        val onChooseBackupFile = remember {
            {
                //Show select file dialog
                ExplorerDialog(
                    context = activity,
                    isSelectingFiles = true,
                    fileExtension = "json",
                    onSelect = { file ->
                        view.restoreSettingsBackup(context, activity, file)
                    }
                ).buildAndShow()

                //Feedback toast
                Toast.makeText(activity, R.string.settings_message_backup_restore_select, Toast.LENGTH_SHORT).show()
            }
        }

        //Links actions
        val onShowLinksInfo = remember {
            {
                //Create info dialog
                BulletPointsDialog(
                    context,
                    title = R.string.settings_metadata_links_info_dialog_title,
                    text = R.string.settings_metadata_links_info_dialog_description,
                    points = listOf(
                        R.string.settings_metadata_links_info_dialog_point1,
                        R.string.settings_metadata_links_info_dialog_point2
                    )
                ).buildAndShow()
            }
        }
        val onChooseLinkAlbum = remember<(Int) -> Unit> {
            { index ->
                //Show select album dialog
                AlbumsDialog(
                    context = context,
                    albums = Library.albums,
                    onSelectAlbum = { album ->
                        //Choose album folder
                        val folder = album.imagesFolder ?: return@AlbumsDialog
                        view.updateLinkAlbumFolder(activity, index, folder)
                    },
                    onSelectFolder = { folder ->
                        //Choose folder
                        view.updateLinkAlbumFolder(activity, index, folder)
                    }
                ).buildAndShow()

                //Feedback toast
                Toast.makeText(activity, R.string.settings_message_link_album_select, Toast.LENGTH_SHORT).show()
            }
        }
        val onChooseLinkMetadata = remember<(Int, Link) -> Unit> {
            { index, link ->
                //Check if album folder exists
                if (!link.albumFolder.exists()) {
                    //Feedback toast
                    Toast.makeText(activity, R.string.settings_error_link_add_first, Toast.LENGTH_SHORT).show()
                } else {
                    //Show select file dialog
                    ExplorerDialog(
                        context = context,
                        isSelectingFiles = true,
                        fileExtension = "json",
                        onSelect = { file ->
                            //Choose file
                            view.updateLinkMetadataFile(index, file)
                        }
                    ).buildAndShow()

                    //Feedback toast
                    Toast.makeText(activity, R.string.settings_message_link_metadata_select, Toast.LENGTH_SHORT).show()
                }
            }
        }
        val onAddLink = remember {
            {
                view.addLink(activity)
            }
        }

        //Navigation
        val navController = rememberNavController()

        //Layout
        Layout(R.string.settings_title) {
            NavHost(
                navController = navController,
                startDestination = SettingsRoutes.MAIN,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(500)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(500)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(500)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(500)
                    )
                }
            ) {
                //Get padding
                val paddingValues = it

                //Screen selection
                composable(SettingsRoutes.MAIN) {
                    SettingsMainLayout(
                        paddingValues,
                        onCategoryClick = { route -> navController.navigate(route) }
                    )
                }
                composable(SettingsRoutes.APP) {
                    SettingsAppLayout(
                        paddingValues,
                        uriHandler,
                        onCreateBackup,
                        onChooseBackupFile
                    )
                }
                composable(SettingsRoutes.METADATA) {
                    SettingsMetadataLayout(
                        paddingValues,
                        onShowLinksInfo,
                        onChooseLinkAlbum,
                        onChooseLinkMetadata,
                        onAddLink
                    )
                }
                composable(SettingsRoutes.HOME_SCREEN) {
                    SettingsHomeScreenLayout(paddingValues)
                }
                composable(SettingsRoutes.ALBUM_SCREEN) {
                    SettingsAlbumScreenLayout(paddingValues)
                }
                composable(SettingsRoutes.VIEWER_SCREEN) {
                    SettingsViewerScreenLayout(paddingValues)
                }
                composable(SettingsRoutes.VIDEO_SCREEN) {
                    SettingsVideoPlayerLayout(paddingValues)
                }
            }
        }
    }

    @Composable
    private fun SettingsMainLayout(paddingValues: PaddingValues, onCategoryClick: (String) -> Unit) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        ) {
            //General
            item {
                //Title
                GroupTitle(R.string.settings_main_general_title)

                //Items
                Group {
                    GroupItems {
                        //App
                        GroupItem(
                            onClick = { onCategoryClick(SettingsRoutes.APP) }
                        )  {
                            SettingsItem(
                                title = R.string.settings_main_general_app_title,
                                description = R.string.settings_main_general_app_description
                            )
                        }

                        //Divider
                        GroupDivider()

                        //Metadata
                        GroupItem(
                            onClick = { onCategoryClick(SettingsRoutes.METADATA) }
                        )  {
                            SettingsItem(
                                title = R.string.settings_main_general_metadata_title,
                                description = R.string.settings_main_general_metadata_description
                            )
                        }
                    }
                }
            }

            //Screens
            item {
                //Title
                GroupTitle(R.string.settings_main_screens_title)

                //Items
                Group {
                    GroupItems {
                        //Home screen
                        GroupItem(
                            onClick = { onCategoryClick(SettingsRoutes.HOME_SCREEN) }
                        )  {
                            SettingsItem(
                                title = R.string.settings_main_screens_home_title,
                                description = R.string.settings_main_screens_home_description
                            )
                        }

                        //Divider
                        GroupDivider()

                        //Album screen
                        GroupItem(
                            onClick = { onCategoryClick(SettingsRoutes.ALBUM_SCREEN) }
                        )  {
                            SettingsItem(
                                title = R.string.settings_main_screens_album_title,
                                description = R.string.settings_main_screens_album_description
                            )
                        }

                        //Divider
                        GroupDivider()

                        //Viewer screen
                        GroupItem(
                            onClick = { onCategoryClick(SettingsRoutes.VIEWER_SCREEN) }
                        )  {
                            SettingsItem(
                                title = R.string.settings_main_screens_viewer_title,
                                description = R.string.settings_main_screens_viewer_description
                            )
                        }

                        //Divider
                        GroupDivider()

                        //Video player
                        GroupItem(
                            onClick = { onCategoryClick(SettingsRoutes.VIDEO_SCREEN) }
                        ) {
                            SettingsItem(
                                title = R.string.settings_main_screens_video_title,
                                description = R.string.settings_main_screens_video_description
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsAppLayout(paddingValues: PaddingValues, uriHandler: UriHandler, onCreateBackup: () -> Unit, onChooseBackupFile: () -> Unit) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        ) {
            //Backups
            item {
                Group {
                    //Title
                    GroupTitle(R.string.settings_app_backup_title)

                    //Items
                    GroupItems {
                        //Create backup
                        GroupItem(
                            onClick = onCreateBackup
                        ) {
                            SettingsItem(
                                title = R.string.settings_app_backup_create_title,
                                description = R.string.settings_app_backup_create_description,
                            )
                        }

                        //Divider
                        GroupDivider()

                        //Recover backup
                        GroupItem(
                            onClick = onChooseBackupFile
                        )  {
                            SettingsItem(
                                title = R.string.settings_app_backup_recover_title,
                                description = R.string.settings_app_backup_recover_description,
                            )
                        }
                    }
                }
            }

            //About
            item {
                //Title
                GroupTitle(R.string.settings_app_about_title)

                //Items
                Group {
                    GroupItems {
                        //Version
                        GroupItem {
                            SettingsItem(
                                title = stringResource(R.string.settings_app_about_version_title),
                                description = stringResource(R.string.settings_app_about_version_description, BuildConfig.VERSION_NAME)
                            )
                        }

                        //Divider
                        GroupDivider()

                        //Developer
                        GroupItem(
                            onClick = { uriHandler.openUri("https://botpa.vercel.app/") }
                        ) {
                            SettingsItem(
                                title = R.string.settings_app_about_developer_title,
                                description = R.string.settings_app_about_developer_description
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.open),
                                    contentDescription = "Portfolio",
                                    contentScale = ContentScale.Fit,
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier
                                        .size(24.dp)
                                )
                            }
                        }

                        //Divider
                        GroupDivider()

                        //Github
                        GroupItem(
                            onClick = { uriHandler.openUri("https://github.com/BOTPanzer/Coon-Gallery") }
                        ) {
                            SettingsItem(
                                title = R.string.settings_app_about_repo_title,
                                description = R.string.settings_app_about_repo_description
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.open),
                                    contentDescription = "GitHub",
                                    contentScale = ContentScale.Fit,
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier
                                        .size(24.dp)
                                )
                            }
                        }

                        //Divider
                        GroupDivider()

                        //Copyright
                        GroupItem {
                            SettingsItem(
                                title = R.string.settings_app_about_copyright_title,
                                description = R.string.settings_app_about_copyright_description
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsMetadataLayout(paddingValues: PaddingValues, onShowLinksInfo: () -> Unit, onChooseLinkAlbum: (Int) -> Unit, onChooseLinkMetadata: (Int, Link) -> Unit, onAddLink: () -> Unit) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        ) {
            item {
                //Metadata
                Group {
                    //Title
                    GroupTitle(R.string.settings_metadata_management_title)

                    //Items
                    GroupItems {
                        //Automatic metadata modification
                        GroupItem {
                            SettingsItem(
                                title = R.string.settings_metadata_management_modification_title,
                                description = R.string.settings_metadata_management_modification_description
                            ) {
                                //Value
                                Switch(
                                    checked = view.appModifyMetadata,
                                    onCheckedChange = { isChecked ->
                                        view.updateAppModifyMetadata(isChecked)
                                    }
                                )
                            }
                        }
                    }
                }

                //Links
                Group {
                    //Title
                    GroupTitle(R.string.settings_metadata_links_title)

                    //Items
                    GroupItems {
                        //Info
                        GroupItem(
                            onClick = onShowLinksInfo
                        ) {
                            SettingsItem(
                                title = R.string.settings_metadata_links_info_title,
                                description = R.string.settings_metadata_links_info_description
                            )
                        }

                        //Divider
                        if (Link.links.isNotEmpty()) GroupDivider()

                        //Links
                        Link.links.forEachIndexed { index, link ->
                            //Add item
                            GroupItem {
                                LinkItem(
                                    index = index,
                                    link = link,
                                    onChooseAlbum = onChooseLinkAlbum,
                                    onChooseMetadata = onChooseLinkMetadata,
                                    onDelete = { index -> view.removeLink(index) }
                                )
                            }

                            //Add divider between items
                            if (index < Link.links.size - 1) GroupDivider()
                        }
                    }

                    //Add link button
                    SimpleButton(
                        text = R.string.settings_metadata_links_add,
                        onClick = onAddLink,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun SettingsHomeScreenLayout(paddingValues: PaddingValues) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        ) {
            item {
                Group {
                    //Title
                    GroupTitle(R.string.settings_home_grid_title)

                    //Items
                    GroupItems {
                        //Items per row
                        GroupItem {
                            SettingsItem(
                                title = stringResource(R.string.settings_home_grid_row_title, view.homeItemsPerRow.toInt()),
                                description = stringResource(R.string.settings_home_grid_row_description)
                            ) {
                                //Value
                                Slider(
                                    value = view.homeItemsPerRow,
                                    onValueChange = { newValue ->
                                        view.homeItemsPerRow = newValue
                                    },
                                    onValueChangeFinished = { view.saveHomeItemsPerRow() },
                                    valueRange = 1f..5f,
                                    steps = 3,
                                    modifier = Modifier
                                        .weight(0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsAlbumScreenLayout(paddingValues: PaddingValues) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        ) {
            //Grid
            item {
                Group {
                    //Title
                    GroupTitle(R.string.settings_album_grid_title)

                    //Items
                    GroupItems {
                        //Items per row
                        GroupItem {
                            SettingsItem(
                                title = stringResource(R.string.settings_album_grid_row_title, view.albumItemsPerRow.toInt()),
                                description = stringResource(R.string.settings_album_grid_row_description)
                            ) {
                                //Value
                                Slider(
                                    value = view.albumItemsPerRow,
                                    onValueChange = { newValue ->
                                        view.albumItemsPerRow = newValue
                                    },
                                    onValueChangeFinished = { view.saveAlbumItemsPerRow() },
                                    valueRange = 1f..5f,
                                    steps = 3,
                                    modifier = Modifier
                                        .weight(0.5f)
                                )
                            }
                        }
                    }
                }
            }

            //Advanced
            item {
                Group {
                    //Title
                    GroupTitle(R.string.settings_album_advanced_title)

                    //Items
                    GroupItems {
                        //Show missing metadata icon
                        GroupItem {
                            SettingsItem(
                                title = R.string.settings_album_advanced_metadata_title,
                                description = R.string.settings_album_advanced_metadata_description
                            ) {
                                //Value
                                Switch(
                                    checked = view.albumShowMissingMetadataIcon,
                                    onCheckedChange = { isChecked ->
                                        view.updateAlbumShowMissingMetadataIcon(isChecked)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsViewerScreenLayout(paddingValues: PaddingValues) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        ) {
            item {
                Group {
                    //Title
                    GroupTitle(R.string.settings_viewer_shortcuts_title)

                    //Items
                    GroupItems {
                        //Info shortcut
                        GroupItem {
                            SettingsItem(
                                title = R.string.settings_viewer_shortcuts_info_title,
                                description = R.string.settings_viewer_shortcuts_info_description
                            ) {
                                //Value
                                Switch(
                                    checked = view.viewerShowInfo,
                                    onCheckedChange = { isChecked ->
                                        view.updateViewerShowInfo(isChecked)
                                    }
                                )
                            }
                        }

                        //Divider
                        GroupDivider()

                        //Edit shortcut
                        GroupItem {
                            SettingsItem(
                                title = R.string.settings_viewer_shortcuts_edit_title,
                                description = R.string.settings_viewer_shortcuts_edit_description
                            ) {
                                //Value
                                Switch(
                                    checked = view.viewerShowEdit,
                                    onCheckedChange = { isChecked ->
                                        view.updateViewerShowEdit(isChecked)
                                    }
                                )
                            }
                        }

                        //Divider
                        GroupDivider()

                        //Share shortcut
                        GroupItem {
                            SettingsItem(
                                title = R.string.settings_viewer_shortcuts_share_title,
                                description = R.string.settings_viewer_shortcuts_share_description
                            ) {
                                //Value
                                Switch(
                                    checked = view.viewerShowShare,
                                    onCheckedChange = { isChecked ->
                                        view.updateViewerShowShare(isChecked)
                                    }
                                )
                            }
                        }

                        //Divider
                        GroupDivider()

                        //Favourite shortcut
                        GroupItem {
                            SettingsItem(
                                title = R.string.settings_viewer_shortcuts_favourite_title,
                                description = R.string.settings_viewer_shortcuts_favourite_description
                            ) {
                                //Value
                                Switch(
                                    checked = view.viewerShowFavourite,
                                    onCheckedChange = { isChecked ->
                                        view.updateViewerShowFavourite(isChecked)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsVideoPlayerLayout(paddingValues: PaddingValues) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        ) {
            //Player
            item {
                Group {
                    //Title
                    GroupTitle(R.string.settings_video_player_title)

                    //Items
                    GroupItems {
                        //Skip backwards amount
                        GroupItem {
                            SettingsItem(
                                title = stringResource(R.string.settings_video_player_skip_backwards_title, view.videoSkipBackwardsAmount.toLong()),
                                description = stringResource(R.string.settings_video_player_skip_backwards_description)
                            ) {
                                //Value
                                Slider(
                                    value = view.videoSkipBackwardsAmount,
                                    onValueChange = { newValue ->
                                        view.videoSkipBackwardsAmount = newValue
                                    },
                                    onValueChangeFinished = { view.saveVideoSkipBackwardsAmount() },
                                    valueRange = 5f..25f,
                                    steps = 3,
                                    modifier = Modifier
                                        .weight(0.5f)
                                )
                            }
                        }

                        //Divider
                        GroupDivider()

                        //Skip forward amount
                        GroupItem {
                            SettingsItem(
                                title = stringResource(R.string.settings_video_player_skip_forward_title, view.videoSkipForwardAmount.toLong()),
                                description = stringResource(R.string.settings_video_player_skip_forward_description)
                            ) {
                                //Value
                                Slider(
                                    value = view.videoSkipForwardAmount,
                                    onValueChange = { newValue ->
                                        view.videoSkipForwardAmount = newValue
                                    },
                                    onValueChangeFinished = { view.saveVideoSkipForwardAmount() },
                                    valueRange = 5f..25f,
                                    steps = 3,
                                    modifier = Modifier
                                        .weight(0.5f)
                                )
                            }
                        }
                    }
                }
            }

            //Advanced
            item {
                Group {
                    //Title
                    GroupTitle(R.string.settings_video_advanced_title)

                    //Items
                    GroupItems {
                        //Use internal player
                        GroupItem {
                            SettingsItem(
                                title = R.string.settings_video_advanced_internal_player_title,
                                description = R.string.settings_video_advanced_internal_player_description
                            ) {
                                //Value
                                Switch(
                                    checked = view.videoUseInternalPlayer,
                                    onCheckedChange = { isChecked ->
                                        view.updateVideoUseInternalPlayer(isChecked)
                                    }
                                )
                            }
                        }

                        //Divider
                        GroupDivider()

                        //Ignore audio focus
                        GroupItem {
                            SettingsItem(
                                title = R.string.settings_video_advanced_focus_title,
                                description = R.string.settings_video_advanced_focus_description
                            ) {
                                //Value
                                Switch(
                                    checked = view.videoIgnoreAudioFocus,
                                    onCheckedChange = { isChecked ->
                                        view.updateVideoIgnoreAudioFocus(isChecked)
                                    }
                                )
                            }
                        }

                        //Divider
                        GroupDivider()

                        //Show controller on start
                        GroupItem {
                            SettingsItem(
                                title = R.string.settings_video_advanced_show_controls_title,
                                description = R.string.settings_video_advanced_show_controls_description
                            ) {
                                //Value
                                Switch(
                                    checked = view.videoShowControlsOnStart,
                                    onCheckedChange = { isChecked ->
                                        view.updateVideoShowControlsOnStart(isChecked)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}
