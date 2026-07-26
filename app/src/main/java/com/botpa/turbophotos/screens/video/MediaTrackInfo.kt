package com.botpa.turbophotos.screens.video

import androidx.media3.common.Tracks

data class MediaTrackInfo(
    val name: String,
    val language: String? = null,
    val trackGroup: Tracks.Group? = null,
    val trackIndex: Int = -1,
    var isSelected: Boolean = false
)