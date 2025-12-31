package com.botpa.turbophotos.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.botpa.turbophotos.theme.CoonTheme
import com.botpa.turbophotos.theme.FONT_COMFORTAA
import com.botpa.turbophotos.theme.FONT_OPIFICIO
import com.botpa.turbophotos.gallery.Link
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage
import com.google.android.material.snackbar.Snackbar
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {

    //Links
    private enum class PickerAction { SelectFolder, SelectFile, CreateFile }

    //App
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Load storage
        Storage.load(this@SettingsActivity)

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
    @Preview
    @Composable
    private fun SettingsLayout() {
        //Get context & activity
        val context = LocalContext.current
        val activity = this

        //Home settings
        var homeItemsPerRow by remember { mutableFloatStateOf(Storage.getInt("Settings.homeItemsPerRow", 2).toFloat()) }

        //Album settings
        var albumItemsPerRow by remember { mutableFloatStateOf(Storage.getInt("Settings.albumItemsPerRow", 3).toFloat()) }
        var albumShowMissingMetadataIcon by remember { mutableStateOf(Storage.getBool("Settings.albumShowMissingMetadataIcon", false)) }

        //Link item file picker actions
        var filePickerIndex by remember { mutableIntStateOf(-1) }
        var filePickerAction by remember { mutableStateOf(PickerAction.SelectFolder) }
        val filePickerLauncher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
            //Bad result
            if (result.resultCode != RESULT_OK || result.data == null) return@rememberLauncherForActivityResult

            //Parse result
            try {
                // Parse file path from URI
                val path = Orion.convertUriToFilePath(context, result.data!!.data)
                if (path == null) throw Exception("Path was null")
                val file = File(path)

                //Update the link based on the action
                val link = Link.links[filePickerIndex]
                when (filePickerAction) {
                    //Select folder
                    PickerAction.SelectFolder -> {
                        val updated = Link.updateLinkFolder(filePickerIndex, file)
                        if (!updated) {
                            Orion.snack(this@SettingsActivity, "Album already exists")
                            return@rememberLauncherForActivityResult
                        }
                    }

                    //Select file
                    PickerAction.SelectFile -> {
                        //Update link with selected file
                        Link.updateLinkFile(filePickerIndex, file)
                    }

                    //Create file
                    PickerAction.CreateFile -> {
                        //Create file based in album name
                        var metadataFile: File
                        var name: String
                        var i = 0
                        do {
                            name = "${
                                link.albumFolder.name.lowercase().replace(" ", "-")
                            }${if (i > 0) " ($i)" else ""}.metadata.json"
                            metadataFile = File(file.absolutePath + "/" + name)
                            i++
                        } while (metadataFile.exists())
                        Orion.writeFile(metadataFile, "{}")

                        //Update link with created file
                        Link.updateLinkFile(filePickerIndex, metadataFile)
                    }
                }

                //Save links
                Link.saveLinks()
            } catch (e: Exception) {
                //Feedback toast
                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show()
            }
        }

        //Link item actions
        val onChooseFolder = remember<(Int) -> Unit> {
            { index ->
                //Save link index
                filePickerIndex = index

                //Feedback toast
                Toast.makeText(this@SettingsActivity, "Select a folder to use as album", Toast.LENGTH_LONG).show()

                //Ask for a folder
                filePickerAction = PickerAction.SelectFolder
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                filePickerLauncher.launch(intent)
            }
        }
        val onChooseFile = remember<(Int, Link) -> Unit> {
            { index, link ->
                //Check if album folder exists
                if (!link.albumFolder.exists()) {
                    Toast.makeText(this@SettingsActivity, "Add album folder first", Toast.LENGTH_LONG).show()
                } else {
                    //Save link index
                    filePickerIndex = index

                    //Check action
                    Orion.snack2(
                        this@SettingsActivity,
                        "Do you have an already created metadata file?",
                        "Select",
                        {
                            Toast.makeText(this@SettingsActivity, "Select a file to use as metadata", Toast.LENGTH_LONG).show()
                            filePickerAction = PickerAction.SelectFile
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                            intent.addCategory(Intent.CATEGORY_OPENABLE)
                            intent.type = "application/json"
                            filePickerLauncher.launch(intent)
                        },
                        "Create",
                        {
                            if (!Link.links[index].albumFolder.exists()) {
                                Toast.makeText(this@SettingsActivity, "Please select an album folder first", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this@SettingsActivity,"Select a folder to create the album metadata file", Toast.LENGTH_LONG).show()
                                filePickerAction = PickerAction.CreateFile
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                filePickerLauncher.launch(intent)
                            }
                        },
                        Snackbar.LENGTH_INDEFINITE
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
                            fontSize = 18.sp,
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
                //Home screen
                item {
                    //Title
                    Text(
                        text = "Home Screen",
                        textAlign = TextAlign.Center,
                        fontFamily = FONT_OPIFICIO,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    )

                    //Items per row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        //Name
                        Text(
                            text = "Items per row · ${homeItemsPerRow.toInt()}",
                            fontSize = 14.sp,
                            modifier = Modifier
                                .weight(1.5f)
                        )

                        //Value
                        Slider(
                            value = homeItemsPerRow,
                            onValueChange = { newValue -> homeItemsPerRow = newValue },
                            onValueChangeFinished = { Storage.putInt("Settings.homeItemsPerRow", homeItemsPerRow.toInt()) },
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier
                                .weight(1f)
                        )
                    }
                }

                //Albums screen
                item {
                    //Title
                    Text(
                        text = "Album Screen",
                        textAlign = TextAlign.Center,
                        fontFamily = FONT_OPIFICIO,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 30.dp, bottom = 10.dp)
                    )

                    //Items per row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        //Name
                        Text(
                            text = "Items per row · ${albumItemsPerRow.toInt()}",
                            fontSize = 14.sp,
                            modifier = Modifier
                                .weight(1.5f)
                        )

                        //Value
                        Slider(
                            value = albumItemsPerRow,
                            onValueChange = { newValue -> albumItemsPerRow = newValue },
                            onValueChangeFinished = { Storage.putInt("Settings.albumItemsPerRow", albumItemsPerRow.toInt()) },
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier
                                .weight(1f)
                        )
                    }

                    //Show missing metadata icon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        //Name
                        Text(
                            text = "Show missing metadata icon",
                            fontSize = 14.sp,
                            modifier = Modifier
                                .weight(1.5f)
                        )

                        //Value
                        Switch(
                            checked = albumShowMissingMetadataIcon,
                            onCheckedChange = { isChecked ->
                                albumShowMissingMetadataIcon = isChecked
                                Storage.putBool("Settings.albumShowMissingMetadataIcon", isChecked)
                            }
                        )
                    }
                }

                //Links (title & description)
                item {
                    //Title
                    Text(
                        text = "Albums & Metadata Links",
                        textAlign = TextAlign.Center,
                        fontFamily = FONT_OPIFICIO,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 30.dp)
                    )

                    //Description
                    Text(
                        text = "Add albums to enable backing them up in the backup service.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 5.dp)
                    )
                    Text(
                        text = "Add metadata files to improve search and find things in your images.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 5.dp)
                    )
                    Text(
                        text = "To change where an album and its metadata are located, click on their respective icons.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 5.dp)
                    )
                }

                //Links (list)
                itemsIndexed(Link.links) { index, link ->
                    LinkItem(
                        index = index,
                        link = link,
                        onChooseFolder = onChooseFolder,
                        onChooseFile = onChooseFile,
                        onDelete = onDelete
                    )
                }

                //Links (add link button)
                item {
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
                                fontSize = 14.sp,
                            )
                        }
                    }
            }
        }
    }

}
