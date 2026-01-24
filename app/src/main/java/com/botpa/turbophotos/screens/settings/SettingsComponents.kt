package com.botpa.turbophotos.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Link
import com.botpa.turbophotos.gallery.views.IconButton
import com.botpa.turbophotos.gallery.views.groupItemPaddingHorizontal
import com.botpa.turbophotos.gallery.views.groupItemPaddingVertical
import com.botpa.turbophotos.theme.FONT_OPIFICIO
import com.botpa.turbophotos.theme.FONT_POPPINS

//Settings
@Composable
fun SettingsItem(
    title: String,
    description: String? = null,
    content: @Composable RowScope.() -> Unit
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
                IconButton(
                    onClick = { onChooseAlbum(index) },
                    painter = painterResource(R.drawable.folder),
                    contentDescription = "Select album folder"
                )

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
                IconButton(
                    onClick = { onChooseMetadata(index, link) },
                    painter = painterResource(R.drawable.file),
                    contentDescription = "Select metadata file"
                )

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
        IconButton(
            onClick = { onDelete(index) },
            painter = painterResource(R.drawable.clear),
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