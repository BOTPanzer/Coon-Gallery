package com.botpa.turbophotos.gallery.views

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.botpa.turbophotos.R
import com.botpa.turbophotos.theme.FONT_OPIFICIO
import com.botpa.turbophotos.util.Orion

//General
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Layout(title: String, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontFamily = FONT_OPIFICIO,
                        fontSize = 20.sp,
                    )
                },
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        content(innerPadding)
    }
}

//Groups
val groupItemPaddingHorizontal: Dp = 15.dp
val groupItemPaddingVertical: Dp = 10.dp

@Composable
fun Group(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .padding(top = 20.dp, bottom = 10.dp)
    ) {
        content()
    }
}

@Composable
fun GroupTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        fontFamily = FONT_OPIFICIO,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = modifier
    )
}

@Composable
fun GroupItems(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(15.dp), // This clips everything inside
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        content()
    }
}

@Composable
fun GroupDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

//Buttons
@Composable
fun IconButton(onClick: () -> Unit, painter: Painter, contentDescription: String, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .size(40.dp)
    ) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
            modifier = Modifier
                .size(24.dp)
        )
    }
}