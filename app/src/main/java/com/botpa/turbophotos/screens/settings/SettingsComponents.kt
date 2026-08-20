package com.botpa.turbophotos.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.data.Link
import com.botpa.turbophotos.gallery.jetpack.FONT_OUTFIT
import com.botpa.turbophotos.gallery.jetpack.IconButton
import com.botpa.turbophotos.gallery.jetpack.groupItemPaddingHorizontal
import com.botpa.turbophotos.gallery.jetpack.groupItemPaddingVertical

//Settings
@Composable
fun SettingsItem(
    title: String,
    description: String? = null,
    content: @Composable (RowScope.() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 50.dp)
            .padding(horizontal = groupItemPaddingHorizontal, vertical = groupItemPaddingVertical)
    ) {
        //Title & description
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(end = if (content != null) 14.dp else 0.dp)
                .weight(1f)
        ) {
            //Title
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            //Description
            if (description != null) {
                Text(
                    text = description,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    softWrap = true,
                    modifier = Modifier
                        .padding(top = 2.dp)
                )
            }
        }

        //Content
        if (content != null) content()
    }
}

@Composable
fun SettingsItem(
    title: Int,
    description: Int? = null,
    content: @Composable (RowScope.() -> Unit)? = null
) {
    SettingsItem(
        stringResource(title),
        if (description != null) stringResource(description) else null,
        content = content
    )
}

//Links
@Composable
fun LinkItem(
    index: Int,
    link: Link,
    onChooseAlbum: (Int) -> Unit,
    onChooseMetadata: (Int, Link) -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row (
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .padding(horizontal = groupItemPaddingHorizontal, vertical = groupItemPaddingVertical)
    ) {
        //Content
        Column(
            modifier = modifier
                .padding(end = 10.dp)
                .weight(1.0f)
        ) {
            //Name
            Text(
                text = stringResource(R.string.settings_metadata_links_item_name, index),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
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
                IconButton(
                    onClick = { onChooseAlbum(index) },
                    painter = painterResource(R.drawable.icon_storage_folder),
                    contentDescription = "Select album folder"
                )

                //Name
                val hasAlbum = link.albumFolder.name != ""
                Text(
                    text = if (hasAlbum) link.albumFolder.name else stringResource(R.string.settings_metadata_links_placeholder_album),
                    fontFamily = FONT_OUTFIT,
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
                IconButton(
                    onClick = { onChooseMetadata(index, link) },
                    painter = painterResource(R.drawable.icon_storage_file),
                    contentDescription = "Select metadata file"
                )

                //Name
                val hasMetadata = link.metadataFile.name != ""
                Text(
                    text = if (hasMetadata) link.metadataFile.name else stringResource(R.string.settings_metadata_links_placeholder_metadata),
                    fontFamily = FONT_OUTFIT,
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
        IconButton(
            onClick = { onDelete(index) },
            painter = painterResource(R.drawable.icon_clear),
            contentDescription = "Delete link",
            modifier = Modifier
                .fillMaxHeight()
        )
    }
}

@Preview
@Composable
fun LinkItemPreview() {
    LinkItem(
        index = 0,
        link = Link("Camera", "camera.json"),
        onChooseAlbum = { i -> },
        onChooseMetadata = { i, l -> },
        onDelete = { i -> }
    )
}

//Screens
@Composable
fun SettingsScreenBanner(
    image: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(bottom = 20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = image),
                contentDescription = "Banner",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary, blendMode = BlendMode.Modulate),
                modifier = Modifier
                    .fillMaxHeight()
            )
        }
    }
}
