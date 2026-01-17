package com.botpa.turbophotos.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Link
import com.botpa.turbophotos.theme.FONT_OPIFICIO
import com.botpa.turbophotos.theme.FONT_POPPINS

//Basic components
@Composable
fun SettingsTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        fontFamily = FONT_OPIFICIO,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = modifier
    )
}

@Composable
fun SettingsItem(title: String, description: String? = null, content: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 50.dp)
            .padding(horizontal = 15.dp, vertical = 10.dp)
    ) {
        //Title & description
        Column(
            modifier = Modifier
                .padding(end = 10.dp)
                .weight(1f)
        ) {
            //Title
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            //Description
            if (description != null) {
                Text(
                    text = description,
                    fontSize = 12.sp,
                    softWrap = true
                )
            }
        }

        //Content
        content()
    }
}

@Composable
fun SettingsItems(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(15.dp), // This clips everything inside
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .padding(top = 20.dp, bottom = 10.dp)
    ) {
        content()
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

//Link item
@Composable
fun LinkItem(
    index: Int,
    link: Link,
    onChooseFolder: (Int) -> Unit,
    onChooseFile: (Int, Link) -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row (
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .padding(horizontal = 15.dp, vertical = 10.dp)
    ) {
        //Content
        Column(
            modifier = modifier
                .padding(end = 10.dp)
                .weight(1.0f)
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

            //Album folder
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 5.dp)
            ) {
                //Select button
                Button(
                    onClick = { onChooseFolder(index) },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(40.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.folder),
                        contentDescription = "Select album folder",
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier
                            .size(24.dp)
                            .fillMaxHeight()
                    )
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
                Button(
                    onClick = { onChooseFile(index, link) },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(40.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.folder),
                        contentDescription = "Select metadata file",
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier
                            .size(24.dp)
                            .fillMaxHeight()
                    )
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
        Button(
            onClick = { onDelete(index) },
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .width(40.dp)
                .fillMaxHeight()
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

//Preview for the link item with default values
@Preview
@Composable
fun LinkItemPreview() {
    LinkItem(
        index = 0,
        link = Link("Camera", "camera.json"),
        onChooseFolder = { i -> },
        onChooseFile = { i, l -> },
        onDelete = { i -> }
    )
}