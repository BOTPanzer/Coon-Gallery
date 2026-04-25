package com.botpa.turbophotos.gallery

import com.botpa.turbophotos.util.Storage

object StoragePairs {

    //Library
    const val LIBRARY_LINKS_KEY: String = "Library.links"
    @JvmField val LIBRARY_AUTOMATIC_METADATA_MODIFICATION: Storage.StoragePair<Boolean> = Storage.StoragePair("Library.automaticMetadataModification", true)

    //Home screen
    @JvmField val HOME_ITEMS_PER_ROW: Storage.StoragePair<Int> = Storage.StoragePair("Home.itemsPerRow", 2)

    //Album screen
    @JvmField val ALBUM_ITEMS_PER_ROW: Storage.StoragePair<Int> = Storage.StoragePair("Album.itemsPerRow", 3)
    @JvmField val ALBUM_SHOW_MISSING_METADATA_ICON: Storage.StoragePair<Boolean> = Storage.StoragePair("Album.showMissingMetadataIcon", false)
    @JvmField val ALBUM_SEARCH_METHOD: Storage.StoragePair<String> = Storage.StoragePair("Album.searchMethod", SearchMethod.ContainsWords.name)

    //Display screen
    @JvmField val DISPLAY_SHOW_INFO: Storage.StoragePair<Boolean> = Storage.StoragePair("Display.showInfo", true)
    @JvmField val DISPLAY_SHOW_EDIT: Storage.StoragePair<Boolean> = Storage.StoragePair("Display.showEdit", false)
    @JvmField val DISPLAY_SHOW_SHARE: Storage.StoragePair<Boolean> = Storage.StoragePair("Display.showShare", false)
    @JvmField val DISPLAY_SHOW_FAVOURITE: Storage.StoragePair<Boolean> = Storage.StoragePair("Display.showFavourite", false)

    //Video player
    @JvmField val VIDEO_LOOP: Storage.StoragePair<Boolean> = Storage.StoragePair("Video.loop", true)
    @JvmField val VIDEO_SKIP_BACKWARDS: Storage.StoragePair<Long> = Storage.StoragePair("Video.skipBackwards", 10)
    @JvmField val VIDEO_SKIP_FORWARD: Storage.StoragePair<Long> = Storage.StoragePair("Video.skipForward", 10)
    @JvmField val VIDEO_USE_INTERNAL_PLAYER: Storage.StoragePair<Boolean> = Storage.StoragePair("Video.useInternalPlayer", true)
    @JvmField val VIDEO_IGNORE_AUDIO_FOCUS: Storage.StoragePair<Boolean> = Storage.StoragePair("Video.ignoreAudioFocus", true)

    //Sync screen
    const val SYNC_USERS_KEY: String = "Sync.users"

}