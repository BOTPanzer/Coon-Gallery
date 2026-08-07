package com.botpa.turbophotos.gallery

interface LoadingIndicator {

    fun search()
    fun metadata(album: String)
    fun hide()

}