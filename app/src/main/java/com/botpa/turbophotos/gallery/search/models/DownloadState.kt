package com.botpa.turbophotos.gallery.search.models

sealed class DownloadState {

    object Checking: DownloadState()
    object Missing: DownloadState()
    data class Downloading(val progress: Float, val size: Long): DownloadState()
    object Failed: DownloadState()
    object Downloaded: DownloadState()

}
