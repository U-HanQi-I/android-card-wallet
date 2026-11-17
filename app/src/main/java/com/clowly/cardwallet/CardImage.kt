package com.clowly.cardwallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
internal fun CardImage(
    model: Any,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    description: String? = null,
    placeholderText: Boolean = true,
) {
    val shape = RoundedCornerShape(18.dp)
    var failed by remember(model) { mutableStateOf(false) }
    Box(
        modifier.shadow(4.dp, shape).clip(shape).background(Line),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = model,
            contentDescription = description,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            onLoading = { failed = false },
            onSuccess = { failed = false },
            onError = { failed = true },
        )
        if (failed && placeholderText) {
            Text("无法显示卡面", Modifier.padding(12.dp), color = Quiet, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
