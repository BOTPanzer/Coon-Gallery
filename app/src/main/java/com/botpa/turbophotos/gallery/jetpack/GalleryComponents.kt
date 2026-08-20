package com.botpa.turbophotos.gallery.jetpack

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                        fontFamily = FONT_FRAUNCES,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                },
                modifier = Modifier
                    .padding(end = 15.dp)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        content(innerPadding)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Layout(title: Int, content: @Composable (PaddingValues) -> Unit) {
    Layout(
        title = stringResource(title),
        content = content
    )
}

//Groups
val groupItemPaddingHorizontal: Dp = 16.dp
val groupItemPaddingVertical: Dp = 14.dp

@Composable
fun Group(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .padding(bottom = 20.dp)
    ) {
        content()
    }
}

@Composable
fun GroupTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        fontFamily = FONT_OPIFICIO,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(bottom = 10.dp)
    )
}

@Composable
fun GroupTitle(title: Int, modifier: Modifier = Modifier) {
    GroupTitle(
        title = stringResource(title),
        modifier = modifier
    )
}

@Composable
fun GroupDescription(description: String, modifier: Modifier = Modifier) {
    Text(
        text = description,
        fontSize = 14.sp,
        modifier = modifier
    )
}

@Composable
fun GroupDescription(description: Int, modifier: Modifier = Modifier) {
    GroupDescription(
        description = stringResource(description),
        modifier = modifier
    )
}

@Composable
fun GroupItems(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp), //Clips everything inside
        modifier = modifier
            .fillMaxWidth()
    ) {
        content()
    }
}

@Composable
fun GroupItem(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(5.dp),
            modifier = modifier
                .fillMaxWidth()
        ) {
            content()
        }
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(5.dp),
            modifier = modifier
                .fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
fun GroupDivider() {
    Spacer(modifier = Modifier.height(3.dp))
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

@Composable
fun SimpleButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = text,
            fontFamily = FONT_OUTFIT,
            fontSize = 16.sp
        )
    }
}

@Composable
fun SimpleButton(text: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SimpleButton(
        text = stringResource(text),
        onClick = onClick,
        modifier = modifier
    )
}
