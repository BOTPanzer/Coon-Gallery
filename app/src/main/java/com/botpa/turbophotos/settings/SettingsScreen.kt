package com.botpa.turbophotos.settings

//import com.botpa.turbophotos.settings.SettingsActivity.PickerAction

class SettingsScreen {


    //Layout
    /*@Preview
    @Composable
    private fun SettingsScreen() {
        val context = LocalContext.current
        val activity = this

        // State for app settings, using remember and mutableStateOf
        var galleryAlbumsPerRow by remember { mutableIntStateOf(Storage.getInt("Settings.galleryAlbumsPerRow", 2)) }
        var galleryImagesPerRow by remember { mutableIntStateOf(Storage.getInt("Settings.galleryImagesPerRow", 3)) }
        var showMissingMetadataIcon by remember { mutableStateOf(Storage.getBool("Settings.showMissingMetadataIcon", false)) }

        // Handles the file/folder picking logic using a Compose-safe launcher
        val linksFilePickerLauncher = registerForActivityResult(StartActivityForResult()) { result ->
            //Bad result
            if (result.resultCode != RESULT_OK || result.data == null) return@registerForActivityResult

            //Parse result
            try {
                //Parse file path from URI
                val path = Orion.convertUriToFilePath(this@SettingsActivity, result.data!!.data)
                if (path == null) throw Exception("Path was null")
                val file = File(path)

                //Update
                val album = Library.links[linksFilePickerIndex]
                when (linksFilePickerAction) {
                    //Select folder
                    PickerAction.SelectFolder -> {
                        val updated =
                            Library.updateLinkFolder(
                                linksFilePickerIndex,
                                file
                            )
                        if (!updated) {
                            Orion.snack(this@SettingsActivity, "Album already exists")
                            return@registerForActivityResult
                        }
                    }

                    //Select file
                    PickerAction.SelectFile -> {
                        Library.updateLinkFile(
                            linksFilePickerIndex,
                            file
                        )
                    }

                    //Create file
                    PickerAction.CreateFile -> {
                        var metadataFile: File
                        var name: String
                        var i = 0
                        do {
                            name = album.imagesFolder.name.lowercase(Locale.getDefault()).replace(" ", "-") + (if (i > 0) " ($i)" else "") + ".metadata.json"
                            metadataFile = File(file.absolutePath + "/" + name)
                            i++
                        } while (metadataFile.exists())
                        Orion.writeFile(metadataFile, "{}")
                        album.metadataFile = metadataFile
                    }

                    //Other (error?)
                    else -> throw Exception("Invalid file picker action")
                }
                linksAdapter.notifyItemChanged(linksFilePickerIndex)

                //Save albums
                Library.saveLinks()
            } catch (e: Exception) {
                //Error logging
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
            }
        }


        //Layout
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Settings") }
                )
            }
        ) { paddingValues ->
            // LazyColumn is the Compose equivalent of RecyclerView
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, bottom = 20.dp)
            ) {
                // "App" settings section
                item {
                    Text(
                        text = "App",
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp)
                    )

                    // Gallery albums per row
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gallery albums per row",
                            modifier = Modifier.weight(1.5f)
                        )
                        Slider(
                            value = galleryAlbumsPerRow.toFloat(),
                            onValueChange = { newValue ->
                                galleryAlbumsPerRow = newValue.toInt()
                                Storage.putInt("Settings.galleryAlbumsPerRow", newValue.toInt())
                            },
                            valueRange = 2f..4f,
                            steps = 2,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Gallery images per row
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gallery images per row",
                            modifier = Modifier.weight(1.5f)
                        )
                        Slider(
                            value = galleryImagesPerRow.toFloat(),
                            onValueChange = { newValue ->
                                galleryImagesPerRow = newValue.toInt()
                                Storage.putInt("Settings.galleryImagesPerRow", newValue.toInt())
                            },
                            valueRange = 2f..4f,
                            steps = 2,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Show missing metadata icon
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Show missing metadata icon",
                            modifier = Modifier.weight(1.5f)
                        )
                        Switch(
                            checked = showMissingMetadataIcon,
                            onCheckedChange = { isChecked ->
                                showMissingMetadataIcon = isChecked
                                Storage.putBool("Settings.showMissingMetadataIcon", isChecked)
                            }
                        )
                    }
                }

                // "Albums & Metadata Links" section
                item {
                    Text(
                        text = "Albums & Metadata Links",
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                    )
                    // Other descriptive texts
                    Text(text = "Add albums to enable backing them up.", modifier = Modifier.fillMaxWidth().padding(top = 5.dp))
                }

                // Links list (Replaces RecyclerView)
                items(Library.links.toList()) { link ->
                    // This represents a single item in the list, like in your LinksAdapter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { /* The activity's item click logic */ },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Link: ${link.imagesFolder.path}", modifier = Modifier.weight(1f))
                        // Your remove button logic here
                    }
                }

                // "Add link" button (Replaces CardView and LinearLayout)
                item {
                    Button(
                        onClick = {
                            val added = Library.addLink(Link("", ""))
                            if (added) {
                                Library.saveLinks()
                            } else {
                                Orion.snack(activity, "Can't have duplicate links")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    ) {
                        Text(text = "Add link")
                    }
                }
            }
        }
    }*/


}