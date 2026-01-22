package com.botpa.turbophotos.settings

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.botpa.turbophotos.BuildConfig
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.Link
import com.botpa.turbophotos.gallery.dialogs.DialogAlbums
import com.botpa.turbophotos.gallery.dialogs.DialogExplorer
import com.botpa.turbophotos.gallery.views.Group
import com.botpa.turbophotos.gallery.views.GroupDivider
import com.botpa.turbophotos.gallery.views.GroupItems
import com.botpa.turbophotos.gallery.views.GroupTitle
import com.botpa.turbophotos.gallery.views.IconButton
import com.botpa.turbophotos.gallery.views.Layout
import com.botpa.turbophotos.theme.CoonTheme
import com.botpa.turbophotos.theme.FONT_COMFORTAA
import com.botpa.turbophotos.util.Storage
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : AppCompatActivity() {

    //View model
    private val view: SettingsViewModel by viewModels()


    //App
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Init storage
        Storage.init(this@SettingsActivity)

        //Edging
        enableEdgeToEdge()

        //Content
        setContent {
            CoonTheme {
                SettingsLayout()
            }
        }
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
                //Create settings backup
                view.createSettingsBackup(context)
            }
        }
        val onChooseBackupFile = remember {
            {
                //Show select file dialog
                DialogExplorer(
                    context = activity,
                    isSelectingFiles = true,
                    fileExtension = "json",
                    onSelect = { file ->
                        //Restore settings backup
                        view.restoreSettingsBackup(context, activity, file)
                    }
                ).buildAndShow()

                //Feedback toast
                Toast.makeText(activity, "Select a backup file to restore.", Toast.LENGTH_SHORT).show()
            }
        }

        //Links actions
        val onChooseLinkAlbum = remember<(Int) -> Unit> {
            { index ->
                //Show select album dialog
                DialogAlbums(
                    context = context,
                    albums = Library.albums,
                    onSelectAlbum = { album ->
                        //Choose album folder
                        val folder = album.imagesFolder ?: return@DialogAlbums
                        view.updateLinkAlbumFolder(activity, index, folder)
                    },
                    onSelectFolder = { folder ->
                        //Choose folder
                        view.updateLinkAlbumFolder(activity, index, folder)
                    }
                ).buildAndShow()

                //Feedback toast
                Toast.makeText(activity, "Select an album to link it.", Toast.LENGTH_SHORT).show()
            }
        }
        val onChooseLinkMetadata = remember<(Int, Link) -> Unit> {
            { index, link ->
                //Check if album folder exists
                if (!link.albumFolder.exists()) {
                    //Feedback toast
                    Toast.makeText(activity, "Add an album first.", Toast.LENGTH_SHORT).show()
                } else {
                    //Show select file dialog
                    DialogExplorer(
                        context = context,
                        isSelectingFiles = true,
                        fileExtension = "json",
                        onSelect = { file ->
                            //Choose file
                            view.updateLinkMetadataFile(index, file)
                        }
                    ).buildAndShow()

                    //Feedback toast
                    Toast.makeText(activity, "Select a file to link it.", Toast.LENGTH_SHORT).show()
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
                        GroupTitle("App")

                        //Items
                        GroupItems {
                            //Backup
                            SettingsItem(
                                title = "Backup",
                                description = "Create or load a backup of your settings."
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

                            //Divider
                            GroupDivider()

                            //Automatic metadata modification
                            SettingsItem(
                                title = "Metadata modification",
                                description = "Modify album metadata automatically when moving, copying or deleting items from an album."
                            ) {
                                //Value
                                Switch(
                                    checked = view.appModifyMetadata,
                                    onCheckedChange = { isChecked -> view.updateAppModifyMetadata(isChecked) }
                                )
                            }
                        }
                    }
                }

                //Home screen
                item {
                    Group {
                        //Title
                        GroupTitle("Home Screen")

                        //Items
                        GroupItems {
                            //Items per row
                            SettingsItem(
                                title = "Items per row · ${view.homeItemsPerRow.toInt()}",
                                description = "The amount of albums to show per home screen row."
                            ) {
                                //Value
                                Slider(
                                    value = view.homeItemsPerRow,
                                    onValueChange = { newValue -> view.updateHomeItemsPerRow(newValue) },
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

                //Albums screen
                item {
                    Group {
                        //Title
                        GroupTitle("Album Screen")

                        //Items
                        GroupItems {
                            //Items per row
                            SettingsItem(
                                title = "Items per row · ${view.albumItemsPerRow.toInt()}",
                                description = "The amount of images/videos to show per album screen row."
                            ) {
                                //Value
                                Slider(
                                    value = view.albumItemsPerRow,
                                    onValueChange = { newValue -> view.updateAlbumItemsPerRow(newValue) },
                                    onValueChangeFinished = { view.saveAlbumItemsPerRow() },
                                    valueRange = 1f..5f,
                                    steps = 3,
                                    modifier = Modifier
                                        .weight(0.5f)
                                )
                            }

                            //Divider
                            GroupDivider()

                            //Show missing metadata icon
                            SettingsItem(
                                title = "Show missing metadata icon",
                                description = "Show an icon on items without a metadata key if their album has metadata."
                            ) {
                                //Value
                                Switch(
                                    checked = view.albumShowMissingMetadataIcon,
                                    onCheckedChange = { isChecked -> view.updateAlbumShowMissingMetadataIcon(isChecked) }
                                )
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
                                title = "Albums & Metadata Links",
                                modifier = Modifier
                                    .weight(1f)
                            )

                            //Description
                            IconButton(
                                onClick = {
                                    //Create text
                                    val text = StringBuilder()
                                    text.append("Links let you to backup your albums and enable smart search.")
                                    text.append("\n· Add an album to enable backing it up in the sync service.")
                                    text.append("\n· Add a metadata file to improve search and find things in your images.")

                                    //Create dialog
                                    val builder = MaterialAlertDialogBuilder(context)
                                    builder.setTitle("Links")
                                    builder.setMessage(text.toString())
                                    builder.setPositiveButton("Close", null)
                                    builder.show()
                                },
                                painter = painterResource(R.drawable.info),
                                contentDescription = "Links info"
                            )
                        }

                        //Items
                        GroupItems {
                            Link.links.forEachIndexed { index, link ->
                                //Add item
                                LinkItem(
                                    index = index,
                                    link = link,
                                    onChooseAlbum = onChooseLinkAlbum,
                                    onChooseMetadata = onChooseLinkMetadata,
                                    onDelete = { index -> view.removeLink(index) }
                                )

                                //Add divider between items
                                if (index < Link.links.size - 1) GroupDivider()
                            }
                        }

                        //Add link button
                        Button(
                            onClick = { view.addLink(activity) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Text(
                                text = "Add link",
                                fontFamily = FONT_COMFORTAA,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                //About
                item {
                    Group {
                        //Title
                        GroupTitle("About")

                        //Items
                        GroupItems {
                            //Version
                            SettingsItem(
                                title = "Version",
                                description = "Coon Gallery v${BuildConfig.VERSION_NAME}"
                            ) {}

                            //Divider
                            GroupDivider()

                            //Developer
                            SettingsItem(
                                title = "Developer",
                                description = "Click to see the things I make!"
                            ) {
                                IconButton(
                                    onClick = { uriHandler.openUri("https://botpa.vercel.app/") },
                                    painter = painterResource(R.drawable.open),
                                    contentDescription = "Portfolio"
                                )
                            }

                            //Divider
                            GroupDivider()

                            //Github
                            SettingsItem(
                                title = "Github",
                                description = "Click to check for updates!"
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
