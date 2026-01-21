package com.botpa.turbophotos.gallery

import androidx.compose.runtime.mutableStateListOf
import com.botpa.turbophotos.home.HomeActivity
import com.botpa.turbophotos.gallery.StoragePairs
import com.botpa.turbophotos.util.Storage
import java.io.File

class Link(albumPath: String, metadataPath: String) {

    @JvmField val albumFolder: File = File(albumPath)
    @JvmField val metadataFile: File = File(metadataPath)
    @JvmField var album: Album? = null

    //Getters
    val albumPath: String get() = albumFolder.absolutePath
    val metadataPath: String get() = metadataFile.absolutePath


    //Override toString to be able to save albums in a string
    override fun toString(): String {
        return "$albumPath\n$metadataPath"
    }


    //Static
    companion object {

        //Links
        var linksLoaded: Boolean = false

        @JvmField val links = mutableStateListOf<Link>()
        @JvmField val linksMap: MutableMap<String, Link?> = HashMap<String, Link?>()


        //Loading & saving list
        fun loadLinks(reset: Boolean) {
            //Already loaded
            if (!reset && linksLoaded) return
            linksLoaded = true

            //Clear links
            links.clear()
            linksMap.clear()

            //Get links from storage (as strings)
            val linksUnparsed = Storage.getStringList(StoragePairs.LIBRARY_LINKS_KEY)

            //Parse links
            for (string in linksUnparsed) addLink(parse(string))
        }

        fun saveLinks() {
            //Save links
            val list = ArrayList<String>()
            for (link in links) list.add(link.toString())
            Storage.putStringList(StoragePairs.LIBRARY_LINKS_KEY, list)

            //Restart main activity on resume
            HomeActivity.reloadOnResume()
        }

        //Updating list
        fun addLink(link: Link): Boolean {
            //Check if link exists
            val key = link.albumPath
            if (linksMap.containsKey(key)) return false

            //Add link
            links.add(link)
            linksMap[key] = link

            //Relink with album
            relinkWithAlbum(link)
            return true
        }

        fun removeLink(index: Int): Boolean {
            //Check if link exists
            if (index < 0 || index >= links.size) return false

            //Remove link
            val link = links.removeAt(index)
            linksMap.remove(link.albumPath)

            //Update album
            if (link.album != null) link.album!!.updateMetadataFile(null)
            return true
        }

        fun updateLinkFolder(index: Int, newFolder: File): Boolean {
            //Check if album is already in a link
            val keyNew = newFolder.absolutePath
            if (linksMap.containsKey(keyNew)) return false

            //Get old link
            val oldLink = links[index]

            //Update it
            val link = Link(newFolder.absolutePath, oldLink.metadataPath)
            links[index] = link
            linksMap[keyNew] = link
            linksMap.remove(oldLink.albumPath)

            //Relink with album
            relinkWithAlbum(link)
            return true
        }

        fun updateLinkFile(index: Int, newFile: File) {
            //Get link
            val link = links[index]

            //Update it
            links[index] = Link(link.albumPath, newFile.absolutePath)

            //Relink with album
            relinkWithAlbum(link)
        }

        fun relinkWithAlbum(link: Link) {
            //Update link album reference
            link.album = Library.albumsMap.getOrDefault(link.albumPath, null)

            //Update link album metadata file
            if (link.album != null) link.album!!.updateMetadataFile(link.metadataFile)
        }

        //Parsing
        fun parse(string: String): Link {
            //Split string into parts
            val parts = string.split("\n")

            //Create link with parts
            return Link(
                parts[0],
                if (parts.size >= 2) parts[1] else "",
            )
        }

    }

}