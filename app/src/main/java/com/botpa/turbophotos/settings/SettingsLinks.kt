package com.botpa.turbophotos.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.botpa.turbophotos.R
import com.botpa.turbophotos.theme.FONT_OPIFICIO
import com.botpa.turbophotos.theme.FONT_POPPINS
import com.botpa.turbophotos.util.Link

//Preview for the link item with default values
@Preview
@Composable
fun LinkItemPreview() {
    LinkItem(
        index = 0,
        link = Link("Camera", "camera.metadata.json"),
        onChooseFolder = {},
        onChooseFile = {},
        onDelete = {}
    )
}

//Link item
@Composable
fun LinkItem(
    index: Int,
    link: Link,
    onChooseFolder: () -> Unit,
    onChooseFile: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // This is a simplified example of what your item might look like
    Column (
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
        ) {
            //Name
            Text(
                text = "Link $index",
                fontFamily = FONT_OPIFICIO,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 5.dp)
            )

            //Content
            Row (
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                //Album folder & metadata file
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                ) {
                    //Album folder
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 5.dp)
                    ) {
                        //Select button
                        Card(
                            onClick = onChooseFolder,
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .size(40.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.image),
                                    contentDescription = "Select album folder",
                                    contentScale = ContentScale.Fit,
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .fillMaxHeight()
                                )
                            }
                        }

                        //Name
                        val hasAlbum = link.albumFolder.name != ""
                        Text(
                            text = if (hasAlbum) link.albumFolder.name else "Album folder",
                            fontFamily = FONT_POPPINS,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp)
                                .alpha(if (hasAlbum) 1f else 0.5f)
                        )
                    }

                    //Metadata file
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 5.dp)
                    ) {
                        //Select button
                        Card(
                            onClick = onChooseFile,
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .size(40.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.info),
                                    contentDescription = "Select album folder",
                                    contentScale = ContentScale.Fit,
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .fillMaxHeight()
                                )
                            }
                        }

                        //Name
                        val hasMetadata = link.metadataFile.name != ""
                        Text(
                            text = if (hasMetadata) link.metadataFile.name else "Metadata file",
                            fontFamily = FONT_POPPINS,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp)
                                .alpha(if (hasMetadata) 1f else 0.5f)
                        )
                    }
                }

                //Delete button
                Card(
                    onClick = onDelete,
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .size(width = 40.dp, height = 90.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(R.drawable.clear),
                            contentDescription = "Delete link",
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                            modifier = Modifier
                                .size(24.dp)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}