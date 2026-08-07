package com.botpa.turbophotos.screens.settings

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.botpa.turbophotos.gallery.views.IconButton
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

    //Layout
    @Composable
    private fun SettingsLayout() {
        //Get useful stuff
        val context = LocalContext.current
        val activity = this
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

        //Layout
        Layout("Settings") {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(it)
                    .padding(horizontal = 20.dp)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            ) {
                //App
                item {
                    Group {
                        //Title
                        GroupTitle(R.string.settings_app_title)

                        //Items
                        GroupItems {
                            //Backup
                            GroupItem {
                                SettingsItem(
                                    title = R.string.settings_app_backup_title,
                                    description = R.string.settings_app_backup_description
                                ) {
                                    //Backup
                                    IconButton(
                                        onClick = onCreateBackup,
                                        painter = painterResource(R.drawable.backup_create),
                                        contentDescription = "Backup"
                                    )

                                    //Restore
                                    IconButton(
                                        onClick = onChooseBackupFile,
                                        painter = painterResource(R.drawable.backup_restore),
                                        contentDescription = "Restore",
                                        modifier = Modifier
                                            .padding(start = 10.dp)
                                    )
                                }
                            }

                            //Divider
                            GroupDivider()

                            //Automatic metadata modification
                            GroupItem {
                                SettingsItem(
                                    title = R.string.settings_app_metadata_title,
                                    description = R.string.settings_app_metadata_description
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
                }

                //Home screen
                item {
                    Group {
                        //Title
                        GroupTitle(R.string.settings_home_title)

                        //Items
                        GroupItems {
                            //Items per row
                            GroupItem {
                                SettingsItem(
                                    title = stringResource(R.string.settings_home_row_title, view.homeItemsPerRow.toInt()),
                                    description = stringResource(R.string.settings_home_row_description)
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

                //Album screen
                item {
                    Group {
                        //Title
                        GroupTitle(R.string.settings_album_title)

                        //Items
                        GroupItems {
                            //Items per row
                            GroupItem {
                                SettingsItem(
                                    title = stringResource(R.string.settings_album_row_title, view.albumItemsPerRow.toInt()),
                                    description = stringResource(R.string.settings_album_row_description)
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

                            //Divider
                            GroupDivider()

                            //Show missing metadata icon
                            GroupItem {
                                SettingsItem(
                                    title = R.string.settings_album_metadata_title,
                                    description = R.string.settings_album_metadata_description
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

                //Display screen
                item {
                    Group {
                        //Title
                        GroupTitle(R.string.settings_display_title)

                        //Items
                        GroupItems {
                            //Info shortcut
                            GroupItem {
                                SettingsItem(
                                    title = R.string.settings_display_info_title,
                                    description = R.string.settings_display_info_description
                                ) {
                                    //Value
                                    Switch(
                                        checked = view.displayShowInfo,
                                        onCheckedChange = { isChecked ->
                                            view.updateDisplayShowInfo(isChecked)
                                        }
                                    )
                                }
                            }

                            //Divider
                            GroupDivider()

                            //Edit shortcut
                            GroupItem {
                                SettingsItem(
                                    title = R.string.settings_display_edit_title,
                                    description = R.string.settings_display_edit_description
                                ) {
                                    //Value
                                    Switch(
                                        checked = view.displayShowEdit,
                                        onCheckedChange = { isChecked ->
                                            view.updateDisplayShowEdit(isChecked)
                                        }
                                    )
                                }
                            }

                            //Divider
                            GroupDivider()

                            //Share shortcut
                            GroupItem {
                                SettingsItem(
                                    title = R.string.settings_display_share_title,
                                    description = R.string.settings_display_share_description
                                ) {
                                    //Value
                                    Switch(
                                        checked = view.displayShowShare,
                                        onCheckedChange = { isChecked ->
                                            view.updateDisplayShowShare(isChecked)
                                        }
                                    )
                                }
                            }

                            //Divider
                            GroupDivider()

                            //Favourite shortcut
                            GroupItem {
                                SettingsItem(
                                    title = R.string.settings_display_favourite_title,
                                    description = R.string.settings_display_favourite_description
                                ) {
                                    //Value
                                    Switch(
                                        checked = view.displayShowFavourite,
                                        onCheckedChange = { isChecked ->
                                            view.updateDisplayShowFavourite(isChecked)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                //Video player
                item {
                    Group {
                        //Title
                        GroupTitle(R.string.settings_video_title)

                        //Items
                        GroupItems {
                            //Skip backwards amount
                            GroupItem {
                                SettingsItem(
                                    title = stringResource(R.string.settings_video_skip_backwards_title, view.videoSkipBackwardsAmount.toLong()),
                                    description = stringResource(R.string.settings_video_skip_backwards_description)
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
                                    title = stringResource(R.string.settings_video_skip_forward_title, view.videoSkipForwardAmount.toLong()),
                                    description = stringResource(R.string.settings_video_skip_forward_description)
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

                            //Divider
                            GroupDivider()

                            //Use internal player
                            GroupItem {
                                SettingsItem(
                                    title = R.string.settings_video_internal_player_title,
                                    description = R.string.settings_video_internal_player_description
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
                                    title = R.string.settings_video_audio_focus_title,
                                    description = R.string.settings_video_audio_focus_description
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
                                    title = R.string.settings_video_show_controls_title,
                                    description = R.string.settings_video_show_controls_description
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

                //Links
                item {
                    Group {
                        //Title & description
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            //Title
                            GroupTitle(
                                title = R.string.settings_links_title,
                                modifier = Modifier
                                    .weight(1f)
                            )

                            //Description
                            IconButton(
                                onClick = {
                                    //Create info dialog
                                    BulletPointsDialog(
                                        context,
                                        title = R.string.settings_links_info_title,
                                        text = R.string.settings_links_info_description,
                                        points = listOf(
                                            R.string.settings_links_info_point1,
                                            R.string.settings_links_info_point2
                                        )
                                    ).buildAndShow()
                                },
                                painter = painterResource(R.drawable.info),
                                contentDescription = "Links info"
                            )
                        }

                        //Items
                        GroupItems {
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
                            text = R.string.settings_links_add,
                            onClick = { view.addLink(activity) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        )
                    }
                }

                //About
                item {
                    Group {
                        //Title
                        GroupTitle(R.string.settings_about_title)

                        //Items
                        GroupItems {
                            //Version
                            GroupItem {
                                SettingsItem(
                                    title = stringResource(R.string.settings_about_version_title),
                                    description = stringResource(R.string.settings_about_version_description, BuildConfig.VERSION_NAME)
                                ) {}
                            }

                            //Divider
                            GroupDivider()

                            //Copyright
                            GroupItem {
                                SettingsItem(
                                    title = R.string.settings_about_copyright_title,
                                    description = R.string.settings_about_copyright_description
                                ) {}
                            }

                            //Divider
                            GroupDivider()

                            //Developer
                            GroupItem {
                                SettingsItem(
                                    title = R.string.settings_about_developer_title,
                                    description = R.string.settings_about_developer_description
                                ) {
                                    IconButton(
                                        onClick = { uriHandler.openUri("https://botpa.vercel.app/") },
                                        painter = painterResource(R.drawable.open),
                                        contentDescription = "Portfolio"
                                    )
                                }
                            }

                            //Divider
                            GroupDivider()

                            //Github
                            GroupItem {
                                SettingsItem(
                                    title = R.string.settings_about_repo_title,
                                    description = R.string.settings_about_repo_description
                                ) {
                                    IconButton(
                                        onClick = { uriHandler.openUri("https://github.com/BOTPanzer/Coon-Gallery") },
                                        painter = painterResource(R.drawable.open),
                                        contentDescription = "Github"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
