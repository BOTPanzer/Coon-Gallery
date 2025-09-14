package com.botpa.turbophotos.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import com.botpa.turbophotos.util.Library
import com.botpa.turbophotos.util.Link
import com.botpa.turbophotos.util.Orion
import com.google.android.material.snackbar.Snackbar
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.botpa.turbophotos.theme.CoonTheme
import com.botpa.turbophotos.theme.FONT_COMFORTAA
import com.botpa.turbophotos.theme.FONT_OPIFICIO
import com.botpa.turbophotos.util.Storage
import java.util.ArrayList

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {

    //Links
    private enum class PickerAction { SelectFolder, SelectFile, CreateFile }

    //App
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Load storage
        Storage.load(this@SettingsActivity)

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

        //App settings
        var galleryAlbumsPerRow by remember { mutableFloatStateOf(Storage.getInt("Settings.galleryAlbumsPerRow", 2).toFloat()) }
        var galleryImagesPerRow by remember { mutableFloatStateOf(Storage.getInt("Settings.galleryImagesPerRow", 3).toFloat()) }
        var showMissingMetadataIcon by remember { mutableStateOf(Storage.getBool("Settings.showMissingMetadataIcon", false)) }

        //Links list
        var links by remember { mutableStateOf(Library.links.toList()) }
        var updateLinksList = {
            links = ArrayList<Link>()
            links = Library.links.toList() as ArrayList<Link>
        }

        //File picker actions
        var filePickerIndex by remember { mutableIntStateOf(-1) }
        var filePickerAction by remember { mutableStateOf(PickerAction.SelectFolder) }
        val filePickerLauncher: ActivityResultLauncher<Intent> = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
            //Bad result
            if (result.resultCode != RESULT_OK || result.data == null) return@rememberLauncherForActivityResult

            //Parse result
            try {
                // Parse file path from URI
                val path = Orion.convertUriToFilePath(context, result.data!!.data)
                if (path == null) throw Exception("Path was null")
                val file = File(path)

                //Update the link based on the action
                val album = Library.links[filePickerIndex]
                when (filePickerAction) {
                    //Select folder
                    PickerAction.SelectFolder -> {
                        val updated = Library.updateLinkFolder(filePickerIndex, file)
                        if (!updated) {
                            Orion.snack(this@SettingsActivity, "Album already exists")
                            return@rememberLauncherForActivityResult
                        }
                    }

                    //Select file
                    PickerAction.SelectFile -> {
                        Library.updateLinkFile(filePickerIndex, file)
                    }

                    //Create file
                    PickerAction.CreateFile -> {
                        var metadataFile: File
                        var name: String
                        var i = 0
                        do {
                            name = "${
                                album.albumFolder.name.lowercase().replace(" ", "-")
                            }${if (i > 0) " ($i)" else ""}.metadata.json"
                            metadataFile = File(file.absolutePath + "/" + name)
                            i++
                        } while (metadataFile.exists())
                        Orion.writeFile(metadataFile, "{}")
                        album.metadataFile = metadataFile
                    }
                }

                //Update list & save links
                updateLinksList()
                Library.saveLinks()
            } catch (e: Exception) {
                //Feedback toast
                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show()
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
                    }
                )
            }
        ) { paddingValues ->
            // LazyColumn is the Compose equivalent of RecyclerView
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
            ) {
                //App settings
                item {
                    //Title
                    Text(
                        text = "App",
                        textAlign = TextAlign.Center,
                        fontFamily = FONT_OPIFICIO,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 10.dp)
                    )

                    //Gallery albums per row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        //Name
                        Text(
                            text = "Gallery albums per row (" + galleryAlbumsPerRow.toInt() + ")",
                            fontSize = 14.sp,
                            modifier = Modifier
                                .weight(1.5f)
                        )

                        //Value
                        Slider(
                            value = galleryAlbumsPerRow,
                            onValueChange = { newValue ->
                                galleryAlbumsPerRow = newValue
                                Storage.putInt("Settings.galleryAlbumsPerRow", newValue.toInt())
                            },
                            valueRange = 2f..4f,
                            steps = 1,
                            modifier = Modifier
                                .weight(1f)
                        )
                    }

                    //Gallery images per row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        //Name
                        Text(
                            text = "Gallery images per row (" + galleryImagesPerRow.toInt() + ")",
                            fontSize = 14.sp,
                            modifier = Modifier
                                .weight(1.5f)
                        )

                        //Value
                        Slider(
                            value = galleryImagesPerRow,
                            onValueChange = { newValue ->
                                galleryImagesPerRow = newValue
                                Storage.putInt("Settings.galleryImagesPerRow", newValue.toInt())
                            },
                            valueRange = 2f..4f,
                            steps = 1,
                            modifier = Modifier
                                .weight(1f)
                        )
                    }

                    //Show missing metadata icon
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
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
                            checked = showMissingMetadataIcon,
                            onCheckedChange = { isChecked ->
                                showMissingMetadataIcon = isChecked
                                Storage.putBool("Settings.showMissingMetadataIcon", isChecked)
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
                            .padding(top = 20.dp)
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
                        text = "To change where an album and its metadata are located, click the \"album folder\" or \"metadata file\" icons.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 5.dp)
                    )
                }

                //Links (list)
                itemsIndexed(links) { index, link ->
                    LinkItem(
                        index = index,
                        link = link,
                        onChooseFolder = {
                            //Save link index
                            filePickerIndex = index

                            //Feedback toast
                            Toast.makeText(this@SettingsActivity, "Select a folder to use as album", Toast.LENGTH_LONG).show()

                            //Ask for a folder
                            filePickerAction = PickerAction.SelectFolder
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            filePickerLauncher.launch(intent)
                        },
                        onChooseFile = {
                            //Check if album folder exists
                            if (!link.albumFolder.exists()) {
                                //Feedback toast
                                Toast.makeText(this@SettingsActivity, "Add album folder first", Toast.LENGTH_LONG).show()
                                return@LinkItem
                            }

                            //Save link index
                            filePickerIndex = index

                            //Check action
                            Orion.snack2(
                                this@SettingsActivity,
                                "Do you have an already created metadata file?",
                                "Select",
                                {
                                    //Feedback toast
                                    Toast.makeText(this@SettingsActivity, "Select a file to use as metadata", Toast.LENGTH_LONG).show()

                                    //Ask for a file
                                    filePickerAction = PickerAction.SelectFile
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                                    intent.setType("application/json")
                                    filePickerLauncher.launch(intent)
                                },
                                "Create",
                                {
                                    //Album folder is needed to take the name
                                    if (!Library.links[index].albumFolder.exists()) {
                                        Toast.makeText(this@SettingsActivity, "Please select an album folder first", Toast.LENGTH_LONG).show()
                                        return@snack2
                                    }

                                    //Feedback toast
                                    Toast.makeText(this@SettingsActivity,"Select a folder to create the album metadata file", Toast.LENGTH_LONG).show()

                                    //Ask for a folder & create file inside
                                    filePickerAction = PickerAction.CreateFile
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                    filePickerLauncher.launch(intent)
                                },
                                Snackbar.LENGTH_INDEFINITE
                            )
                        },
                        onDelete = {
                            //Remove link
                            val removed = Library.removeLink(index)
                            if (!removed) return@LinkItem

                            //Update list & save links
                            updateLinksList()
                            Library.saveLinks()
                        },
                    )
                }

                //Links (add link button)
                item {
                    Button(
                        onClick = {
                            //Create link & try to add it
                            val link = Link("", "");
                            val added = Library.addLink(link)

                            //Check if link was added
                            if (added) {
                                //Added -> Update list & save links
                                updateLinksList()
                                Library.saveLinks()
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
