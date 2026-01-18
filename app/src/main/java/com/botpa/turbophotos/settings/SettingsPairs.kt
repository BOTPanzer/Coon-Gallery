package com.botpa.turbophotos.settings

import com.botpa.turbophotos.util.Storage.StoragePair

object SettingsPairs {

    //App
    @JvmField public val APP_AUTOMATIC_METADATA_MODIFICATION: StoragePair<Boolean> = StoragePair("Settings.appAutomaticMetadataModification", true)

    //Home
    @JvmField public val HOME_ITEMS_PER_ROW: StoragePair<Int> = StoragePair("Settings.homeItemsPerRow", 2)

    //Album
    @JvmField public val ALBUM_ITEMS_PER_ROW: StoragePair<Int> = StoragePair("Settings.albumItemsPerRow", 3)
    @JvmField public val ALBUM_SHOW_MISSING_METADATA_ICON: StoragePair<Boolean> = StoragePair("Settings.albumShowMissingMetadataIcon", false)

    //Sync
    @JvmField public val SYNC_USERS_KEY: String = "Sync.users"

}