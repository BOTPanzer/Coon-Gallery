package com.botpa.turbophotos.gallery

import com.botpa.turbophotos.util.Storage

object StoragePairs {

    //Library
    const val LIBRARY_LINKS_KEY: String = "Library.links"

    //App
    @JvmField val APP_AUTOMATIC_METADATA_MODIFICATION: Storage.StoragePair<Boolean> = Storage.StoragePair("Settings.appAutomaticMetadataModification", true)

    //Home
    @JvmField val HOME_ITEMS_PER_ROW: Storage.StoragePair<Int> = Storage.StoragePair("Settings.homeItemsPerRow", 2)

    //Album
    @JvmField val ALBUM_ITEMS_PER_ROW: Storage.StoragePair<Int> = Storage.StoragePair("Settings.albumItemsPerRow", 3)
    @JvmField val ALBUM_SHOW_MISSING_METADATA_ICON: Storage.StoragePair<Boolean> = Storage.StoragePair("Settings.albumShowMissingMetadataIcon", false)

    //Video player
    @JvmField val VIDEO_LOOP: Storage.StoragePair<Boolean> = Storage.StoragePair("Video.loop", true)
    @JvmField val VIDEO_SKIP_BACKWARDS: Storage.StoragePair<Long> = Storage.StoragePair("Video.skipBackwards", 10)
    @JvmField val VIDEO_SKIP_FORWARD: Storage.StoragePair<Long> = Storage.StoragePair("Video.skipForward", 10)
    @JvmField val VIDEO_USE_INTERNAL_PLAYER: Storage.StoragePair<Boolean> = Storage.StoragePair("Video.useInternalPlayer", true)
    @JvmField val VIDEO_IGNORE_AUDIO_FOCUS: Storage.StoragePair<Boolean> = Storage.StoragePair("Video.ignoreAudioFocus", true)

    //Sync
    const val SYNC_USERS_KEY: String = "Sync.users"

}