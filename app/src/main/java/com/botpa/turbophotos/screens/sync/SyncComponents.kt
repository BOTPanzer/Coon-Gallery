package com.botpa.turbophotos.screens.sync

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.views.IconButton
import com.botpa.turbophotos.gallery.views.groupItemPaddingHorizontal
import com.botpa.turbophotos.gallery.views.groupItemPaddingVertical
import com.botpa.turbophotos.theme.FONT_OPIFICIO

//Users
@Composable
fun UserItem(
    index: Int,
    user: User,
    onConnect: (Int, User) -> Unit,
    onSelect: (Int, User) -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row (
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .combinedClickable(
                onClick = { onConnect(index, user) },
                onLongClick = { onSelect(index, user) }
            )
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
                text = user.name,
                fontFamily = FONT_OPIFICIO,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
            )

            //Code
            Text(
                text = user.code,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }

        //Delete button
        IconButton(
            onClick = { onDelete(index) },
            painter = painterResource(R.drawable.clear),
            contentDescription = "Delete user",
            modifier = Modifier
                .fillMaxHeight()
        )
    }
}