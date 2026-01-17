package com.botpa.turbophotos.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.botpa.turbophotos.R
import com.botpa.turbophotos.theme.CoonTheme
import com.botpa.turbophotos.theme.FONT_COMFORTAA
import com.botpa.turbophotos.theme.FONT_OPIFICIO
import com.botpa.turbophotos.gallery.Link
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {

    //View model
    private val viewModel: SettingsViewModel by viewModels()

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
                SettingsLayout(viewModel)
            }
        }
    }

    //Layout
    @Composable
    private fun SettingsLayout(viewModel: SettingsViewModel) {
        //Get useful stuff
        val context = LocalContext.current
        val activity = this
        val uriHandler = LocalUriHandler.current

        //Links file picker
        val filePickerLauncher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
            //Bad result
            if (result.resultCode != RESULT_OK || result.data == null) return@rememberLauncherForActivityResult

            //Handle result
            viewModel.handleFileResult(result.data!!.data, context, activity)
        }

        //Links item actions
        val onChooseFolder = remember<(Int) -> Unit> {
            { index ->
                //Save link index
                viewModel.filePickerIndex = index

                //Feedback toast
                Toast.makeText(activity, "Select a folder to use as album", Toast.LENGTH_LONG).show()

                //Ask for a folder
                viewModel.filePickerAction = SettingsViewModel.PickerAction.SelectFolder
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                filePickerLauncher.launch(intent)
            }
        }
        val onChooseFile = remember<(Int, Link) -> Unit> {
            { index, link ->
                //Check if album folder exists
                if (!link.albumFolder.exists()) {
                    Toast.makeText(activity, "Add an album folder first", Toast.LENGTH_SHORT).show()
                } else {
                    //Save link index
                    viewModel.filePickerIndex = index

                    //Check action
                    Orion.snackTwo(
                        activity,
                        "Choose a metadata file action",
                        "Create",
                        {
                            if (!Link.links[index].albumFolder.exists()) {
                                Toast.makeText(activity, "Add an album folder first", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(activity,"Select a folder to create the album metadata file", Toast.LENGTH_LONG).show()
                                viewModel.filePickerAction = SettingsViewModel.PickerAction.CreateFile
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                filePickerLauncher.launch(intent)
                            }
                        },
                        "Select",
                        {
                            Toast.makeText(activity, "Select a file to use as metadata", Toast.LENGTH_LONG).show()
                            viewModel.filePickerAction = SettingsViewModel.PickerAction.SelectFile
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                            intent.addCategory(Intent.CATEGORY_OPENABLE)
                            intent.type = "application/json"
                            filePickerLauncher.launch(intent)
                        },
                        Snackbar.LENGTH_LONG
                    )
                }
            }
        }
        val onDelete = remember<(Int) -> Unit> {
            { index ->
                //Remove link
                if (Link.removeLink(index)) Link.saveLinks()
            }
        }

        //Layout
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Settings",
                            fontFamily = FONT_OPIFICIO,
                            fontSize = 20.sp,
                        )
                    },
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                )
            },
            contentWindowInsets = WindowInsets(0.dp)
        ) {
            LazyColumn(
                contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(it)
                    .padding(horizontal = 20.dp)
            ) {
                //App
                item {
                    SettingsGroup {
                        //Title
                        SettingsTitle("App")

                        //Items
                        SettingsItems {
                            //Automatic metadata modification
                            SettingsItem(
                                title = "Metadata modification",
                                description = "Modify album metadata automatically when moving, copying or deleting items from an album."
                            ) {
                                //Value
                                Switch(
                                    checked = viewModel.appModifyMetadata,
                                    onCheckedChange = { isChecked -> viewModel.updateAppModifyMetadata(isChecked) }
                                )
                            }
                        }
                    }
                }

                //Home screen
                item {
                    SettingsGroup {
                        //Title
                        SettingsTitle("Home Screen")

                        //Items
                        SettingsItems {
                            //Items per row
                            SettingsItem(
                                title = "Items per row · ${viewModel.homeItemsPerRow.toInt()}",
                                description = "The amount of albums to show per home screen row."
                            ) {
                                //Value
                                Slider(
                                    value = viewModel.homeItemsPerRow,
                                    onValueChange = { newValue -> viewModel.updateHomeItemsPerRow(newValue) },
                                    onValueChangeFinished = { viewModel.saveHomeItemsPerRow() },
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
                    SettingsGroup {
                        //Title
                        SettingsTitle("Album Screen")

                        //Items
                        SettingsItems {
                            //Items per row
                            SettingsItem(
                                title = "Items per row · ${viewModel.albumItemsPerRow.toInt()}",
                                description = "The amount of images/videos to show per album screen row."
                            ) {
                                //Value
                                Slider(
                                    value = viewModel.albumItemsPerRow,
                                    onValueChange = { newValue -> viewModel.updateAlbumItemsPerRow(newValue) },
                                    onValueChangeFinished = { viewModel.saveAlbumItemsPerRow() },
                                    valueRange = 1f..5f,
                                    steps = 3,
                                    modifier = Modifier
                                        .weight(0.5f)
                                )
                            }

                            //Divider
                            SettingsDivider()

                            //Show missing metadata icon
                            SettingsItem(
                                title = "Show missing metadata icon",
                                description = "Show an icon on items without a metadata key if their album has metadata."
                            ) {
                                //Value
                                Switch(
                                    checked = viewModel.albumShowMissingMetadataIcon,
                                    onCheckedChange = { isChecked -> viewModel.updateAlbumShowMissingMetadataIcon(isChecked) }
                                )
                            }
                        }
                    }
                }

                //Links
                item {
                    SettingsGroup {
                        //Title & description
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            //Title
                            SettingsTitle(
                                title = "Albums & Metadata Links",
                                modifier = Modifier
                                    .weight(1f)
                            )

                            //Description
                            Button(
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
                                    builder.setPositiveButton("OK") { _, _ -> }
                                    builder.show()
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .size(40.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.info),
                                    contentDescription = "Links info",
                                    contentScale = ContentScale.Fit,
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                                    modifier = Modifier
                                        .size(24.dp)
                                )
                            }
                        }

                        //Items
                        SettingsItems {
                            Link.links.forEachIndexed { index, link ->
                                //Add item
                                LinkItem(
                                    index = index,
                                    link = link,
                                    onChooseFolder = onChooseFolder,
                                    onChooseFile = onChooseFile,
                                    onDelete = onDelete
                                )

                                //Add divider between items
                                if (index < Link.links.size - 1) SettingsDivider()
                            }
                        }

                        //Add link button
                        Button(
                            onClick = {
                                //Create & try to add new link
                                if (Link.addLink(Link("", ""))) {
                                    //Added -> Save links
                                    Link.saveLinks()
                                } else {
                                    //Not added -> There is another link with the same album
                                    Orion.snack(activity, "Can't have duplicate albums")
                                }
                            },
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

                //Credits
                item {
                    SettingsGroup {
                        //Title
                        SettingsTitle("Credits")

                        //Items
                        SettingsItems {
                            //Portfolio
                            SettingsItem(
                                title = "Portfolio",
                                description = "Check out the things I make!"
                            ) {
                                Button(
                                    onClick = {
                                        uriHandler.openUri("https://botpa.vercel.app/")
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier
                                        .size(40.dp)
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.app_icon),
                                        contentDescription = "Portfolio",
                                        contentScale = ContentScale.Fit,
                                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                                        modifier = Modifier
                                            .size(24.dp)
                                    )
                                }
                            }

                            //Divider
                            SettingsDivider()

                            //Github
                            SettingsItem(
                                title = "Github",
                                description = "Check it out for updates."
                            ) {
                                Button(
                                    onClick = {
                                        uriHandler.openUri("https://github.com/BOTPanzer/Coon-Gallery")
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier
                                        .size(40.dp)
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.github),
                                        contentDescription = "Github",
                                        contentScale = ContentScale.Fit,
                                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                                        modifier = Modifier
                                            .size(24.dp)
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
