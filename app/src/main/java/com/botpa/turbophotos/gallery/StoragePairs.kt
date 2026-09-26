package com.botpa.turbophotos.gallery

import com.botpa.turbophotos.gallery.data.SortDirection
import com.botpa.turbophotos.gallery.data.SortMethod
import com.botpa.turbophotos.gallery.search.SearchMethod
import com.botpa.turbophotos.util.Storage

object StoragePairs {

    //App
    val APP_UPDATE_CHECK: Storage.StoragePair<Boolean> = Storage.StoragePair("App.checkForUpdates", true)
    val APP_UPDATE_SKIPPED: Storage.StoragePair<String> = Storage.StoragePair("App.skipUpdate", "")

    //Library
    const val LIBRARY_LINKS_KEY: String = "Library.links"
    val LIBRARY_AUTOMATIC_METADATA_MODIFICATION: Storage.StoragePair<Boolean> = Storage.StoragePair("Library.automaticMetadataModification", true)
    val LIBRARY_SORT_METHOD: Storage.StoragePair<String> = Storage.StoragePair("Library.sortMethod", SortMethod.Date.name)
    val LIBRARY_SORT_DIRECTION: Storage.StoragePair<String> = Storage.StoragePair("Library.sortDirection", SortDirection.Descending.name)

    //Home screen
    val HOME_ITEMS_PER_ROW: Storage.StoragePair<Int> = Storage.StoragePair("Home.itemsPerRow", 2)
    val HOME_PINNED_ALBUM: Storage.StoragePair<String> = Storage.StoragePair("Home.pinnedAlbum", "")

    //Album screen
    val ALBUM_ITEMS_PER_ROW: Storage.StoragePair<Int> = Storage.StoragePair("Album.itemsPerRow", 3)
    val ALBUM_SHOW_MISSING_METADATA_ICON: Storage.StoragePair<Boolean> = Storage.StoragePair("Album.showMissingMetadataIcon", false)
    val ALBUM_SEARCH_METHOD: Storage.StoragePair<String> = Storage.StoragePair("Album.searchMethod", SearchMethod.ContainsWords.name)

    //Viewer screen
    val VIEWER_SHOW_PROPERTIES: Storage.StoragePair<Boolean> = Storage.StoragePair("Viewer.showProperties", true)
    val VIEWER_SHOW_EDIT: Storage.StoragePair<Boolean> = Storage.StoragePair("Viewer.showEdit", true)
    val VIEWER_SHOW_SHARE: Storage.StoragePair<Boolean> = Storage.StoragePair("Viewer.showShare", true)
    val VIEWER_SHOW_FAVOURITE: Storage.StoragePair<Boolean> = Storage.StoragePair("Viewer.showFavourite", false)

    //Video player
    val VIDEO_LOOP: Storage.StoragePair<Boolean> = Storage.StoragePair("Video.loop", true)
    val VIDEO_SKIP_BACKWARDS: Storage.StoragePair<Long> = Storage.StoragePair("Video.skipBackwards", 10)
    val VIDEO_SKIP_FORWARD: Storage.StoragePair<Long> = Storage.StoragePair("Video.skipForward", 10)
    val VIDEO_USE_INTERNAL_PLAYER: Storage.StoragePair<Boolean> = Storage.StoragePair("Video.useInternalPlayer", true)
    val VIDEO_AUTOMATIC_PIP: Storage.StoragePair<Boolean> = Storage.StoragePair("Video.automaticPiP", true)
    val VIDEO_IGNORE_AUDIO_FOCUS: Storage.StoragePair<Boolean> = Storage.StoragePair("Video.ignoreAudioFocus", true)
    val VIDEO_SHOW_CONTROLS_ON_START: Storage.StoragePair<Boolean> = Storage.StoragePair("Video.showControlsOnStart", true)

    //Sync screen
    const val SYNC_USERS_KEY: String = "Sync.users"

}