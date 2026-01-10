package com.botpa.turbophotos.gallery

interface LoadingIndicator {

    fun search()
    fun load(content: String)
    fun load(folder: String, type: String)
    fun hide()

}